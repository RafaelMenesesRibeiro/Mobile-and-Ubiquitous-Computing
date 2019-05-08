package cmov1819.p2photo.helpers.termite;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.mortbay.thread.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

import javax.crypto.SecretKey;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.ConvertUtils;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

public class P2PhotoWiFiDirectManager {
    private static final String WIFI_DIRECT_MGR_TAG = "WIFI DIRECT MANAGER";

    private final MainMenuActivity mMainMenuActivity;
    private final String mUsername;
    private final String mMacAddress;

    private P2PhotoSocketManager socketManager;
    private SimWifiP2pSocketServer mServerSocket;

    /**********************************************************
     * CONSTRUCTORS
     **********************************************************/

    public P2PhotoWiFiDirectManager(MainMenuActivity activity, String username, String macAddress) {
        this.mMainMenuActivity = activity;
        this.mUsername = username;
        this.mMacAddress = macAddress;
        this.mServerSocket = null;
        this.socketManager = new P2PhotoSocketManager(this);
    }

    /**********************************************************
     * REGULAR METHODS
     **********************************************************/

    public void requestCatalog(final SimWifiP2pDevice targetDevice, final String catalogId) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Request catalog: %s to %s", catalogId, targetDevice.deviceName));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("catalogId", catalogId);
            socketManager.doSend(targetDevice, jsonObject.toString(4).getBytes("UTF-8"));
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "catalogFileContents.toString() failed resulting in exception");
        } catch (UnsupportedEncodingException uee) {
            // swallow
        }

    }

    public void sendCatalog(final SimWifiP2pDevice targetDevice, final JSONObject catalogFileContents) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Sending catalog to %s", targetDevice.deviceName));
            String jsonString = catalogFileContents.toString(4);
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Contents:\n%s\n ciphering and sending...", jsonString));
            SecretKey aesKey = generateAes256Key();
            sendKeyToP2PWebServer(aesKey, targetDevice.deviceName); // TODO Exchange deviceName with actual dest username
            socketManager.doSend(targetDevice, cipherWithAes256(jsonString.getBytes("UTF-8"), aesKey));
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "catalogFileContents.toString() failed resulting in exception");
        } catch (UnsupportedEncodingException uee) {
            // swallow
        }
    }

    public void sendPhoto(final SimWifiP2pDevice targetDevice, final Bitmap photo) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Sending photo to %s", targetDevice.deviceName));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.PNG,100, byteArrayOutputStream);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("photo", ConvertUtils.byteArrayToBase64String(byteArrayOutputStream.toByteArray()));
            socketManager.doSend(targetDevice, jsonObject.toString(4).getBytes("UTF-8"));
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

    public String getmUsername() {
        return mUsername;
    }

    public String getmMacAddress() {
        return mMacAddress;
    }

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
        new IncommingSocketTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
    }

    public void setServerSocket(SimWifiP2pSocketServer newSocket) {
        this.mServerSocket = newSocket;
    }
}
