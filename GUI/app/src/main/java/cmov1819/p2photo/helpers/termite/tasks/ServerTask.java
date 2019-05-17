package cmov1819.p2photo.helpers.termite.tasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Map;

import javax.crypto.SecretKey;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.exceptions.RSAException;
import cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.ImageLoading;
import cmov1819.p2photo.helpers.managers.KeyManager;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.WifiDirectManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.bitmapToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToSecretKey;
import static cmov1819.p2photo.helpers.CryptoUtils.cipherWithAes;
import static cmov1819.p2photo.helpers.CryptoUtils.cipherWithRSA;
import static cmov1819.p2photo.helpers.CryptoUtils.decipherWithRSA;
import static cmov1819.p2photo.helpers.CryptoUtils.newUUIDString;
import static cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.CatalogMerge.mergeCatalogFiles;
import static cmov1819.p2photo.helpers.interfaceimpl.P2PWebServerInterfaceImpl.assertMembership;
import static cmov1819.p2photo.helpers.interfaceimpl.P2PWebServerInterfaceImpl.getMemberPublicKey;
import static cmov1819.p2photo.helpers.managers.LogManager.SERVER_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.termite.Consts.ABORT_COMMIT;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_ID;
import static cmov1819.p2photo.helpers.termite.Consts.CHALLENGE;
import static cmov1819.p2photo.helpers.termite.Consts.CONFIRM_COMMIT;
import static cmov1819.p2photo.helpers.termite.Consts.CONFIRM_RCV;
import static cmov1819.p2photo.helpers.termite.Consts.FAIL;
import static cmov1819.p2photo.helpers.termite.Consts.FROM;
import static cmov1819.p2photo.helpers.termite.Consts.GO_LEAVE_GROUP;
import static cmov1819.p2photo.helpers.termite.Consts.LEAVE_GROUP;
import static cmov1819.p2photo.helpers.termite.Consts.NEED_OPERATION;
import static cmov1819.p2photo.helpers.termite.Consts.NO_HAVE;
import static cmov1819.p2photo.helpers.termite.Consts.NO_OPERATION;
import static cmov1819.p2photo.helpers.termite.Consts.OKAY;
import static cmov1819.p2photo.helpers.termite.Consts.OPERATION;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_UUID;
import static cmov1819.p2photo.helpers.termite.Consts.READY_TO_COMMIT;
import static cmov1819.p2photo.helpers.termite.Consts.REFUSE;
import static cmov1819.p2photo.helpers.termite.Consts.REPLY_TO_CHALLENGE;
import static cmov1819.p2photo.helpers.termite.Consts.REQUEST_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.RID;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_CATALOG;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_CHALLENGE;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_SESSION;
import static cmov1819.p2photo.helpers.termite.Consts.SESSION_KEY;
import static cmov1819.p2photo.helpers.termite.Consts.SOLUTION;
import static cmov1819.p2photo.helpers.termite.Consts.TERMITE_PORT;
import static cmov1819.p2photo.helpers.termite.Consts.waitAndTerminate;

public class ServerTask extends AsyncTask<Void, String, Void> {

    private WifiDirectManager wfDirectMgr = null;
    private KeyManager mKeyManager = null;

    @Override
    protected void onPreExecute() {
        logInfo(SERVER_TAG, "Started WiFi Direct server task (" + this.hashCode() + ").");
    }

