package cmov1819.p2photo.helpers.managers;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.PublicKey;
import java.security.SignatureException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.SecretKey;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.DateUtils;
import cmov1819.p2photo.helpers.callables.CallableManager;
import cmov1819.p2photo.helpers.callables.GetPhotoFromPeerCallable;
import cmov1819.p2photo.helpers.termite.tasks.SendDataTask;
import cmov1819.p2photo.helpers.termite.tasks.ServerTask;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

import static cmov1819.p2photo.helpers.ConvertUtils.bitmapToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.CryptoUtils.signData;
import static cmov1819.p2photo.helpers.CryptoUtils.verifySignatureWithSHA1withRSA;
import static cmov1819.p2photo.helpers.DateUtils.isFreshTimestamp;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.FROM;
import static cmov1819.p2photo.helpers.termite.Consts.OPERATION;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_UUID;
import static cmov1819.p2photo.helpers.termite.Consts.RID;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_CATALOG;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.SIGNATURE;
import static cmov1819.p2photo.helpers.termite.Consts.TIMESTAMP;
import static cmov1819.p2photo.helpers.termite.Consts.TO;
import static cmov1819.p2photo.helpers.termite.Consts.USERNAME;

public class WifiDirectManager {
    private static final String WIFI_DIRECT_MGR_TAG = "WIFI DIRECT MANAGER";

    private static WifiDirectManager instance;

    private final MainMenuActivity mMainMenuActivity;
    private final KeyManager mKeyManager;

    private final AtomicInteger requestId;

    private final Map<String, SimWifiP2pDevice> usernameDeviceMap;   // username, SimWifiP2pDevice
    private final Map<String, String> deviceUsernameMap;             // SimWifiP2pDevice.deviceName, username

    /**********************************************************
     * CONSTRUCTORS
     **********************************************************/

    private WifiDirectManager(MainMenuActivity activity) {
        this.mMainMenuActivity = activity;
        this.mKeyManager = KeyManager.getInstance();
        this.requestId = new AtomicInteger(0);
        this.usernameDeviceMap = new ConcurrentHashMap<>();
        this.deviceUsernameMap = new ConcurrentHashMap<>();
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
            jsonObject.put(CATALOG_FILE, catalogFile);
            doSend(device, jsonObject);
        } catch (JSONException jsone) {
            LogManager.logError(WIFI_DIRECT_MGR_TAG, jsone.getMessage());
        }
    }

    /**********************************************************
     * PHOTO DISTRIBUTION METHODS
     **********************************************************/

    public static void pullPhotos(final List<String> missingPhotos, final String catalogId) {
        new AsyncTask<Void, Void, Void>() {
            // TODO Obtain negotiated session keys;

            @Override
            protected Void doInBackground(Void... voids) {
                WifiDirectManager mWifiDirectManager = WifiDirectManager.getInstance();
                List<SimWifiP2pDevice> mGroup = mWifiDirectManager.getMainMenuActivity().getmGroupPeers();

                int missingPhotosCount = missingPhotos.size();

                ExecutorService executorService = Executors.newFixedThreadPool(missingPhotosCount);
                ExecutorCompletionService<String> completionService = new ExecutorCompletionService<>(executorService);

                for (SimWifiP2pDevice device : mGroup) {
                    String user = mWifiDirectManager.getDeviceUsernameMap().get(device.deviceName);
                    PublicKey ownerPublicKey = KeyManager.getInstance().getPublicKeys().get(user);
                    SecretKey sessionKey = KeyManager.getInstance().getSessionKeys().get(user);

                    for (String missingPhoto : missingPhotos) {
                        Callable<String> job =
                                new GetPhotoFromPeerCallable(device, user, missingPhoto, catalogId, sessionKey,ownerPublicKey);
                        completionService.submit(new CallableManager(job,20, TimeUnit.SECONDS));
                    }

                    for (int i = 0; i < missingPhotosCount; i++) {
                        try {
                            Future<String> futureResult = completionService.take();
                            if (!futureResult.isCancelled()) {
                                String result = futureResult.get();
                                if (result != null) {
                                    missingPhotos.remove(result);
                                }
                            }
                        } catch (Exception exc) {
                            // swallow
                        }
                    }
                    missingPhotosCount = missingPhotos.size();
                }

                return null;
            }
        }.execute();
    }

    public void sendPhoto(final SimWifiP2pDevice callerDevice, final String photoUuid, final Bitmap photo) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Sending photo to %s", callerDevice.deviceName));
            JSONObject jsonObject = newBaselineJson(SEND_PHOTO);
            jsonObject.put(PHOTO_UUID, photoUuid);
            jsonObject.put(PHOTO_FILE, byteArrayToBase64String(bitmapToByteArray(photo)));
            doSend(callerDevice, jsonObject);
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "Unable to form JSONObject with bitmap data");
        }
    }

    /**********************************************************
     * HELPERS
     **********************************************************/

    public boolean isValidMessage(String sender, int rid, JSONObject response) {
        try {
            if (!response.getString(USERNAME).equals(sender)) {
                return false;
            }
            if (response.getInt(RID) != rid) {
                return  false;
            }
            return true;
        } catch (JSONException jsone) {
            return false;
        }
    }
    public boolean isValidMessage(String operation, JSONObject response, PublicKey publicKey) {
        try {
            if (!response.getString(OPERATION).equals(operation)) {
                return false;
            }
            if (!isFreshTimestamp(response.getString(TIMESTAMP))) {
                return false;
            }
            if (!verifySignatureWithSHA1withRSA(publicKey, response)) {
                return false;
            }
            return true;
        } catch (JSONException jsone) {
            return false;
        }
    }

    public JSONObject newBaselineJson(String operation) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(OPERATION, operation);
        jsonObject.put(USERNAME, SessionManager.getUsername(mMainMenuActivity));
        return jsonObject;
    }

    public void doSend(final SimWifiP2pDevice targetDevice, JSONObject data) {
        conformToTLSBeforeSend(targetDevice, data, requestId.incrementAndGet());
        new SendDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, targetDevice, data);
    }

    public JSONObject conformToTLSBeforeSend(SimWifiP2pDevice targetDevice, JSONObject data, int rid){
        try {
            LogManager.logInfo(WIFI_DIRECT_MGR_TAG, String.format("Trying to send data to %s", targetDevice.deviceName));
            data.put(RID, rid);
            data.put(FROM, mMainMenuActivity.getDeviceName());
            data.put(TO, targetDevice.deviceName);
            data.put(TIMESTAMP, DateUtils.generateTimestamp());
            data.put(SIGNATURE, signData(mKeyManager.getmPrivateKey(), data));
            return data;
        } catch (JSONException | SignatureException exc) {
            LogManager.logError(WIFI_DIRECT_MGR_TAG, "Unable to sign message, abort send...");
        }
        return null;
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

    public int getRequestId() {
        return requestId.incrementAndGet();
    }

    public void setServerSocket() {
        new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setServerSocket(SimWifiP2pSocketServer newSocket) {
        mMainMenuActivity.setmSrvSocket(newSocket);
    }

    public Map<String, SimWifiP2pDevice> getUsernameDeviceMap() {
        return usernameDeviceMap;
    }


    public Map<String, String> getDeviceUsernameMap() {
        return deviceUsernameMap;
    }
}
