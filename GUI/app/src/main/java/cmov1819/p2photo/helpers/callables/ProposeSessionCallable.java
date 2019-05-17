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
import cmov1819.p2photo.helpers.CryptoUtils;
import cmov1819.p2photo.helpers.managers.KeyManager;
import cmov1819.p2photo.helpers.managers.WifiDirectManager;
import cmov1819.p2photo.helpers.termite.tasks.WifiDirectUtils;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.ConvertUtils.secretKeyToByteArray;
import static cmov1819.p2photo.helpers.CryptoUtils.cipherWithRSA;
import static cmov1819.p2photo.helpers.CryptoUtils.decipherWithRSA;
import static cmov1819.p2photo.helpers.CryptoUtils.generateAesKey;
import static cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.ImageLoading.savePhoto;
import static cmov1819.p2photo.helpers.interfaceimpl.P2PWebServerInterfaceImpl.getMemberPublicKey;
import static cmov1819.p2photo.helpers.managers.LogManager.PROPOSE_SESSION_MGR_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.termite.Consts.CHALLENGE;
import static cmov1819.p2photo.helpers.termite.Consts.FAIL;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_UUID;
import static cmov1819.p2photo.helpers.termite.Consts.REFUSE;
import static cmov1819.p2photo.helpers.termite.Consts.REPLY_TO_CHALLENGE;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_CHALLENGE;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_SESSION;
import static cmov1819.p2photo.helpers.termite.Consts.SESSION_KEY;
import static cmov1819.p2photo.helpers.termite.Consts.SOLUTION;
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
            // TODO Something here
        }
        logWarning(PROPOSE_SESSION_MGR_TAG, "Refusing proposal, challenge response is not well signed or doesn't contain necessary fields!");
        return REFUSE;
    }

    private String propose() {
        unCommitSessionKey = generateAesKey();

        // Try to generate a session key and put it on un-commit map
        if (unCommitSessionKey == null) {
            logError(PROPOSE_SESSION_MGR_TAG,"Failed to generate a session key for user: " + targetDevice.deviceName + ". Aborting...");
            return FAIL;
        } else {
            logInfo(PROPOSE_SESSION_MGR_TAG,"User: " + targetDevice.deviceName + " now has a un-commit session key...");
            mKeyManager.getUnCommitSessionKeys().put(targetDevice.deviceName, unCommitSessionKey);
        }

        // Try to load my neighbor's public key locally or remotely so I can cipher the session key
        targetDevicePublicKey = tryGetKeyFromLocalMaps(targetDevice.deviceName);
        if (targetDevicePublicKey == null) {
            logError(PROPOSE_SESSION_MGR_TAG,"User: " + targetDevice.deviceName + " doesn't have a registered key...");
            return REFUSE;
        }

        try {
            // Send a session key to my neighbor with a session key, my signature, a rid and other TLS data
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
            // Create the client socket to the neighbor and try not to lose it's reference
            logInfo(PROPOSE_SESSION_MGR_TAG, "Creating client socket to " + targetDevice.deviceName + "...");
            clientSocket = new SimWifiP2pSocket(targetDevice.getVirtIp(), TERMITE_PORT);
            // Write and read from the the channel, respectively
            WifiDirectUtils.doSend(PROPOSE_SESSION_MGR_TAG, clientSocket, jsonRequest);
            String response = WifiDirectUtils.receiveResponse(PROPOSE_SESSION_MGR_TAG, clientSocket);
            // Process server response after verifying it's a well formed JSON ChallengeResponse
            JSONObject jsonResponse = isChallengeResponse(response);
            if (jsonResponse == null) {
                return FAIL;
            } else {
                // If everything looks OK answerChallenge and on the process, make my own challenge to the neighbor
                return answerChallenge(clientSocket, jsonResponse);
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
        return FAIL;
    }

    private String answerChallenge(SimWifiP2pSocket clientSocket, JSONObject jsonResponse) {
        try {
            // Decipher the challenge the neighbor sent to me and convert it back to a uuid
            String base64Challenge = jsonResponse.getString(CHALLENGE);
            byte[] cipheredChallenge = base64StringToByteArray(base64Challenge);
            byte[] decipheredChallenge = decipherWithRSA(cipheredChallenge, mKeyManager.getmPrivateKey());
            String challengeSolution = new String(decipheredChallenge);
            // Make a challenge similar to the one he made, cipher it in his public key and send it as usual, with the
            // solution to his challenge
            String myOwnChallenge = CryptoUtils.newUUIDString();
            String myBase64Challenge = byteArrayToBase64String(cipherWithRSA(myOwnChallenge, targetDevicePublicKey));
            mKeyManager.getExpectedChallenges().put(jsonResponse.getString("FROM"), myBase64Challenge);
            JSONObject jsonSolvedChallenge = wfDirectMgr.newBaselineJson(REPLY_TO_CHALLENGE);
            jsonSolvedChallenge.put(SOLUTION, challengeSolution);
            jsonSolvedChallenge.put(CHALLENGE, myBase64Challenge);
            wfDirectMgr.conformToTLS(jsonSolvedChallenge, rid, jsonResponse.getString("FROM"));
            // Using the previous created sockets, write and read from the channel
            WifiDirectUtils.doSend(PROPOSE_SESSION_MGR_TAG, clientSocket, jsonSolvedChallenge);
            String readLine = WifiDirectUtils.receiveResponse(PROPOSE_SESSION_MGR_TAG, clientSocket);
            // Verify if the answer is a READY_TO_COMMIT type and includes the solution to my challenge
            
            // If commit valid, reply with simple "OK", else send "ABORT"
            // EOF ????
        } catch (JSONException e) {
            logError(PROPOSE_SESSION_MGR_TAG, "Couldn't retrieve base64 challenge from challenge response...");
        } catch (RSAException e) {
            logError(PROPOSE_SESSION_MGR_TAG, "Unable to decipher challenge with this private key...");
        } catch (SignatureException e) {
            logError(PROPOSE_SESSION_MGR_TAG, "Unable to sign answer to challenge challenge with this private key...");
        }
        return FAIL;
    }

    private JSONObject isChallengeResponse(String response) {
        if (isError(response)) {
            logWarning(PROPOSE_SESSION_MGR_TAG, response);
            return null;
        }
        try {
            JSONObject challengeResponse = new JSONObject(response);
            if (wfDirectMgr.isValidResponse(challengeResponse, SEND_CHALLENGE, rid, targetDevicePublicKey)) {
                return challengeResponse;
            }
        } catch (JSONException jsone) {
            logError(PROPOSE_SESSION_MGR_TAG, "Failed to rebuild JSON of challenge response!");
        }
        return null;
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
