package cmov1819.p2photo.helpers.callables;

import org.json.JSONException;
import org.json.JSONObject;

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
import static cmov1819.p2photo.helpers.interfaceimpl.P2PWebServerInterfaceImpl.getMemberPublicKey;
import static cmov1819.p2photo.helpers.managers.LogManager.PROPOSE_SESSION_MGR_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.termite.Consts.ABORT_COMMIT;
import static cmov1819.p2photo.helpers.termite.Consts.CHALLENGE;
import static cmov1819.p2photo.helpers.termite.Consts.CONFIRM_COMMIT;
import static cmov1819.p2photo.helpers.termite.Consts.READY_TO_COMMIT;
import static cmov1819.p2photo.helpers.termite.Consts.REPLY_TO_CHALLENGE;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_CHALLENGE;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_SESSION;
import static cmov1819.p2photo.helpers.termite.Consts.SESSION_KEY;
import static cmov1819.p2photo.helpers.termite.Consts.SOLUTION;
import static cmov1819.p2photo.helpers.termite.Consts.TERMITE_PORT;
import static cmov1819.p2photo.helpers.termite.Consts.isError;
import static cmov1819.p2photo.helpers.termite.Consts.waitAndTerminate;

public class ProposeSessionCallable implements Callable<SimWifiP2pDevice> {
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

    public SimWifiP2pDevice getTargetDevice() {
        return targetDevice;
    }

    @Override
    public SimWifiP2pDevice call() {
        logInfo(PROPOSE_SESSION_MGR_TAG, "Initiating a proposal protocol to device: " + targetDevice.deviceName);
        if (proposalProtocol()) {
            // if protocol result ends well return null
            return null;
        } else {
            // else return the device for reattempt
            return targetDevice;
        }
    }

    private boolean proposalProtocol() {
        unCommitSessionKey = generateAesKey();

        // Try to generate a session key and put it on un-commit map
        if (unCommitSessionKey == null) {
            logError(PROPOSE_SESSION_MGR_TAG,"Failed to generate a session key for user: " + targetDevice.deviceName + ". Aborting...");
            return false;
        } else {
            logInfo(PROPOSE_SESSION_MGR_TAG,"User: " + targetDevice.deviceName + " now has a un-commit session key...");
            mKeyManager.getUnCommitSessionKeys().put(targetDevice.deviceName, unCommitSessionKey);
        }

        // Try to load my neighbor's public key locally or remotely so I can cipher the session key
        targetDevicePublicKey = tryGetKeyFromLocalMaps(targetDevice.deviceName);
        if (targetDevicePublicKey == null) {
            logError(PROPOSE_SESSION_MGR_TAG,"User: " + targetDevice.deviceName + " doesn't have a registered key...");
            return false;
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
            return false;
        } catch (JSONException jsone) {
            logError(PROPOSE_SESSION_MGR_TAG, "This device could not build a proposal message due to JSON Exception!");
            return false;
        } catch (SignatureException jsone) {
            logError(PROPOSE_SESSION_MGR_TAG, "This device could not sign the proposal message! Aborting...");
            return false;
        }
    }

    private boolean doSend(JSONObject jsonRequest) {
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
            if (jsonResponse != null) {
                // If everything looks OK answerChallenge and on the process, make my own challenge to the neighbor
                return answerChallenge(clientSocket, jsonResponse);
            }
        } catch (IOException ioe) {
            logError(PROPOSE_SESSION_MGR_TAG, "IO Exception occurred while managing client sockets...");
        } finally {
            waitAndTerminate(5000, clientSocket);
        }
        return false;
    }

    private boolean answerChallenge(SimWifiP2pSocket clientSocket, JSONObject jsonResponse) {
        try {
            String username = jsonResponse.getString("FROM");
            // Decipher the challenge the neighbor sent to me and convert it back to a uuid
            String base64Challenge = jsonResponse.getString(CHALLENGE);
            byte[] cipheredChallenge = base64StringToByteArray(base64Challenge);
            byte[] decipheredChallenge = decipherWithRSA(cipheredChallenge, mKeyManager.getmPrivateKey());
            String challengeSolution = new String(decipheredChallenge);
            // Make a challenge similar to the one he made, cipher it in his public key and send it as usual, with the

            // solution to his challenge
            String myOwnChallenge = CryptoUtils.newUUIDString();
            String myBase64Challenge = byteArrayToBase64String(cipherWithRSA(myOwnChallenge, targetDevicePublicKey));
            mKeyManager.getExpectedChallenges().put(username, myBase64Challenge);
            JSONObject jsonSolvedChallenge = wfDirectMgr.newBaselineJson(REPLY_TO_CHALLENGE);
            jsonSolvedChallenge.put(SOLUTION, challengeSolution);
            jsonSolvedChallenge.put(CHALLENGE, myBase64Challenge);
            wfDirectMgr.conformToTLS(jsonSolvedChallenge, rid, username);

            // Using the previous created sockets, write and read from the channel
            WifiDirectUtils.doSend(PROPOSE_SESSION_MGR_TAG, clientSocket, jsonSolvedChallenge);
            String readLine = WifiDirectUtils.receiveResponse(PROPOSE_SESSION_MGR_TAG, clientSocket);
            // Verify if the answer is a READY_TO_COMMIT type and includes the solution to my challenge
            if (isError(readLine)) {
                WifiDirectUtils.doSend(PROPOSE_SESSION_MGR_TAG, clientSocket, wfDirectMgr.newBaselineJson(ABORT_COMMIT));
                logWarning(PROPOSE_SESSION_MGR_TAG, targetDevice + " sent an error response...");
                return false;
            }

            JSONObject readyToCommitResponse = new JSONObject(readLine);
            if (!wfDirectMgr.isValidResponse(readyToCommitResponse, READY_TO_COMMIT, rid, targetDevicePublicKey)) {
                WifiDirectUtils.doSend(PROPOSE_SESSION_MGR_TAG, clientSocket, wfDirectMgr.newBaselineJson(ABORT_COMMIT));
                logWarning(PROPOSE_SESSION_MGR_TAG, targetDevice + " sent an invalid message...");
                return false;
            }

            if (!readyToCommitResponse.getString(READY_TO_COMMIT).equals(READY_TO_COMMIT)) {
                WifiDirectUtils.doSend(PROPOSE_SESSION_MGR_TAG, clientSocket, wfDirectMgr.newBaselineJson(ABORT_COMMIT));
                logWarning(PROPOSE_SESSION_MGR_TAG, targetDevice + " doesn't want to commit...");
                return false;
            }
            // Commit and tell the other guy to commit
            mKeyManager.getSessionKeys().put(username, unCommitSessionKey);
            WifiDirectUtils.doSend(PROPOSE_SESSION_MGR_TAG, clientSocket, wfDirectMgr.newBaselineJson(CONFIRM_COMMIT));
            logInfo(PROPOSE_SESSION_MGR_TAG, targetDevice + " session key established!");
            return true;

        } catch (JSONException e) {
            logError(PROPOSE_SESSION_MGR_TAG, "Couldn't retrieve base64 challenge from challenge response...");
        } catch (RSAException e) {
            logError(PROPOSE_SESSION_MGR_TAG, "Unable to decipher challenge with this private key...");
        } catch (SignatureException e) {
            logError(PROPOSE_SESSION_MGR_TAG, "Unable to sign answer to challenge challenge with this private key...");
        }
        return false;
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
