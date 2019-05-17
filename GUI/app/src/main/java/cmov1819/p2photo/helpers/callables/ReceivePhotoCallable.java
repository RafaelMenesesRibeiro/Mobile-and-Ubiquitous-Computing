package cmov1819.p2photo.helpers.callables;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.concurrent.Callable;

import javax.crypto.SecretKey;

import cmov1819.p2photo.helpers.managers.KeyManager;
import cmov1819.p2photo.helpers.managers.WifiDirectManager;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToByteArray;
import static cmov1819.p2photo.helpers.CryptoUtils.decipherWithAes;
import static cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.ImageLoading.savePhoto;
import static cmov1819.p2photo.helpers.managers.LogManager.RCV_PHOTO_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_ID;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_UUID;
import static cmov1819.p2photo.helpers.termite.Consts.REQUEST_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.SEND;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.TERMITE_PORT;
import static cmov1819.p2photo.helpers.termite.Consts.isError;

public class ReceivePhotoCallable implements Callable<String> {
    private WifiDirectManager wifiDirectManager;
    private SimWifiP2pDevice targetDevice;
    private String photoUuid;
    private String catalogId;
    private SecretKey sessionKey;
    private PublicKey publicKey;
    private int rid;

    public ReceivePhotoCallable(SimWifiP2pDevice targetDevice, String photoUuid, String catalogId) {

        this.wifiDirectManager = WifiDirectManager.getInstance();
        this.targetDevice = targetDevice;
        this.photoUuid = photoUuid;
        this.catalogId = catalogId;
        this.sessionKey = KeyManager.getInstance().getSessionKeys().get(targetDevice.deviceName);
        this.publicKey = KeyManager.getInstance().getPublicKeys().get(targetDevice.deviceName);
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
            logInfo(RCV_PHOTO_TAG, String.format("Request photo: %s to %s", photoUuid, targetDevice.deviceName));
            JSONObject jsonObject = wifiDirectManager.newBaselineJson(REQUEST_PHOTO);
            jsonObject.put(CATALOG_ID, catalogId);
            jsonObject.put(PHOTO_UUID, photoUuid);
            wifiDirectManager.conformToTLS(jsonObject, wifiDirectManager.getRequestId(), targetDevice.deviceName);
            return doSend(jsonObject);
        } catch (JSONException jsone) {
            logError(RCV_PHOTO_TAG, "catalogFileContents.toString() failed resulting in exception");
        } catch (SignatureException se) {
            logError(RCV_PHOTO_TAG, "Request Photo unable to conform to TLS. Aborting request...");
        }
        return false;
    }

    private boolean doSend(JSONObject jsonData) {
        SimWifiP2pSocket clientSocket = null;
        try {
            // Construct a new clientSocket and send request
            logInfo(RCV_PHOTO_TAG, "Creating client socket to " + targetDevice.deviceName + "...");
            clientSocket = new SimWifiP2pSocket(targetDevice.getVirtIp(), TERMITE_PORT);
            clientSocket.getOutputStream().write((jsonData.toString() + SEND).getBytes());
            InputStream inputStream = clientSocket.getInputStream();
            // Read response
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String encodedResponse = bufferedReader.readLine();

            if (isError(encodedResponse)) {
                logWarning(RCV_PHOTO_TAG, encodedResponse);
                return false;
            }

            byte[] decodedResponse = decipherWithAes(sessionKey, base64StringToByteArray(encodedResponse));
            JSONObject response = new JSONObject(new String(decodedResponse));
            // Process response
            if (isValidResponse(response)) {
                return trySaveIncomingPhoto(response);
            }
        } catch (IOException ioe) {
            logError(RCV_PHOTO_TAG, "IO Exception occurred while managing client sockets...");
        } catch (JSONException jsone) {
            logError(RCV_PHOTO_TAG, jsone.getMessage());
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
            } catch (IOException ioe) {
                logError(RCV_PHOTO_TAG, ioe.getMessage());
            }
        }
        return false;
    }

    public boolean isValidResponse(JSONObject response) {
        if (!wifiDirectManager.isValidMessage(targetDevice.deviceName, rid, response)) {
            return false;
        }
        if (!wifiDirectManager.isValidMessage(SEND_PHOTO, response, publicKey)) {
            return false;
        }
        return true;
    }

    private boolean trySaveIncomingPhoto(JSONObject jsonObject) throws JSONException {
        logInfo(RCV_PHOTO_TAG, "Processing incoming photo...");
        try {
            String photoUuid = jsonObject.getString(PHOTO_UUID);
            String base64photo = jsonObject.getString(PHOTO_FILE);
            byte[] encodedPhoto = base64StringToByteArray(base64photo);
            Bitmap decodedPhoto = BitmapFactory.decodeByteArray(encodedPhoto, 0, encodedPhoto.length);
            savePhoto(wifiDirectManager.getMainMenuActivity(), photoUuid, decodedPhoto);
            return true;
        } catch (IOException ioe) {
            if (ioe instanceof FileNotFoundException) {
                logWarning(RCV_PHOTO_TAG, "Unable to save photo to this targetDevice's disk...");
            } else {
                logError(RCV_PHOTO_TAG, "Output stream errors occurred while saving photos to disk...");
            }
        }
        return false;
    }
}
