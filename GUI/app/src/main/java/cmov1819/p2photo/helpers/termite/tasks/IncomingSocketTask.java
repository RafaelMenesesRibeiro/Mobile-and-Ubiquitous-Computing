package cmov1819.p2photo.helpers.termite.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.CatalogMerge;
import cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.CatalogOperations;
import cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.ImageLoading;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.helpers.managers.WifiDirectManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.bitmapToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToUtf8;
import static cmov1819.p2photo.helpers.CryptoUtils.decipherWithAes;

public class IncomingSocketTask extends AsyncTask<Void, String, Void> {
    private static final String INCOMING_TASK_TAG = "INCOMING SOCKET";
    private static final String CONFIRM_RCV = "\n";
    private static final String SEND = "\n";

    private static final int TERMITE_PORT = 10001;

    private WifiDirectManager wiFiDirectManager = null;

    @Override
    protected void onPreExecute() {
        LogManager.logInfo(INCOMING_TASK_TAG, "Started WiFi Direct server task (" + this.hashCode() + ").");
    }

    @Override
    protected Void doInBackground(final Void... params) {
        try {
            // get singleton instance of our WifiDirectManager
            wiFiDirectManager = WifiDirectManager.getInstance();
            // setup a server socket on MainMenuActivity
            wiFiDirectManager.setServerSocket(new SimWifiP2pSocketServer(TERMITE_PORT));
            // set server socket to listen to incoming requests
            while (!Thread.currentThread().isInterrupted()) {
                SimWifiP2pSocket socket = wiFiDirectManager.getServerSocket().accept();
                try {
                    // Read from input stream
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String incomingJsonData = bufferedReader.readLine();
                    // Process values
                    JSONObject jsonObject = new JSONObject(incomingJsonData);
                    if (jsonObject.has("operation")) {
                        String operation = jsonObject.getString("operation");
                        switch (operation) {
                            case "requestingCatalog":
                                String jsonCatalog = replyWithRequestCatalog(wiFiDirectManager, jsonObject);
                                socket.getOutputStream().write((jsonCatalog + CONFIRM_RCV).getBytes());
                                break;
                            case "sendingCatalog":
                                // Store incoming catalog in this device
                                processIncomingCatalog(wiFiDirectManager, jsonObject);
                                socket.getOutputStream().write(CONFIRM_RCV.getBytes());
                                break;
                            case "requestingPhoto":
                                String jsonPhoto = replyWithRequestedPhoto(wiFiDirectManager, jsonObject);
                                socket.getOutputStream().write((jsonPhoto + CONFIRM_RCV).getBytes());
                                break;
                            case "sendingPhto":
                                // Store incoming photo in this device
                                processIncomingPhoto(wiFiDirectManager, jsonObject);
                                socket.getOutputStream().write(CONFIRM_RCV.getBytes());
                            default:
                                socket.getOutputStream().write(("warning: 'operation' field invalid" + CONFIRM_RCV).getBytes());
                                break;
                        }
                    } else {
                        socket.getOutputStream().write(("error: 'operation' field not found" + CONFIRM_RCV).getBytes());
                    }
                    // Close interaction
                    socket.getOutputStream().write((CONFIRM_RCV).getBytes());
                } catch (IOException ioe) {
                    Log.e(INCOMING_TASK_TAG, "Error reading socket: " + ioe.getMessage());
                } catch (JSONException jsone) {
                    Log.e(INCOMING_TASK_TAG, "Error reading socket: malformed json request");
                } finally {
                    socket.close();
                }
            }
        } catch (IOException ioe) {
            Log.e(INCOMING_TASK_TAG, "Socket error: " + ioe.getMessage());
        }

        return null;
    }

