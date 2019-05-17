package cmov1819.p2photo.helpers.managers;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.DateUtils;
import cmov1819.p2photo.helpers.callables.ReceivePhotoCallableManager;
import cmov1819.p2photo.helpers.callables.ProposeSessionCallable;
import cmov1819.p2photo.helpers.callables.ProposeSessionCallableManager;
import cmov1819.p2photo.helpers.callables.ReceivePhotoCallable;
import cmov1819.p2photo.helpers.termite.tasks.SendDataTask;
import cmov1819.p2photo.helpers.termite.tasks.ServerTask;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

import static cmov1819.p2photo.helpers.ConvertUtils.bitmapToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.CryptoUtils.signData;
import static cmov1819.p2photo.helpers.CryptoUtils.verifySignatureWithSHA1withRSA;
import static cmov1819.p2photo.helpers.DateUtils.isFreshTimestamp;
import static cmov1819.p2photo.helpers.managers.LogManager.WIFI_DIRECT_MGR_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.managers.LogManager.toast;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.FROM;
import static cmov1819.p2photo.helpers.termite.Consts.GO_LEAVE_GROUP;
import static cmov1819.p2photo.helpers.termite.Consts.LEAVE_GROUP;
import static cmov1819.p2photo.helpers.termite.Consts.OPERATION;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_UUID;
import static cmov1819.p2photo.helpers.termite.Consts.RID;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_CATALOG;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.SIGNATURE;
import static cmov1819.p2photo.helpers.termite.Consts.TIMESTAMP;
import static cmov1819.p2photo.helpers.termite.Consts.TO;

public class WifiDirectManager {

    private static WifiDirectManager instance;

    private final MainMenuActivity mMainMenuActivity;
    private final KeyManager mKeyManager;

    private final AtomicInteger requestId;

    private final Map<String, SimWifiP2pDevice> usernameDeviceMap;

    private boolean isLeader;

    /**********************************************************
     * CONSTRUCTORS
     **********************************************************/

    private WifiDirectManager(MainMenuActivity activity) {
        this.mMainMenuActivity = activity;
        this.mKeyManager = KeyManager.getInstance();
        this.requestId = new AtomicInteger(0);
        this.usernameDeviceMap = new ConcurrentHashMap<>();
        this.isLeader = false;
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
        logInfo(WIFI_DIRECT_MGR_TAG,"Broadcasting catalog to " + device.deviceName);
        for (JSONObject catalogFile : myCatalogFiles) {
            try {
                sendCatalog(device, catalogFile);
            } catch (RuntimeException se) {
                String msg = "One or more catalog postings have failed due to cipher fail.";
                logError(WIFI_DIRECT_MGR_TAG, msg);
                toast(this.mMainMenuActivity,msg);
            }
        }
    }

    public void sendCatalog(final SimWifiP2pDevice device, final JSONObject catalogFile) throws RuntimeException {
        logInfo(WIFI_DIRECT_MGR_TAG,"Sending a catalog to..."  + device.deviceName);
        try {
            JSONObject jsonObject = newBaselineJson(SEND_CATALOG);
            jsonObject.put(CATALOG_FILE, catalogFile);
            doSend(device, jsonObject);
        } catch (JSONException jsone) {
            toast(this.mMainMenuActivity, jsone.getMessage());
            logError(WIFI_DIRECT_MGR_TAG, jsone.getMessage());
        }
    }

    /**********************************************************
     * PHOTO DISTRIBUTION METHODS
     **********************************************************/

    public static void pullPhotos(final List<String> missingPhotos, final String catalogId) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                WifiDirectManager mWifiDirectManager = WifiDirectManager.getInstance();
                List<SimWifiP2pDevice> mGroup = mWifiDirectManager.getMainMenuActivity().getmGroupPeers();

                int missingPhotosCount = missingPhotos.size();

                ExecutorService executorService = Executors.newFixedThreadPool(missingPhotosCount);
                ExecutorCompletionService<String> completionService = new ExecutorCompletionService<>(executorService);

