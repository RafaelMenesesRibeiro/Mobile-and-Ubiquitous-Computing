package cmov1819.p2photo.helpers.termite;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.CryptoUtils;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.SessionManager;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

import static cmov1819.p2photo.helpers.ConvertUtils.bitmapToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.CryptoUtils.cipherWithAes256;

public class P2PhotoWiFiDirectManager {
    private static final String WIFI_DIRECT_MGR_TAG = "WIFI DIRECT MANAGER";

    private final MainMenuActivity mMainMenuActivity;

    private SimWifiP2pSocketServer mServerSocket = null;

    /**********************************************************
     * CONSTRUCTORS
     **********************************************************/

    public P2PhotoWiFiDirectManager(MainMenuActivity activity) {
        this.mMainMenuActivity = activity;
    }

    /**********************************************************
     * GENERIC METHODS
     *********************************************************/

    public void doSend(final SimWifiP2pDevice targetDevice, final byte[] data) {
        LogManager.logInfo(WIFI_DIRECT_MGR_TAG, String.format("Trying to send data to %s", targetDevice.deviceName));
        new SendDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, targetDevice, data);
    }

    /**********************************************************
     * CATALOG DISTRIBUTION METHODS
     **********************************************************/

    public void pushCatalogFiles(SimWifiP2pDevice device, List<JSONObject> myCatalogFiles) {
        for (JSONObject catalogFile : myCatalogFiles) {
            try {
                sendCatalog(device, catalogFile);
            } catch (RuntimeException se) {
                LogManager.logError(WIFI_DIRECT_MGR_TAG, "One or more catalog postages has failed due to cipher fail.");
            }
        }
    }

    public void sendCatalog(final SimWifiP2pDevice targetDevice,
                            final JSONObject catalogFileContents) throws RuntimeException {

        // This token can be used by the targetDevice to obtain the AES Key from the server
        LogManager.logInfo(WIFI_DIRECT_MGR_TAG, "Generating AES256 Key and corresponding retrieval Token...");
        String token = UUID.randomUUID().toString();
        SecretKey key = CryptoUtils.generateAes256Key();
        // Ask the server temporarily store the AES Key associated with this token
        registerKeyOnWebServer(targetDevice.deviceName, token, key);

        JSONObject jsonObject = new JSONObject();
        try {
            LogManager.logInfo(WIFI_DIRECT_MGR_TAG,
                    String.format(
                            "Sending catalog to %s\nContents:\n%s\n",
                            targetDevice.deviceName,
                            catalogFileContents.toString(4))
            );

            String cipheredCatalogFile = byteArrayToBase64String(
                    cipherWithAes256(key, catalogFileContents.toString().getBytes("UTF-8"))
            );

            jsonObject.put("operation", "sendingCatalog");
            jsonObject.put("from", SessionManager.getUsername(mMainMenuActivity));
            jsonObject.put("catalogFile", cipheredCatalogFile);
            jsonObject.put("token", token);
            doSend(targetDevice, jsonObject.toString().getBytes("UTF-8"));

        } catch (JSONException jsone) {
            LogManager.logError(WIFI_DIRECT_MGR_TAG, jsone.getMessage());
        } catch (UnsupportedEncodingException uee) {
            LogManager.logError(WIFI_DIRECT_MGR_TAG, uee.getMessage());
        }
    }

    private void registerKeyOnWebServer(final String deviceName,
                                        final String token,
                                        final SecretKey aesKey) {
        LogManager.logInfo(WIFI_DIRECT_MGR_TAG, "Request server to save token: " + token);
        // TODO ask server to keep this aesKey for recipient username on a Map for 10 minutes then discards it.
    }

    public void pullMissingCatalogFiles(List<SimWifiP2pDevice> mGroupPeers, List<String> myMissingCatalogFiles) {
        // TODO for each missing catalog file requestCatalog to group peers.
    }

    public void requestCatalog(final SimWifiP2pDevice calleeDevice, final String catalogId) {
        Log.i(WIFI_DIRECT_MGR_TAG, String.format("Request catalog: %s to %s", catalogId, calleeDevice.deviceName));
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("operation", "requestingCatalog");
            jsonObject.put("from", SessionManager.getUsername(mMainMenuActivity));
            jsonObject.put("catalogId", catalogId);
            doSend(calleeDevice, jsonObject.toString().getBytes("UTF-8"));
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, jsone.getMessage());
        } catch (UnsupportedEncodingException uee) {
            LogManager.logWarning(WIFI_DIRECT_MGR_TAG, uee.getMessage());
            doSend(calleeDevice, jsonObject.toString().getBytes());
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
            jsonObject.put("operation", "requestingPhoto");
            jsonObject.put("from", SessionManager.getUsername(mMainMenuActivity));
            jsonObject.put("catalogId", catalogId);
            jsonObject.put("photoUuid", photoUuid);

            doSend(calleeDevice, jsonObject.toString().getBytes("UTF-8"));
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
            jsonObject.put("operation", "sendingPhoto");
            jsonObject.put("from", SessionManager.getUsername(mMainMenuActivity));
            jsonObject.put("photoUuid", photoUuid);
            jsonObject.put("photo", byteArrayToBase64String(rawPhoto));

            doSend(callerDevice, jsonObject.toString().getBytes("UTF-8"));
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "Unable to form JSONObject with bitmap data");
        } catch (UnsupportedEncodingException uee) {
            // swallow
        }
    }

    /**********************************************************
     * GETTERS AND SETTERS
     **********************************************************/

    public MainMenuActivity getMainMenuActivity() {
        return mMainMenuActivity;
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
