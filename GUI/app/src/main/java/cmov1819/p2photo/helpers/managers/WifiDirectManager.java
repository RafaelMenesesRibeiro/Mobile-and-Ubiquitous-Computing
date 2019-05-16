package cmov1819.p2photo.helpers.managers;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.CryptoUtils;
import cmov1819.p2photo.helpers.termite.tasks.IncomingSocketTask;
import cmov1819.p2photo.helpers.termite.tasks.SendDataTask;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

import static cmov1819.p2photo.helpers.ConvertUtils.bitmapToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.CryptoUtils.cipherWithAes;

public class WifiDirectManager {
    private static final String WIFI_DIRECT_MGR_TAG = "WIFI DIRECT MANAGER";
    private static WifiDirectManager instance;
    private final MainMenuActivity mMainMenuActivity;

    /**********************************************************
     * CONSTRUCTORS
     **********************************************************/

    private WifiDirectManager(MainMenuActivity activity) {
        this.mMainMenuActivity = activity;
    }

    public static WifiDirectManager init(MainMenuActivity activity) {
        if (instance == null) {
            instance = new WifiDirectManager(activity);
            new IncomingSocketTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        return instance;
    }

    public static WifiDirectManager getInstance() {
        return instance;
    }

    /**********************************************************
     * GENERIC METHODS
     *********************************************************/

    public void doSend(final SimWifiP2pDevice targetDevice, JSONObject data) {
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

    public void sendCatalog(final SimWifiP2pDevice targetDevice, final JSONObject catalogFileContents) throws RuntimeException {
        // This token can be used by the targetDevice to obtain the AES Key from the server
        // TODO Exchange this code with TLS logic
        LogManager.logInfo(WIFI_DIRECT_MGR_TAG, "Generating AES256 Key and corresponding retrieval Token...");
        String token = UUID.randomUUID().toString();
        SecretKey key = CryptoUtils.generateAesKey();
        // Ask the server temporarily store the AES Key associated with this token
        registerKeyOnWebServer(targetDevice.deviceName, token, key);

        try {
            String unformatedLog = "Sending catalog to %s\nContents:\n%s\n";
            String identedCatalogFileContents = catalogFileContents.toString(4);
            LogManager.logInfo(WIFI_DIRECT_MGR_TAG,String.format(unformatedLog, targetDevice.deviceName, identedCatalogFileContents));

            byte[] encodedCatalog = cipherWithAes(key, catalogFileContents.toString().getBytes());
            String cipheredCatalogFile = byteArrayToBase64String(encodedCatalog);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("operation", "sendingCatalog");
            jsonObject.put("from", SessionManager.getUsername(mMainMenuActivity));
            jsonObject.put("catalogFile", cipheredCatalogFile);
            jsonObject.put("token", token);
            doSend(targetDevice, jsonObject);
        } catch (JSONException jsone) {
            LogManager.logError(WIFI_DIRECT_MGR_TAG, jsone.getMessage());
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
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Request catalog: %s to %s", catalogId, calleeDevice.deviceName));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("operation", "requestingCatalog");
            jsonObject.put("from", SessionManager.getUsername(mMainMenuActivity));
            jsonObject.put("catalogId", catalogId);
            doSend(calleeDevice, jsonObject);
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, jsone.getMessage());
        }
    }

    /**********************************************************
     * PHOTO REQUEST / RESPONSE METHODS
     **********************************************************/

    public void requestPhoto(final SimWifiP2pDevice calleeDevice, final String catalogId, final String photoUuid) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Request photo: %s to %s", photoUuid, calleeDevice.deviceName));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("operation", "requestingPhoto");
            jsonObject.put("from", SessionManager.getUsername(mMainMenuActivity));
            jsonObject.put("catalogId", catalogId);
            jsonObject.put("photoUuid", photoUuid);
            doSend(calleeDevice, jsonObject);
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "catalogFileContents.toString() failed resulting in exception");
        }
    }

    public void sendPhoto(final SimWifiP2pDevice callerDevice, final String photoUuid, final Bitmap photo) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Sending photo to %s", callerDevice.deviceName));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("operation", "sendingPhoto");
            jsonObject.put("from", SessionManager.getUsername(mMainMenuActivity));
            jsonObject.put("photoUuid", photoUuid);
            jsonObject.put("photo", byteArrayToBase64String(bitmapToByteArray(photo)));
            doSend(callerDevice, jsonObject);
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "Unable to form JSONObject with bitmap data");
        }
    }

    /**********************************************************
     * GETTERS AND SETTERS
     **********************************************************/

    public MainMenuActivity getMainMenuActivity() {
        return mMainMenuActivity;
    }

    public SimWifiP2pSocketServer getServerSocket() {
        return mMainMenuActivity.getmSrvSocket();
    }

    public void setServerSocket() {
        new IncomingSocketTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setServerSocket(SimWifiP2pSocketServer newSocket) {
        mMainMenuActivity.setmSrvSocket(newSocket);
    }
}