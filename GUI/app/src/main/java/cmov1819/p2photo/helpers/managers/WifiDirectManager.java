package cmov1819.p2photo.helpers.managers;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SignatureException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.DateUtils;
import cmov1819.p2photo.helpers.termite.tasks.SendDataTask;
import cmov1819.p2photo.helpers.termite.tasks.ServerTask;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

import static cmov1819.p2photo.helpers.ConvertUtils.bitmapToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.CryptoUtils.signData;

public class WifiDirectManager {
    private static final String WIFI_DIRECT_MGR_TAG = "WIFI DIRECT MANAGER";

    public final static String ARE_YOU_GO = "areYouGO";
    public final static String CONNECT_TO_GO = "connectingToGO" ;
    public final static String LEAVE_GROUP = "memberLeaving";
    public final static String GO_LEAVE_GROUP = "goLeaving";
    public final static String SEND_CATALOG = "sendingCatalog";
    public final static String REQUEST_CATALOG = "requestingCatalog";
    public final static String SEND_PHOTO = "sendingPhoto";
    public final static String REQUEST_PHOTO = "requestingPhoto";

    private static WifiDirectManager instance;

    private final MainMenuActivity mMainMenuActivity;
    private final KeyManager mKeyManager;

    private final AtomicInteger requestId;

    private final Map<String, SimWifiP2pDevice> usernameDevice;   // username, SimWifiP2pDevice data

    /**********************************************************
     * CONSTRUCTORS
     **********************************************************/

    private WifiDirectManager(MainMenuActivity activity) {
        this.mMainMenuActivity = activity;
        this.mKeyManager = KeyManager.getInstance();
        this.requestId = new AtomicInteger(0);
        this.usernameDevice = new ConcurrentHashMap<>();
    }

    public static WifiDirectManager init(MainMenuActivity activity) {
        if (instance == null) {
            instance = new WifiDirectManager(activity);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        return instance;
    }

    public static WifiDirectManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("WifiDirectManager was not initiated before using getInstance, call init on MainMenu!");
        }
        return instance;
    }

    /**********************************************************
     * CATALOG DISTRIBUTION METHODS
     **********************************************************/

    public void pushCatalogFiles(SimWifiP2pDevice device, List<JSONObject> myCatalogFiles) {
        LogManager.logInfo(WIFI_DIRECT_MGR_TAG,"Broadcasting catalog to " + device.deviceName);
        for (JSONObject catalogFile : myCatalogFiles) {
            try {
                sendCatalog(device, catalogFile);
            } catch (RuntimeException se) {
                String msg = "One or more catalog postings have failed due to cipher fail.";
                LogManager.logError(WIFI_DIRECT_MGR_TAG, msg);
            }
        }
    }

    public void sendCatalog(final SimWifiP2pDevice device, final JSONObject catalogFile) throws RuntimeException {
        LogManager.logInfo(WIFI_DIRECT_MGR_TAG,"Sending a catalog to...");
        try {
            JSONObject jsonObject = newBaselineJson(SEND_CATALOG);
            jsonObject.put("catalogFile", catalogFile);
            doSend(device, jsonObject);
        } catch (JSONException jsone) {
            LogManager.logError(WIFI_DIRECT_MGR_TAG, jsone.getMessage());
        }
    }

    public void requestCatalog(final SimWifiP2pDevice calleeDevice, final String catalogId) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Request catalog: %s to %s", catalogId, calleeDevice.deviceName));
            JSONObject jsonObject = newBaselineJson(REQUEST_CATALOG);
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
            JSONObject jsonObject = newBaselineJson(REQUEST_PHOTO);
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
            JSONObject jsonObject = newBaselineJson(SEND_PHOTO);
            jsonObject.put("photoUuid", photoUuid);
            jsonObject.put("photo", byteArrayToBase64String(bitmapToByteArray(photo)));
            doSend(callerDevice, jsonObject);
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "Unable to form JSONObject with bitmap data");
        }
    }

    /**********************************************************
     * HELPERS
     **********************************************************/

     public JSONObject newBaselineJson(String operation) throws JSONException {
         JSONObject jsonObject = new JSONObject();
         jsonObject.put("operation", operation);
         jsonObject.put("username", SessionManager.getUsername(mMainMenuActivity));
         return jsonObject;
     }

    public void doSend(final SimWifiP2pDevice targetDevice, JSONObject data) {
        try {
            LogManager.logInfo(WIFI_DIRECT_MGR_TAG, String.format("Trying to send data to %s", targetDevice.deviceName));
            data.put("from", mMainMenuActivity.getDeviceName());
            data.put("to", targetDevice.deviceName);
            data.put("requestId", requestId.incrementAndGet());
            data.put("timestamp", DateUtils.generateTimestamp());
            data.put("signature", signData(mKeyManager.getmPrivateKey(), data));
            new SendDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, targetDevice, data);
        } catch (JSONException | SignatureException exc) {
            LogManager.logError(WIFI_DIRECT_MGR_TAG, "Unable to sign message, abort send...");
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
        new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setServerSocket(SimWifiP2pSocketServer newSocket) {
        mMainMenuActivity.setmSrvSocket(newSocket);
    }

    public Map<String, SimWifiP2pDevice> getUsernameDevice() {
        return usernameDevice;
    }
}
