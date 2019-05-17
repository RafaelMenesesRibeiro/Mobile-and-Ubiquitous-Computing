package cmov1819.p2photo.helpers.termite.tasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyException;
import java.security.PublicKey;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.ImageLoading;
import cmov1819.p2photo.helpers.managers.KeyManager;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.helpers.managers.WifiDirectManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

import static cmov1819.p2photo.helpers.ConvertUtils.bitmapToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.CatalogMerge.mergeCatalogFiles;
import static cmov1819.p2photo.helpers.interfaceimpl.P2PWebServerInterfaceImpl.getMemberPublicKey;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_ID;
import static cmov1819.p2photo.helpers.termite.Consts.CONFIRM_RCV;
import static cmov1819.p2photo.helpers.termite.Consts.OPERATION;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_UUID;
import static cmov1819.p2photo.helpers.termite.Consts.REQUEST_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_CATALOG;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.TERMITE_PORT;
import static cmov1819.p2photo.helpers.termite.Consts.USERNAME;

public class ServerTask extends AsyncTask<Void, String, Void> {
    private static final String SERVER_TAG = "SERVER SOCKET";

    private WifiDirectManager mWifiDirectManager = null;
    private KeyManager mKeyManager = null;

    @Override
    protected void onPreExecute() {
        logInfo(SERVER_TAG, "Started WiFi Direct server task (" + this.hashCode() + ").");
    }

    @Override
    protected Void doInBackground(final Void... params) {
        try {
            // get singleton instance of our WifiDirectManager
            mWifiDirectManager = WifiDirectManager.getInstance();
            mKeyManager = KeyManager.getInstance();
            // setup a server socket on MainMenuActivity
            mWifiDirectManager.setServerSocket(new SimWifiP2pSocketServer(TERMITE_PORT));
            // set server socket to listen to incoming requests
            while (!Thread.currentThread().isInterrupted()) {
                SimWifiP2pSocket socket = mWifiDirectManager.getServerSocket().accept();
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
                                doRespond(socket, processIncomingCatalog(mWifiDirectManager, request));
                                break;
                            case REQUEST_PHOTO:
                                doRespond(socket, replyWithRequestedPhoto(mWifiDirectManager, request));
                                break;
                            default:
                                doRespond(socket,"warning: '" + OPERATION + "' is unsupported...");
                                break;
                        }
                    } else {
                        doRespond(socket,"warning: requests need 'operation' field...");
                    }
                } catch (IOException ioe) {
                    Log.e(SERVER_TAG, "Error reading socket: " + ioe.getMessage());
                } catch (JSONException jsone) {
                    Log.e(SERVER_TAG, "Error reading socket: malformed json request");
                } finally {
                    socket.close();
                }
            }
        } catch (IOException ioe) {
            Log.e(SERVER_TAG, "Socket error: " + ioe.getMessage());
        }

        return null;
    }

    private String processIncomingCatalog(WifiDirectManager wiFiDirectManager, JSONObject message) throws JSONException {
        logInfo(SERVER_TAG, "Processing incoming catalog...");
        String username = message.getString(USERNAME);
        PublicKey publicKey = tryGetKeyFromLocalMaps(username);
        if (publicKey == null) {
            logWarning(SERVER_TAG,"Could not verify sender's signature...");
        } else {
            logInfo(SERVER_TAG,"Verifying sender's signature...");
            mWifiDirectManager.isValidMessage(SEND_CATALOG, message, publicKey);
            JSONObject catalogFile = message.getJSONObject(CATALOG_FILE);
            String catalogId = catalogFile.getString(CATALOG_ID);
            mergeCatalogFiles(mWifiDirectManager.getMainMenuActivity(), catalogId, catalogFile);
        }
        return "";
    }

    @Override
    protected void onPostExecute(Void result) {
        logInfo(SERVER_TAG, "WiFi Direct server task shutdown (" + this.hashCode() + ").");
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

    private PublicKey tryGetKeyFromLocalMaps(String username) {
        PublicKey key = mKeyManager.getPublicKeys().get(username);
        if (key == null) {
            try {
                key = getMemberPublicKey(mWifiDirectManager.getMainMenuActivity(), username);
            } catch (KeyException ke) {
                logWarning(SERVER_TAG, ke.getMessage());
            }
            if (key != null) {
                mKeyManager.getPublicKeys().put(username, key);
            }
        }
        return key;
    }

    private void doRespond(SimWifiP2pSocket socket, JSONObject jsonObject) throws IOException {
        doRespond(socket, jsonObject.toString());
    }

    private void doRespond(SimWifiP2pSocket socket, String string) throws IOException {
        socket.getOutputStream().write((string + CONFIRM_RCV).getBytes());
    }

}