package cmov1819.p2photo.helpers.termite;

import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import javax.crypto.SecretKey;

import cmov1819.p2photo.MainMenuActivity;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

public class P2PhotoWiFiDirectManager {
    private static final String WIFI_DIRECT_MGR_TAG = "WIFI DIRECT MANAGER";

    private final MainMenuActivity mMainMenuActivity;
    private final String mUsername;
    private final String mMacAddress;

    private SimWifiP2pSocketServer mServerSocket;
    private SimWifiP2pSocket mClientSocket;
    private P2PhotoSocketManager socketManager;

    /**********************************************************
     * CONSTRUCTORS
     **********************************************************/

    public P2PhotoWiFiDirectManager(MainMenuActivity activity, String username, String macAddress) {
        this.mMainMenuActivity = activity;
        this.mUsername = username;
        this.mMacAddress = macAddress;
        this.mServerSocket = null;
        this.mClientSocket = null;
        this.socketManager = new P2PhotoSocketManager(activity, this);
    }

    /**********************************************************
     * REGULAR METHODS
     **********************************************************/

    public void requestCatalog(final SimWifiP2pDevice targetDevice, final String catalogId) {
        Log.i(WIFI_DIRECT_MGR_TAG, String.format("Request catalog: %s to %s", catalogId, targetDevice.deviceName));
        socketManager.doSend(targetDevice, catalogId.getBytes());
    }

    public void sendCatalog(final SimWifiP2pDevice targetDevice, final JSONObject catalogFileContents) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Sending catalog to %s", targetDevice.deviceName));
            String jsonString = catalogFileContents.toString(4);
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Contents:\n%s\n ciphering and sending...", jsonString));
            SecretKey aesKey = generateAes256Key();
            sendKeyToP2PWebServer(aesKey, targetDevice.deviceName); // TODO Exchange deviceName with actual dest username
            socketManager.doSend(targetDevice, cipherWithAes256(jsonString.getBytes(), aesKey));
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "catalogFileContents.toString() failed resulting in exception");
        }
    }

    public void sendPhoto(final SimWifiP2pDevice targetDevice, final Bitmap photo) {
        Log.i(WIFI_DIRECT_MGR_TAG, String.format("Sending photo to %s", targetDevice.deviceName));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.PNG,100, byteArrayOutputStream);
        socketManager.doSend(targetDevice, byteArrayOutputStream.toByteArray());
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

    public void setServerSocket(SimWifiP2pSocketServer newSocket) {
        this.mServerSocket = newSocket;
    }

    public SimWifiP2pSocket getClientSocket() {
        return mClientSocket;
    }

    public void setClientSocket(SimWifiP2pSocket mClientSocket) {
        this.mClientSocket = mClientSocket;
    }
}