    @Override
    protected Void doInBackground(final Void... params) {
        try {
            // get singleton instance of our WifiDirectManager
            wfDirectMgr = WifiDirectManager.getInstance();
            mKeyManager = KeyManager.getInstance();
            // setup a server socket on MainMenuActivity
            wfDirectMgr.setServerSocket(new SimWifiP2pSocketServer(TERMITE_PORT));

            // set server socket to listen to incoming requests
            while (!Thread.currentThread().isInterrupted()) {
                SimWifiP2pSocket socket = wfDirectMgr.getServerSocket().accept();
                try {
                    // Read from input stream
                    InputStream inputStream = socket.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String data = bufferedReader.readLine();
                    // Process values
                    JSONObject request = new JSONObject(data);
                    if (request.has(OPERATION)) {
                        switch (request.getString(OPERATION)) {
                            case SEND_CATALOG:
                                doRespond(socket, processReceivedCatalog(request));
                                break;
                            case REQUEST_PHOTO:
                                doRespond(socket, processPhotoRequest(request));
                                break;
                            case SEND_SESSION:
                                doRespond(socket, processSessionProposal(request));
                                break;
                            case REPLY_TO_CHALLENGE:
                                doRespond(socket, processChallengeReply(request));
                                break;
                            case ABORT_COMMIT:
                                doRespond(socket, processAbortCommit(request));
                                break;
                            case CONFIRM_COMMIT:
                                doRespond(socket, processDoCommit(request));
                                break;
                            case LEAVE_GROUP:
                                doRespond(socket, processSubjectLeaving(request));
                                break;
                            case GO_LEAVE_GROUP:
                                doRespond(socket, processLeaderLeaving(request));
                                break;
                            default:
                                doRespond(socket, NO_OPERATION);
                                break;
                        }
                    } else {
                        doRespond(socket, NEED_OPERATION);
                    }
                } catch (IOException ioe) {
                    Log.e(SERVER_TAG, "Error reading socket: " + ioe.getMessage());
                } catch (JSONException jsone) {
                    Log.e(SERVER_TAG, "Error reading socket: malformed json request");
                } finally {
                    waitAndTerminate(1, socket);
                }
            }
        } catch (IOException ioe) {
            Log.e(SERVER_TAG, "Socket error: " + ioe.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        logInfo(SERVER_TAG, "WiFi Direct server task shutdown (" + this.hashCode() + ").");
    }

    private String processSessionProposal(JSONObject message) throws JSONException {
        logInfo(SERVER_TAG, "Processing session proposal, purpose phase...");

        String username = message.getString(FROM);
        PrivateKey mPrivateKey = mKeyManager.getmPrivateKey();
        PublicKey sendersPublicKey = tryGetKeyFromLocalMaps(username);

        if (sendersPublicKey == null) {
            return REFUSE;
        }

        if (wfDirectMgr.isValidMessage(SEND_SESSION, message, sendersPublicKey)) {
            try {
                logInfo(SERVER_TAG, "Placing session key from " + username + " in un-commit status...");
                byte[] cipheredSessionKey = base64StringToByteArray(message.getString(SESSION_KEY));
                byte[] decipheredSessionKey = decipherWithRSA(cipheredSessionKey, mPrivateKey);
                SecretKey sessionKey = byteArrayToSecretKey(decipheredSessionKey);
                mKeyManager.getUnCommitSessionKeys().put(username, sessionKey);
            }  catch (RSAException rsa) {
                logWarning(SERVER_TAG, "Unable to decipher un-commit session key with this device private key...");
                return REFUSE;
            }

            try {
                logInfo(SERVER_TAG, "Initiating challenge phase to " + username + "...");
                JSONObject jsonChallenge = wfDirectMgr.newBaselineJson(SEND_CHALLENGE);
                String challenge = newUUIDString();
                mKeyManager.getExpectedChallenges().put(username, challenge);
                challenge = byteArrayToBase64String(cipherWithRSA(challenge, sendersPublicKey));
                jsonChallenge.put(CHALLENGE, challenge);
                wfDirectMgr.conformToTLS(jsonChallenge, message.getInt(RID), message.getString(FROM));
                return jsonChallenge.toString();
            } catch (RSAException rsa) {
                logWarning(SERVER_TAG, "This device failed to cipher challenge to send to session proposer...");
                return FAIL;
            } catch (SignatureException se) {
                logWarning(SERVER_TAG, "This device failed to sign  challenge message to be sent to session proposer...");
                return FAIL;
            }
        }
        return REFUSE;
    }

    private String processChallengeReply(JSONObject challengeReply) {
        try {
            // Retrieve expected challenge for this user
            Map<String, String> expectedChallenges = mKeyManager.getExpectedChallenges();
            String username = challengeReply.getString(FROM);
            String correctSolution = expectedChallenges.get(username);
            String givenSolution = challengeReply.getString(SOLUTION);
            // Assert existence and correctness of given solution if no JSON exception occurs
            if (correctSolution == null) {
                logWarning(SERVER_TAG,"processChallengeReply didn't expect this user to send a challenge.");
                return REFUSE;
            } else if (correctSolution.equals(givenSolution)) {
                // If solution is correct I also need to reply to the user who answered my challenge
                String base64challenge = challengeReply.getString(CHALLENGE);
                byte[] cipheredChallenge = base64StringToByteArray(base64challenge);
                byte[] decipheredChallenge = decipherWithRSA(cipheredChallenge, mKeyManager.getmPrivateKey());
                String solution = new String(decipheredChallenge);
                // Everything is ready on my end to commit, so I move the session key to Ready to Commit map
                SecretKey keyToCommit = mKeyManager.getUnCommitSessionKeys().get(username);
                if (keyToCommit == null) {
                    logError(SERVER_TAG,"processChallengeReply was supposed to have a un-commit key but didn't...");
                    return FAIL;
                }
                mKeyManager.getReadyToCommitSessionKeys().put(username, keyToCommit);
                // I create a commit response and wait for an OK or ABORT_COMMIT message
                JSONObject commitResponse = wfDirectMgr.newBaselineJson(READY_TO_COMMIT);
                commitResponse.put(SOLUTION, solution);
                wfDirectMgr.conformToTLS(commitResponse, challengeReply.getInt(RID), username);
                return commitResponse.toString();
            }
        } catch (JSONException exc) {
            logError(SERVER_TAG,"processChallengeReply couldn't obtained required JSON fields on processChallengeReply.");
            return REFUSE;
        } catch (RSAException exc) {
            logError(SERVER_TAG,"processChallengeReply couldn't decipher sent challenge using this device private key.");
            return REFUSE;
        } catch (SignatureException exc) {
            logError(SERVER_TAG,"processChallengeReply couldn't sign the ready to commit reply...");
            return FAIL;
        }

        logWarning(SERVER_TAG,"processChallengeReply asserts that given solution is wrong...");
        return FAIL;
    }

    private String processAbortCommit(JSONObject request) throws JSONException {
        if (request.getString(OPERATION).equals(ABORT_COMMIT)) {
            String username = request.getString(FROM);
            SecretKey sessionKey = mKeyManager.getReadyToCommitSessionKeys().remove(username);
        }
        return "";
    }

    private String processDoCommit(JSONObject request) throws JSONException {
        if (request.getString(OPERATION).equals(CONFIRM_COMMIT)) {
            String username = request.getString(FROM);
            SecretKey sessionKey = mKeyManager.getReadyToCommitSessionKeys().remove(username);
            if (sessionKey == null) {
                logWarning(SERVER_TAG, "Expected to have a key to commit from user " + username);
            } else {
                mKeyManager.getSessionKeys().put(username, sessionKey);
                logInfo(SERVER_TAG, "Successfully traded a session key " + username);
            }
        }
        return "";
    }

    private String processReceivedCatalog(JSONObject message) throws JSONException {
        logInfo(SERVER_TAG, "Processing incoming catalog...");

        String username = message.getString(FROM);
        PublicKey sendersPublicKey = tryGetKeyFromLocalMaps(username);

        if (sendersPublicKey == null) {
            logWarning(SERVER_TAG,"Could not obtain senders public key...");
            return REFUSE;
        }

        if (wfDirectMgr.isValidMessage(SEND_CATALOG, message, sendersPublicKey)) {
            JSONObject catalogFile = message.getJSONObject(CATALOG_FILE);
            String catalogId = catalogFile.getString(CATALOG_ID);
            mergeCatalogFiles(wfDirectMgr.getMainMenuActivity(), catalogId, catalogFile);
            return OKAY;
        }

        return REFUSE;
    }

    private String processPhotoRequest(JSONObject message) throws JSONException {
        String username = message.getString(FROM);
        PublicKey sendersPublicKey = tryGetKeyFromLocalMaps(username);

        if (sendersPublicKey == null) {
            logWarning(SERVER_TAG,"Could not obtain senders public key...");
            return REFUSE;
        }

        if (wfDirectMgr.isValidMessage(SEND_CATALOG, message, sendersPublicKey)) {
            String catalogId = message.getString(CATALOG_ID);
            MainMenuActivity mMainMenuActivity = wfDirectMgr.getMainMenuActivity();
            if (assertMembership(mMainMenuActivity, username, catalogId)) {
                String clearText = processSendPhotoRequest(mMainMenuActivity, message);
                if (!clearText.equals("")) {
                    SecretKey sessionKey = mKeyManager.getSessionKeys().get(username);
                    return byteArrayToBase64String(cipherWithAes(sessionKey, clearText.getBytes()));
                }
            }
        }

        return REFUSE;
    }

    private String processSendPhotoRequest(MainMenuActivity activity, JSONObject message) throws JSONException {
        try {
            Bitmap photo = ImageLoading.loadPhoto(activity, message.getString(PHOTO_UUID));
            JSONObject jsonObject = wfDirectMgr.newBaselineJson(SEND_PHOTO);
            jsonObject.put(PHOTO_UUID, message.getString(PHOTO_UUID));
            jsonObject.put(PHOTO_FILE, byteArrayToBase64String(bitmapToByteArray(photo)));
            wfDirectMgr.conformToTLS(jsonObject, message.getInt(RID), message.getString(FROM));
            return jsonObject.toString();
        } catch (FileNotFoundException fnfe) {
            logWarning(SERVER_TAG, "Photo not found locally...");
            return NO_HAVE;
        } catch (SignatureException se) {
            LogManager.logError(SERVER_TAG, "Unable to sign message, abort reply...");
            return FAIL;
        }
    }

    private String processSubjectLeaving(JSONObject message) throws JSONException {
        String username = message.getString(FROM);
        PublicKey sendersPublicKey = tryGetKeyFromLocalMaps(username);

        if (sendersPublicKey == null) {
            logWarning(SERVER_TAG,"Could not obtain senders public key...");
            return REFUSE;
        }
        if (wfDirectMgr.isValidMessage(LEAVE_GROUP, message, sendersPublicKey)) {
            mKeyManager.getSessionKeys().remove(username);
            return OKAY;
        }
        return REFUSE;
    }

    private String processLeaderLeaving(JSONObject message) throws JSONException {
        String username = message.getString(FROM);
        PublicKey sendersPublicKey = tryGetKeyFromLocalMaps(username);

        if (sendersPublicKey == null) {
            logWarning(SERVER_TAG,"Could not obtain senders public key...");
            return REFUSE;
        }
        if (wfDirectMgr.isValidMessage(GO_LEAVE_GROUP, message, sendersPublicKey)) {
            mKeyManager.getSessionKeys().remove(username);

            // TODO - Group needs a leader now. //

            return OKAY;
        }
        return REFUSE;
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

    private void doRespond(SimWifiP2pSocket socket, String string) throws IOException {
        socket.getOutputStream().write((string + CONFIRM_RCV).getBytes());
    }

}