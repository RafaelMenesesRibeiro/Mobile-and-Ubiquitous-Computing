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
import static cmov1819.p2photo.helpers.CryptoUtils.generateAesKey;
import static cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.ImageLoading.savePhoto;
import static cmov1819.p2photo.helpers.interfaceimpl.P2PWebServerInterfaceImpl.getMemberPublicKey;
import static cmov1819.p2photo.helpers.managers.LogManager.PROPOSE_SESSION_MGR_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.RCV_PHOTO_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.termite.Consts.FAIL;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_UUID;
import static cmov1819.p2photo.helpers.termite.Consts.REFUSED;
import static cmov1819.p2photo.helpers.termite.Consts.SEND;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_SESSION;
import static cmov1819.p2photo.helpers.termite.Consts.TERMITE_PORT;
import static cmov1819.p2photo.helpers.termite.Consts.isError;
import static cmov1819.p2photo.helpers.termite.Consts.stopAndWait;

public class ProposeSessionCallable implements Callable<String> {
    private final WifiDirectManager wfDirectMgr;
    private final KeyManager mKeyManager;
    private final SimWifiP2pDevice targetDevice;
    private final int rid;
    private PublicKey targetPublicKey;
    private SecretKey sessionKey;


    public ProposeSessionCallable(SimWifiP2pDevice targetDevice) {
        this.wfDirectMgr = WifiDirectManager.getInstance();
        this.mKeyManager = KeyManager.getInstance();
        this.targetDevice = targetDevice;
        this.rid = wfDirectMgr.getRequestId();
    }

    @Override
    public String call() {
        logInfo(PROPOSE_SESSION_MGR_TAG, "Initiating a proposal protocol to device: " + targetDevice.deviceName);
        stopAndWait(3000);
        initProposal();
        return null;
    }

    private String initProposal() {
        SecretKey unCommitSessionKey = generateAesKey();
        if (unCommitSessionKey == null) {
            logError(PROPOSE_SESSION_MGR_TAG,"Failed to generate a session key for user: " + targetDevice.deviceName + ". Aborting...");
            return FAIL;
        } else {
            logInfo(PROPOSE_SESSION_MGR_TAG,"User: " + targetDevice.deviceName + " now has a un-commit session key...");
            mKeyManager.getUncommitSessionKeys().put(targetDevice.deviceName, unCommitSessionKey);
        }
        PublicKey proposalCode = mKeyManager.getPublicKeys().get(targetDevice.deviceName);
        mKeyManager.getSessionKeys().put(targetDevice.deviceName, unCommitSessionKey);

        return REFUSED;
    }

    private String propose() {
        try {
            logInfo(RCV_PHOTO_TAG, "Proposing session key to user: " + targetDevice.deviceName);
            JSONObject jsonObject = wfDirectMgr.newBaselineJson(SEND_SESSION);
            // TODO
            wfDirectMgr.conformToTLS(jsonObject, wfDirectMgr.getRequestId(), targetDevice.deviceName);
            // return doSend(jsonObject);
        } catch (JSONException jsone) {
            logError(RCV_PHOTO_TAG, "Propose has failed while building a json message");
        } catch (SignatureException se) {
            logError(RCV_PHOTO_TAG, "ProposeSessionKey was unable to conform to TLS. Aborting request...");
        }
        return REFUSED;
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
        if (!wfDirectMgr.isValidMessage(wfDirectMgr.getDeviceName(), rid, response)) {
            return false;
        }
        if (!wfDirectMgr.isValidMessage(SEND_PHOTO, response, targetPublicKey)) {
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
            savePhoto(wfDirectMgr.getMainMenuActivity(), photoUuid, decodedPhoto);
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