                for (SimWifiP2pDevice device : mGroup) {


                    for (String missingPhoto : missingPhotos) {
                        Callable<String> job = new ReceivePhotoCallable(device, missingPhoto, catalogId);
                        completionService.submit(new ReceivePhotoCallableManager(job,30, TimeUnit.SECONDS));
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
                        } catch (ExecutionException | InterruptedException exc) {
                            logWarning(WIFI_DIRECT_MGR_TAG, "A photo download may have been interrupted or timed out!");
                        }
                    }
                    missingPhotosCount = missingPhotos.size();
                }

                return null;
            }
        }.execute();
    }

    public void sendPhoto(final SimWifiP2pDevice device, final String photoUuid, final Bitmap photo) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Sending photo to %s", device.deviceName));
            JSONObject jsonObject = newBaselineJson(SEND_PHOTO);
            jsonObject.put(PHOTO_UUID, photoUuid);
            jsonObject.put(PHOTO_FILE, byteArrayToBase64String(bitmapToByteArray(photo)));
            doSend(device, jsonObject);
        } catch (JSONException jsone) {
            Log.e(WIFI_DIRECT_MGR_TAG, "Unable to form JSONObject with bitmap data");
        }
    }

    /**********************************************************
     * SESSION KEY DISTRIBUTION METHODS
     **********************************************************/

    public void negotiateSessions(List<SimWifiP2pDevice> oldGroup, List<SimWifiP2pDevice> newGroup) {
        List<SimWifiP2pDevice> targetDevices = new ArrayList<>();
        for (SimWifiP2pDevice targetDevice : oldGroup) {
            if (!newGroup.contains(targetDevice)) {
                usernameDeviceMap.remove(targetDevice.deviceName);
                mKeyManager.getSessionKeys().remove(targetDevice.deviceName);
            }
        }
        for (SimWifiP2pDevice targetDevice : newGroup) {
            if (!oldGroup.contains(targetDevice)) {
                if (getDeviceName().compareTo(targetDevice.deviceName) > 0) {
                    targetDevices.add(targetDevice);
                }
            }
        }
        proposeSession(targetDevices);
    }

    private static void proposeSession(final List<SimWifiP2pDevice> targetDevices) {
        new AsyncTask<Void, Void, List<SimWifiP2pDevice>>() {
            @Override
            protected List<SimWifiP2pDevice> doInBackground(Void... voids) {
                int devicesInNeedOfSessionEstablishment = targetDevices.size();

                ExecutorService executorService = Executors.newFixedThreadPool(devicesInNeedOfSessionEstablishment);
                ExecutorCompletionService<SimWifiP2pDevice> completionService = new ExecutorCompletionService<>(executorService);

                for (SimWifiP2pDevice device : targetDevices) {
                    Callable<SimWifiP2pDevice> job = new ProposeSessionCallable(device);
                    completionService.submit(new ProposeSessionCallableManager(job,30, TimeUnit.SECONDS));
                }

                List<SimWifiP2pDevice> devicesNeedingSecondAttempt = new ArrayList<>();
                for (int i = 0; i < devicesInNeedOfSessionEstablishment; i++) {
                    try {
                        Future<SimWifiP2pDevice> futureResult = completionService.take();
                        if (!futureResult.isCancelled()) {
                            SimWifiP2pDevice result = futureResult.get();
                            if (result != null) {
                                devicesNeedingSecondAttempt.add(result);
                            }
                        }
                    } catch (ExecutionException | InterruptedException exc) {
                        logWarning(WIFI_DIRECT_MGR_TAG, "A photo download may have been interrupted or timed out!");
                    }
                }

                return devicesNeedingSecondAttempt;
            }

            @Override
            protected void onPostExecute(List<SimWifiP2pDevice> simWifiP2pDevices) {
                super.onPostExecute(simWifiP2pDevices);
                if (!simWifiP2pDevices.isEmpty()) {
                    proposeSession(simWifiP2pDevices);
                }
            }

        }.execute();
    }

    /**********************************************************
     * GROUP CHANGING METHODS
     **********************************************************/

    public void leaveGroup(final SimWifiP2pDevice device) {
        try {
            Log.i(WIFI_DIRECT_MGR_TAG, String.format("Notifying leaving group %s", device.deviceName));
            if (isLeader) {
                JSONObject jsonObject = newBaselineJson(GO_LEAVE_GROUP);
                doSend(device, jsonObject);
            }
            else {
                JSONObject jsonObject = newBaselineJson(LEAVE_GROUP);
                doSend(device, jsonObject);
            }
        }
        catch (JSONException ex) {
            Log.e(WIFI_DIRECT_MGR_TAG, "Unable to build to form JSONObject");
        }
    }

    /**********************************************************
     * HELPERS
     **********************************************************/

    public boolean isValidMessage(String sender, int rid, JSONObject response) {
        try {
            if (!response.getString(FROM).equals(sender)) {
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

    public boolean isValidResponse(JSONObject response, String operation, int rid, PublicKey senderPublicKey) {
        if (!isValidMessage(getDeviceName(), rid, response)) {
            return false;
        }
        if (!isValidMessage(operation, response, senderPublicKey)) {
            return false;
        }
        return true;
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
        jsonObject.put(FROM, SessionManager.getUsername(mMainMenuActivity));
        return jsonObject;
    }

    public void doSend(final SimWifiP2pDevice targetDevice, JSONObject data) {
        try {
            conformToTLS(data, requestId.incrementAndGet(), targetDevice.deviceName);
            new SendDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, targetDevice, data);
        } catch (JSONException | SignatureException exc) {
            logError(WIFI_DIRECT_MGR_TAG, "Unable to conform to TLS. Aborting send request...");
        }
    }

    public void conformToTLS(JSONObject data, int rid, String to) throws JSONException, SignatureException {
        data.put(RID, rid);
        data.put(TO, to);
        data.put(TIMESTAMP, DateUtils.generateTimestamp());
        data.put(SIGNATURE, signData(mKeyManager.getmPrivateKey(), data));
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

    public String getDeviceName() {
        return mMainMenuActivity.getDeviceName();
    }

    private String getMyUsername() {
        return SessionManager.getUsername(mMainMenuActivity);
    }
}
