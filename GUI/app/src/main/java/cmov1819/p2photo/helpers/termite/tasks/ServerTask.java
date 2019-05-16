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
import java.io.InputStream;
import java.io.InputStreamReader;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.ImageLoading;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.helpers.managers.WifiDirectManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.bitmapToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.termite.Consts.*;

public class ServerTask extends AsyncTask<Void, String, Void> {
    private static final String INCOMING_TASK_TAG = "INCOMING SOCKET";

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
                    InputStream inputStream = socket.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String data = bufferedReader.readLine();
                    // Process values
                    JSONObject request = new JSONObject(data);
                    if (request.has(OPERATION)) {
                        switch (request.getString(OPERATION)) {
                            case SEND_CATALOG:
                                doRespond(socket, processIncomingCatalog(wiFiDirectManager, request));
                                break;
                            case REQUEST_PHOTO:
                                doRespond(socket, replyWithRequestedPhoto(wiFiDirectManager, request));
                                break;
                            case SEND_PHOTO:
                                doRespond(socket,processIncomingPhoto(wiFiDirectManager, request));
                                break;
                            default:
                                doRespond(socket,"warning: '" + OPERATION + "' is unsupported...");
                                break;
                        }
                    } else {
                        doRespond(socket,"warning: requests need 'operation' field...");
                    }
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

    private String processIncomingCatalog(WifiDirectManager wiFiDirectManager, JSONObject jsonObject) throws JSONException {
        LogManager.logInfo(INCOMING_TASK_TAG, "Processing incoming catalog...");
        // TODO IM HERE ISSUE
        JSONObject catalogFile = jsonObject.getJSONObject(CATALOG_FILE);
        String catalogId = catalogFile.getString(CATALOG_ID);
        String sender = jsonObject.getString(FROM);

        LogManager.logInfo(INCOMING_TASK_TAG, "Deciphering catalog file...");
        String cipheredCatalogFile = jsonObject.getString(CATALOG_FILE);
        byte[] encodedCatalogFile = base64StringToByteArray(cipheredCatalogFile);
        // byte[] decodedCatalogFile = decipherWithAes(key, encodedCatalogFile);
        // JSONObject decipheredCatalogFile = new JSONObject(byteArrayToUtf8(decodedCatalogFile));

        // CatalogMerge.mergeCatalogFiles(wiFiDirectManager.getMainMenuActivity(), catalogId, decipheredCatalogFile);

        return "";
    }

    private String processIncomingPhoto(WifiDirectManager wiFiDirectManager, JSONObject jsonObject) throws JSONException {
        LogManager.logInfo(INCOMING_TASK_TAG, "Processing incoming photo");
        try {
            String photoUuid = jsonObject.getString(PHOTO_UUID);
            String base64photo = jsonObject.getString(PHOTO_FILE);
            byte[] encodedPhoto = base64StringToByteArray(base64photo);
            Bitmap decodedPhoto = BitmapFactory.decodeByteArray(encodedPhoto, 0, encodedPhoto.length);
            ImageLoading.savePhoto(wiFiDirectManager.getMainMenuActivity(), photoUuid, decodedPhoto);
        } catch (IOException ioe) {
            LogManager.logError(INCOMING_TASK_TAG, ioe.getMessage());
        }
        return "";
    }

    @Override
    protected void onPostExecute(Void result) {
        LogManager.logInfo(INCOMING_TASK_TAG, "WiFi Direct server task shutdown (" + this.hashCode() + ").");
    }

    private String replyWithRequestedPhoto(final WifiDirectManager wiFiDirectManager,
                                           JSONObject jsonObject) throws JSONException, FileNotFoundException {

        MainMenuActivity activity = wiFiDirectManager.getMainMenuActivity();
        String catalogId = jsonObject.getString(CATALOG_ID);
        String callerUsername = jsonObject.getString(USERNAME);
        String photoUuid = jsonObject.getString(PHOTO_UUID);

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
        jsonObject.put(OPERATION, SEND_PHOTO);
        jsonObject.put(USERNAME, SessionManager.getUsername(wiFiDirectManager.getMainMenuActivity()));
        jsonObject.put(PHOTO_UUID, photoUuid);
        jsonObject.put(PHOTO_FILE, byteArrayToBase64String(bitmapToByteArray(photo)));
        return jsonObject.toString();
    }

    /** Helpers */
    private void doRespond(SimWifiP2pSocket socket, JSONObject jsonObject) throws IOException {
        doRespond(socket, jsonObject.toString());
    }

    private void doRespond(SimWifiP2pSocket socket, String string) throws IOException {
        socket.getOutputStream().write((string + CONFIRM_RCV).getBytes());
    }
}