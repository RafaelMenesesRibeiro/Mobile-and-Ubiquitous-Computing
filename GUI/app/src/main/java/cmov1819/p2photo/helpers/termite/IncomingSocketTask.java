package cmov1819.p2photo.helpers.termite;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.R;
import cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.CatalogOperations;
import cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.ImageLoading;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.SessionManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

import static cmov1819.p2photo.helpers.ConvertUtils.bitmapToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;

public class IncomingSocketTask extends AsyncTask<Object, String, Void> {

    private static final String INCOMING_TASK_TAG = "INCOMING SOCKET";

    @Override
    protected void onPreExecute() {
        LogManager.logInfo(INCOMING_TASK_TAG, "Started WiFi Direct server task (" + this.hashCode() + ").");
    }

    @Override
    protected Void doInBackground(final Object... params) {


        try {
            P2PhotoWiFiDirectManager wiFiDirectManager = (P2PhotoWiFiDirectManager) params[0];
            // setup a server socket on MainMenuActivity
            wiFiDirectManager.setServerSocket(new SimWifiP2pSocketServer(R.string.termite_port));
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
                            case "requestCatalog":
                                socket.getOutputStream()
                                        .write(processCatalogRequest(wiFiDirectManager, jsonObject));
                                break;
                            case "requestPhoto":
                                socket.getOutputStream()
                                        .write(processPhotoRequest(wiFiDirectManager, jsonObject));
                                break;
                            case "areYouServerReply":
                                break;
                            default:
                                socket.getOutputStream()
                                        .write(("warning: 'operation' field invalid\n").getBytes());
                                break;
                        }
                    } else {
                        socket.getOutputStream()
                                .write(("error: 'operation' field not found\n").getBytes());
                    }
                    // Close interaction
                    socket.getOutputStream().write(("\n").getBytes());
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

    @Override
    protected void onPostExecute(Void result) {
        LogManager.logInfo(INCOMING_TASK_TAG, "WiFi Direct server task shutdown (" + this.hashCode() + ").");
    }

    private byte[] processCatalogRequest(P2PhotoWiFiDirectManager wiFiDirectManager,
                                         JSONObject jsonObject) throws JSONException, IOException {

        MainMenuActivity activity = wiFiDirectManager.getMainMenuActivity();
        String catalogId = jsonObject.getString("catalogId");
        String callerUsername = jsonObject.getString("callerUsername");

        // TODO Ask P2PWebsServer if this <catalogId> has a member named <callerUsername>
        /*
        if (isMember(catalogId, callerUsername)) {
            JSONObject catalogFileContents = CatalogOperations.readCatalog(activity, catalogId);
            ...
        } else {
            return "error: target username does not belong to invoked album".getBytes();
        }
        */
        JSONObject catalogFileContents = CatalogOperations.readCatalog(activity, catalogId);

        jsonObject = new JSONObject();
        jsonObject.put("operation", "sendCatalog");
        jsonObject.put("callerUsername", SessionManager.getUsername(wiFiDirectManager.getMainMenuActivity()));
        jsonObject.put("catalogId", catalogId);
        jsonObject.put("catalogFile", catalogFileContents.toString());

        try {
            return jsonObject.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            return jsonObject.toString().getBytes();
        }
    }

    private byte[] processPhotoRequest(final P2PhotoWiFiDirectManager wiFiDirectManager,
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

        try {
            return jsonObject.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            return jsonObject.toString().getBytes();
        }
    }
}