    private void processIncomingCatalog(WifiDirectManager wiFiDirectManager, JSONObject jsonObject)
            throws JSONException {

        LogManager.logInfo(INCOMING_TASK_TAG, String.format("Processing incomming catalog \n%s\n", jsonObject.toString(4)));

        String catalogId = jsonObject.getString("catalogId");
        String sender = jsonObject.getString("from");
        String aesKeyRetrievalToken = jsonObject.getString("token");

        LogManager.logInfo(INCOMING_TASK_TAG, "Trying to exchange token for aes key...");
        byte[] encodedKey = base64StringToByteArray(exchangeTokenForAESKey(sender, aesKeyRetrievalToken));
        SecretKey key = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");

        LogManager.logInfo(INCOMING_TASK_TAG, "Deciphering catalog file...\n");
        String cipheredCatalogFile = jsonObject.getString("catalogFile");
        byte[] encodedCatalogFile = base64StringToByteArray(cipheredCatalogFile);
        byte[] decodedCatalogFile = decipherWithAes(key, encodedCatalogFile);
        JSONObject decipheredCatalogFile = new JSONObject(byteArrayToUtf8(decodedCatalogFile));

        CatalogMerge.mergeCatalogFiles(wiFiDirectManager.getMainMenuActivity(), catalogId, decipheredCatalogFile);
    }

    private void processIncomingPhoto(WifiDirectManager wiFiDirectManager, JSONObject jsonObject)
            throws JSONException {

        LogManager.logInfo(INCOMING_TASK_TAG, String.format("Processing incomming photo\n%s\n", jsonObject.toString(4)));

        String photoUuid = jsonObject.getString("photoUuid");
        String base64photo = jsonObject.getString("photo");
        byte[] encodedPhoto = base64StringToByteArray(base64photo);
        Bitmap decodedPhoto = BitmapFactory.decodeByteArray(encodedPhoto, 0, encodedPhoto.length);

        try {
            ImageLoading.savePhoto(
                    wiFiDirectManager.getMainMenuActivity(), photoUuid, decodedPhoto
            );
        } catch (IOException ioe) {
            LogManager.logError(INCOMING_TASK_TAG, ioe.getMessage());
        }
    }

    private String exchangeTokenForAESKey(String userWhoGeneratedTheToken, String token) {
        // TODO send request to server to obtain Base64 token.
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        LogManager.logInfo(INCOMING_TASK_TAG, "WiFi Direct server task shutdown (" + this.hashCode() + ").");
    }

    private String replyWithRequestCatalog(final WifiDirectManager wiFiDirectManager,
                                           JSONObject jsonObject) throws JSONException, IOException {

        MainMenuActivity activity = wiFiDirectManager.getMainMenuActivity();
        String catalogId = jsonObject.getString("catalogId");
        String sender = jsonObject.getString("from");

        // TODO Ask P2PWebsServer if this <catalogId> has a member named <callerUsername>
        /*
        if (isMember(catalogId, sender)) {
            ...
        } else {
            return "error: target username does not belong to invoked album".getBytes();
        }
        */
        JSONObject catalogFileContents = CatalogOperations.readCatalog(activity, catalogId);
        jsonObject = new JSONObject();
        jsonObject.put("operation", "sendCatalog");
        jsonObject.put("from", SessionManager.getUsername(wiFiDirectManager.getMainMenuActivity()));
        jsonObject.put("catalogId", catalogId);
        jsonObject.put("catalogFile", catalogFileContents.toString());
        return jsonObject.toString();
    }

    private String replyWithRequestedPhoto(final WifiDirectManager wiFiDirectManager,
                                           JSONObject jsonObject) throws JSONException, FileNotFoundException {

        MainMenuActivity activity = wiFiDirectManager.getMainMenuActivity();
        String catalogId = jsonObject.getString("catalogId");
        String callerUsername = jsonObject.getString("callerUsername");
        String photoUuid = jsonObject.getString("photoUuid");

        // TODO Ask P2PWebsServer if this <catalogId> has a member named <callerUsername>
        /*
        if (isMember(catalogId, callerUsername)) {
            Bitmap photo = ImageLoading.loadPhoto(activity, photoUuid);
            ...
        } else {
            return "error: target username does not belong to invoked album".getBytes();
        }
        */
        Bitmap photo = ImageLoading.loadPhoto(activity, photoUuid);
        jsonObject = new JSONObject();
        jsonObject.put("operation", "sendPhoto");
        jsonObject.put("callerUsername", SessionManager.getUsername(wiFiDirectManager.getMainMenuActivity()));
        jsonObject.put("photoUuid", photoUuid);
        jsonObject.put("photo", byteArrayToBase64String(bitmapToByteArray(photo)));
        return jsonObject.toString();
    }
}