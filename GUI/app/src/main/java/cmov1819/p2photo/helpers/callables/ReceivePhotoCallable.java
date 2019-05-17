package cmov1819.p2photo.helpers.callables;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.concurrent.Callable;

import javax.crypto.SecretKey;

import cmov1819.p2photo.helpers.managers.KeyManager;
import cmov1819.p2photo.helpers.managers.WifiDirectManager;
import cmov1819.p2photo.helpers.termite.tasks.WifiDirectUtils;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToByteArray;
import static cmov1819.p2photo.helpers.CryptoUtils.decipherWithAes;
import static cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.ImageLoading.savePhoto;
import static cmov1819.p2photo.helpers.managers.LogManager.GET_PHOTO_FROM_PEER;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_ID;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_UUID;
import static cmov1819.p2photo.helpers.termite.Consts.REQUEST_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.TERMITE_PORT;
import static cmov1819.p2photo.helpers.termite.Consts.isError;
import static cmov1819.p2photo.helpers.termite.Consts.waitAndTerminate;

public class ReceivePhotoCallable implements Callable<String> {
    private WifiDirectManager wdDirectMgr;
    private SimWifiP2pDevice targetDevice;
    private String photoUuid;
    private String catalogId;
    private SecretKey ourSessionKey;
    private PublicKey targetDevicePublicKey;
    private int rid;

    public ReceivePhotoCallable(SimWifiP2pDevice targetDevice, String photoUuid, String catalogId) {
        this.wdDirectMgr = WifiDirectManager.getInstance();
        this.targetDevice = targetDevice;
        this.photoUuid = photoUuid;
        this.catalogId = catalogId;
        this.ourSessionKey = KeyManager.getInstance().getSessionKeys().get(targetDevice.deviceName);
        this.targetDevicePublicKey = KeyManager.getInstance().getPublicKeys().get(targetDevice.deviceName);
        this.rid = wdDirectMgr.getRequestId();
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
            logInfo(GET_PHOTO_FROM_PEER, String.format("Request photo: %s to %s", photoUuid, targetDevice.deviceName));
            JSONObject jsonObject = wdDirectMgr.newBaselineJson(REQUEST_PHOTO);
            jsonObject.put(CATALOG_ID, catalogId);
            jsonObject.put(PHOTO_UUID, photoUuid);
            wdDirectMgr.conformToTLS(jsonObject, wdDirectMgr.getRequestId(), targetDevice.deviceName);
            return doSend(jsonObject);
        } catch (JSONException jsone) {
            logError(GET_PHOTO_FROM_PEER, "catalogFileContents.toString() failed resulting in exception");
        } catch (SignatureException se) {
            logError(GET_PHOTO_FROM_PEER, "Request Photo unable to conform to TLS. Aborting request...");
        }
        return false;
    }

    private boolean doSend(JSONObject jsonRequest) {
        SimWifiP2pSocket clientSocket = null;
        try {
            // Construct a new clientSocket and send request
            logInfo(GET_PHOTO_FROM_PEER, "Creating client socket to " + targetDevice.deviceName + "...");
            clientSocket = new SimWifiP2pSocket(targetDevice.getVirtIp(), TERMITE_PORT);

            WifiDirectUtils.doSend(GET_PHOTO_FROM_PEER, clientSocket, jsonRequest);
            String encodedResponse = WifiDirectUtils.receiveResponse(GET_PHOTO_FROM_PEER, clientSocket);

            if (isError(encodedResponse)) {
                logWarning(GET_PHOTO_FROM_PEER, encodedResponse);
                return false;
            }

            byte[] decodedResponse = decipherWithAes(ourSessionKey, base64StringToByteArray(encodedResponse));
            JSONObject response = new JSONObject(new String(decodedResponse));
            // Process response
            if (wdDirectMgr.isValidResponse(response, SEND_PHOTO, rid, targetDevicePublicKey)) {
                return trySaveIncomingPhoto(response);
            }
        } catch (IOException ioe) {
            logError(GET_PHOTO_FROM_PEER, "IO Exception occurred while managing client sockets...");
        } catch (JSONException jsone) {
            logError(GET_PHOTO_FROM_PEER, jsone.getMessage());
        }

        if (clientSocket != null) {
            waitAndTerminate(5000, clientSocket);
        }

        return false;
    }

    private boolean trySaveIncomingPhoto(JSONObject jsonObject) throws JSONException {
        logInfo(GET_PHOTO_FROM_PEER, "Processing incoming photo...");
        try {
            String photoUuid = jsonObject.getString(PHOTO_UUID);
            String base64photo = jsonObject.getString(PHOTO_FILE);
            byte[] encodedPhoto = base64StringToByteArray(base64photo);
            Bitmap decodedPhoto = BitmapFactory.decodeByteArray(encodedPhoto, 0, encodedPhoto.length);
            savePhoto(wdDirectMgr.getMainMenuActivity(), photoUuid, decodedPhoto);
            return true;
        } catch (IOException ioe) {
            if (ioe instanceof FileNotFoundException) {
                logWarning(GET_PHOTO_FROM_PEER, "Unable to save photo to this targetDevice's disk...");
            } else {
                logError(GET_PHOTO_FROM_PEER, "Output stream errors occurred while saving photos to disk...");
            }
        }
        return false;
    }
}
