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

import cmov1819.p2photo.exceptions.RSAException;
import cmov1819.p2photo.helpers.managers.KeyManager;
import cmov1819.p2photo.helpers.managers.WifiDirectManager;
import cmov1819.p2photo.helpers.termite.tasks.WifiDirectUtils;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.ConvertUtils.secretKeyToByteArray;
import static cmov1819.p2photo.helpers.CryptoUtils.cipherWithRSA;
import static cmov1819.p2photo.helpers.CryptoUtils.generateAesKey;
import static cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.ImageLoading.savePhoto;
import static cmov1819.p2photo.helpers.interfaceimpl.P2PWebServerInterfaceImpl.getMemberPublicKey;
import static cmov1819.p2photo.helpers.managers.LogManager.PROPOSE_SESSION_MGR_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.termite.Consts.FAIL;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_UUID;
import static cmov1819.p2photo.helpers.termite.Consts.REFUSED;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_CHALLENGE;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_SESSION;
import static cmov1819.p2photo.helpers.termite.Consts.SESSION_KEY;
import static cmov1819.p2photo.helpers.termite.Consts.TERMITE_PORT;
import static cmov1819.p2photo.helpers.termite.Consts.isError;

public class ProposeSessionCallable implements Callable<String> {
    private final WifiDirectManager wfDirectMgr;
    private final KeyManager mKeyManager;
    private final SimWifiP2pDevice targetDevice;
    private final int rid;
    private PublicKey targetDevicePublicKey;
    private SecretKey unCommitSessionKey;


    public ProposeSessionCallable(SimWifiP2pDevice targetDevice) {
        this.wfDirectMgr = WifiDirectManager.getInstance();
        this.mKeyManager = KeyManager.getInstance();
        this.targetDevice = targetDevice;
        this.rid = wfDirectMgr.getRequestId();
    }

    @Override
    public String call() {
        logInfo(PROPOSE_SESSION_MGR_TAG, "Initiating a proposal protocol to device: " + targetDevice.deviceName);
        return proposalProtocol();
    }

    private String proposalProtocol() {
        String readLine = propose();
        if (!isError(readLine)) {
            try {
                JSONObject challengeResponse = new JSONObject(readLine);
                wfDirectMgr.isValidResponse(challengeResponse, SEND_CHALLENGE, rid, targetDevicePublicKey);
            } catch (JSONException jsone) {
                logError(PROPOSE_SESSION_MGR_TAG, "Failed to rebuild JSON of challenge response!");
                return FAIL;
            }

        }
        return readLine;
    }

    private String propose() {
        unCommitSessionKey = generateAesKey();
        if (unCommitSessionKey == null) {
            logError(PROPOSE_SESSION_MGR_TAG,"Failed to generate a session key for user: " + targetDevice.deviceName + ". Aborting...");
            return FAIL;
        } else {
            logInfo(PROPOSE_SESSION_MGR_TAG,"User: " + targetDevice.deviceName + " now has a un-commit session key...");
            mKeyManager.getUncommitSessionKeys().put(targetDevice.deviceName, unCommitSessionKey);
            // TODO Put in un-commit status OK?
        }

        targetDevicePublicKey = tryGetKeyFromLocalMaps(targetDevice.deviceName);
        if (targetDevicePublicKey == null) {
            logError(PROPOSE_SESSION_MGR_TAG,"User: " + targetDevice.deviceName + " doesn't have a registered key...");
            return REFUSED;
        }

        try {
            byte[] encodedSessionKey = secretKeyToByteArray(unCommitSessionKey);
            byte[] cipheredSessionKey = cipherWithRSA(encodedSessionKey, targetDevicePublicKey);
            String base64SessionKey = byteArrayToBase64String(cipheredSessionKey);
            JSONObject requestData = wfDirectMgr.newBaselineJson(SEND_SESSION);
            requestData.put(SESSION_KEY, base64SessionKey);
            wfDirectMgr.conformToTLS(requestData, rid, targetDevice.deviceName);
            return doSend(requestData);
        } catch (RSAException e) {
            logError(PROPOSE_SESSION_MGR_TAG, "This device could not cipher un-commit session key with RSA...");
            return FAIL;
        } catch (JSONException jsone) {
            logError(PROPOSE_SESSION_MGR_TAG, "This device could not build a proposal message due to JSON Exception!");
            return FAIL;
        } catch (SignatureException jsone) {
            logError(PROPOSE_SESSION_MGR_TAG, "This device could not sign the proposal message! Aborting...");
            return FAIL;
        }
    }

    private String doSend(JSONObject jsonRequest) {
        SimWifiP2pSocket clientSocket = null;
        try {
            logInfo(PROPOSE_SESSION_MGR_TAG, "Creating client socket to " + targetDevice.deviceName + "...");
            clientSocket = new SimWifiP2pSocket(targetDevice.getVirtIp(), TERMITE_PORT);

            WifiDirectUtils.doSend(PROPOSE_SESSION_MGR_TAG, clientSocket, jsonRequest);
            String encodedResponse = WifiDirectUtils.receiveResponse(PROPOSE_SESSION_MGR_TAG, clientSocket);

            if (isError(encodedResponse)) {
                logWarning(PROPOSE_SESSION_MGR_TAG, encodedResponse);
                return FAIL;
            }

        } catch (IOException ioe) {
            logError(PROPOSE_SESSION_MGR_TAG, "IO Exception occurred while managing client sockets...");
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
            } catch (IOException ioe) {
                logError(PROPOSE_SESSION_MGR_TAG, ioe.getMessage());
            }
        }
        return REFUSED;
    }

    private boolean trySaveIncomingPhoto(JSONObject jsonObject) throws JSONException {
        logInfo(PROPOSE_SESSION_MGR_TAG, "Processing incoming photo...");
        try {
            String photoUuid = jsonObject.getString(PHOTO_UUID);
            String base64photo = jsonObject.getString(PHOTO_FILE);
            byte[] encodedPhoto = base64StringToByteArray(base64photo);
            Bitmap decodedPhoto = BitmapFactory.decodeByteArray(encodedPhoto, 0, encodedPhoto.length);
            savePhoto(wfDirectMgr.getMainMenuActivity(), photoUuid, decodedPhoto);
            return true;
        } catch (IOException ioe) {
            if (ioe instanceof FileNotFoundException) {
                logWarning(PROPOSE_SESSION_MGR_TAG, "Unable to save photo to this targetDevice's disk...");
            } else {
                logError(PROPOSE_SESSION_MGR_TAG, "Output stream errors occurred while saving photos to disk...");
            }
        }
        return false;
    }

    /** Helpers */

    private PublicKey tryGetKeyFromLocalMaps(String targetDeviceName) {
        PublicKey key = mKeyManager.getPublicKeys().get(targetDeviceName);
        if (key == null) {
            key = getMemberPublicKey(wfDirectMgr.getMainMenuActivity(), targetDeviceName);
            if (key != null) {
                mKeyManager.getPublicKeys().put(targetDeviceName, key);
            }
        }
        return key;
    }
}
