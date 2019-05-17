package cmov1819.p2photo.helpers.callables;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.util.concurrent.Callable;

import javax.crypto.SecretKey;

import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.WifiDirectManager;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToByteArray;
import static cmov1819.p2photo.helpers.CryptoUtils.decipherWithAes;
import static cmov1819.p2photo.helpers.CryptoUtils.verifySignatureWithSHA1withRSA;
import static cmov1819.p2photo.helpers.DateUtils.isFreshTimestamp;
import static cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.ImageLoading.savePhoto;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_ID;
import static cmov1819.p2photo.helpers.termite.Consts.OPERATION;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_UUID;
import static cmov1819.p2photo.helpers.termite.Consts.REQUEST_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.RID;
import static cmov1819.p2photo.helpers.termite.Consts.SEND;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.TERMITE_PORT;
import static cmov1819.p2photo.helpers.termite.Consts.TIMESTAMP;
import static cmov1819.p2photo.helpers.termite.Consts.USERNAME;

public class GetPhotoFromPeerCallable implements Callable<String> {
    private static final String GET_PHOTO_FROM_PEER_TAG = "GetPhotoFromPeer";

    private WifiDirectManager wifiDirectManager;
    private SimWifiP2pDevice device;
    private String username;
    private String photoUuid;
    private String catalogId;
    private SecretKey sessionKey;
    private PublicKey publicKey;
    private int rid;

    public GetPhotoFromPeerCallable(SimWifiP2pDevice device,
                                    String username,
                                    String photoUuid,
                                    String catalogId,
                                    SecretKey sessionKey,
                                    PublicKey publicKey) {

        this.wifiDirectManager = WifiDirectManager.getInstance();
        this.device = device;
        this.username = username;
        this.photoUuid = photoUuid;
        this.catalogId = catalogId;
        this.sessionKey = sessionKey;
        this.publicKey = publicKey;
        this.rid = wifiDirectManager.getRequestId();
    }

    @Override
    public String call() {
        if (requestPhoto()) {
            return photoUuid;
        }
        return null;
    }

    private boolean requestPhoto() {
        try {
            Log.i(GET_PHOTO_FROM_PEER_TAG, String.format("Request photo: %s to %s", photoUuid, device.deviceName));
            JSONObject jsonObject = wifiDirectManager.newBaselineJson(REQUEST_PHOTO);
            jsonObject.put(CATALOG_ID, catalogId);
            jsonObject.put(PHOTO_UUID, photoUuid);
            wifiDirectManager.conformToTLSBeforeSend(device, jsonObject, rid);
            return doSend(jsonObject);
        } catch (JSONException jsone) {
            Log.e(GET_PHOTO_FROM_PEER_TAG, "catalogFileContents.toString() failed resulting in exception");
        }
        return false;
    }

    private boolean doSend(JSONObject jsonData) {
        SimWifiP2pSocket clientSocket = null;
        try {
            // Construct a new clientSocket and send request
            LogManager.logInfo(GET_PHOTO_FROM_PEER_TAG, "Creating client socket to " + device.deviceName + "...");
            clientSocket = new SimWifiP2pSocket(device.getVirtIp(), TERMITE_PORT);
            clientSocket.getOutputStream().write((jsonData.toString() + SEND).getBytes());
            InputStream inputStream = clientSocket.getInputStream();
            // Read response
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String encodedResponse = bufferedReader.readLine();
            byte[] decodedResponse = decipherWithAes(sessionKey, base64StringToByteArray(encodedResponse));
            JSONObject response = new JSONObject(new String(decodedResponse));
            // Process response
            if (isValidResponse(response)) {
                return trySaveIncomingPhoto(response);
            }
            LogManager.logInfo(GET_PHOTO_FROM_PEER_TAG, "Operation completed...");
        } catch (IOException ioe) {
            Log.e(GET_PHOTO_FROM_PEER_TAG, "Error: " + ioe.getMessage());
        } catch (Exception exc) {
            LogManager.logError(GET_PHOTO_FROM_PEER_TAG, exc.getMessage());
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
            } catch (IOException ioe) {
                LogManager.logError(GET_PHOTO_FROM_PEER_TAG, ioe.getMessage());
            }
        }
        return false;
    }

    public boolean isValidResponse(JSONObject response) {
        try {
            if (!response.getString(OPERATION).equals(SEND_PHOTO)) {
                return false;
            }

            if (!response.getString(USERNAME).equals(username)) {
                return false;
            }

            if (response.getInt(RID) != rid) {
                return  false;
            }

            if (!isFreshTimestamp(response.getString(TIMESTAMP))) {
                return false;
            }

            return verifySignatureWithSHA1withRSA(publicKey, response);
        } catch (JSONException jsone) {
            return false;
        }
    }

    private boolean trySaveIncomingPhoto(JSONObject jsonObject) throws JSONException {
        logInfo(GET_PHOTO_FROM_PEER_TAG, "Processing incoming photo...");
        try {
            String photoUuid = jsonObject.getString(PHOTO_UUID);
            String base64photo = jsonObject.getString(PHOTO_FILE);
            byte[] encodedPhoto = base64StringToByteArray(base64photo);
            Bitmap decodedPhoto = BitmapFactory.decodeByteArray(encodedPhoto, 0, encodedPhoto.length);
            savePhoto(wifiDirectManager.getMainMenuActivity(), photoUuid, decodedPhoto);
            return true;
        } catch (IOException ioe) {
            LogManager.logError(GET_PHOTO_FROM_PEER_TAG, ioe.getMessage());
            return false;
        }
    }
}
