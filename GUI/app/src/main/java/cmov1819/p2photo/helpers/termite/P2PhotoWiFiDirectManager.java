package cmov1819.p2photo.helpers.termite;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import javax.crypto.SecretKey;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.managers.SessionManager;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

import static cmov1819.p2photo.helpers.ConvertUtils.bitmapToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;

public class P2PhotoWiFiDirectManager {
    private static final String WIFI_DIRECT_MGR_TAG = "WIFI DIRECT MANAGER";

    private final MainMenuActivity mMainMenuActivity;

    private P2PhotoSocketManager socketManager;
    private SimWifiP2pSocketServer mServerSocket;

    /**********************************************************
     * CONSTRUCTORS
     **********************************************************/

    public P2PhotoWiFiDirectManager(MainMenuActivity activity) {
        this.mMainMenuActivity = activity;
        this.mServerSocket = null;
        this.socketManager = new P2PhotoSocketManager(this);
    }

    /**********************************************************
     * CATALOG REQUEST / RESPONSE METHODS
     **********************************************************/

    public void requestCatalog(final SimWifiP2pDevice calleeDevice, final String catalogId) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Request catalog: %s to %s", catalogId, calleeDevice.deviceName));

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("operation", "requestCatalog");
            jsonObject.put("callerUsername", SessionManager.getUsername(mMainMenuActivity));
            jsonObject.put("catalogId", catalogId);

            socketManager.doSend(calleeDevice, jsonObject.toString().getBytes("UTF-8"));
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "catalogFileContents.toString() failed resulting in exception");
        } catch (UnsupportedEncodingException uee) {
            // swallow
        }
    }

    public void sendCatalog(final String callerUsername,
                            final SimWifiP2pDevice callerDevice,
                            final JSONObject catalogFileContents) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Sending catalog to %s", callerDevice.deviceName));
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Contents:\n%s", catalogFileContents.toString(4)));

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("operation", "sendCatalog");
            jsonObject.put("callerUsername", SessionManager.getUsername(mMainMenuActivity));
            jsonObject.put("catalogFile", catalogFileContents);

            SecretKey key = generateAes256Key();
            sendKeyToP2PWebServer(key, callerUsername);

            socketManager.doSend(
                    callerDevice,
                    cipherWithAes256(jsonObject.toString().getBytes("UTF-8"), key)
            );

        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "catalogFileContents.toString() failed resulting in exception");
        } catch (UnsupportedEncodingException uee) {
            // swallow
        }
    }

    /**********************************************************
     * PHOTO REQUEST / RESPONSE METHODS
     **********************************************************/

    public void requestPhoto(final SimWifiP2pDevice calleeDevice,
                             final String catalogId,
                             final String photoUuid) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Request photo: %s to %s", photoUuid, calleeDevice.deviceName));

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("operation", "requestCatalog");
            jsonObject.put("callerUsername", SessionManager.getUsername(mMainMenuActivity));
            jsonObject.put("catalogId", catalogId);
            jsonObject.put("photoUuid", photoUuid);

            socketManager.doSend(calleeDevice, jsonObject.toString().getBytes("UTF-8"));
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "catalogFileContents.toString() failed resulting in exception");
        } catch (UnsupportedEncodingException uee) {
            // swallow
        }
    }

    public void sendPhoto(final SimWifiP2pDevice callerDevice,
                          final String photoUuid,
                          final Bitmap photo) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Sending photo to %s", callerDevice.deviceName));

            byte[] rawPhoto = bitmapToByteArray(photo);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("operation", "sendCatalog");
            jsonObject.put("callerUsername", SessionManager.getUsername(mMainMenuActivity));
            jsonObject.put("photoUuid", photoUuid);
            jsonObject.put("photo", byteArrayToBase64String(rawPhoto));

            socketManager.doSend(callerDevice, jsonObject.toString().getBytes("UTF-8"));
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "Unable to form JSONObject with bitmap data");
        } catch (UnsupportedEncodingException uee) {
            // swallow
        }
    }

    private void sendKeyToP2PWebServer(SecretKey aesKey, String recipientUsername) {
        // TODO ask server to keep this aesKey for recipient username on a Map for 10 minutes then discards it.
    }

    private SecretKey generateAes256Key() {
        // TODO
        return null;
    }

    private byte[] cipherWithAes256(byte[] bytes, SecretKey key) {
        // TODO
        return null;
    }

    /**********************************************************
     * GETTERS AND SETTERS
     **********************************************************/

    public P2PhotoSocketManager getSocketManager() {
        return socketManager;
    }

    public void setSocketManager(P2PhotoSocketManager socketManager) {
        this.socketManager = socketManager;
    }

    public SimWifiP2pSocketServer getServerSocket() {
        return mServerSocket;
    }

    public void setServerSocket() {
        new IncomingSocketTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
    }

    public void setServerSocket(SimWifiP2pSocketServer newSocket) {
        this.mServerSocket = newSocket;
    }
}
