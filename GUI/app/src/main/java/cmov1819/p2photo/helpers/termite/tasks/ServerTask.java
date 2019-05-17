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
import java.util.concurrent.ConcurrentHashMap;

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
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_ID;
import static cmov1819.p2photo.helpers.termite.Consts.CHALLENGE;
import static cmov1819.p2photo.helpers.termite.Consts.CONFIRM_RCV;
import static cmov1819.p2photo.helpers.termite.Consts.FAIL;
import static cmov1819.p2photo.helpers.termite.Consts.FROM;
import static cmov1819.p2photo.helpers.termite.Consts.NEED_OPERATION;
import static cmov1819.p2photo.helpers.termite.Consts.NO_HAVE;
import static cmov1819.p2photo.helpers.termite.Consts.NO_OPERATION;
import static cmov1819.p2photo.helpers.termite.Consts.OKAY;
import static cmov1819.p2photo.helpers.termite.Consts.OPERATION;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_FILE;
import static cmov1819.p2photo.helpers.termite.Consts.PHOTO_UUID;
import static cmov1819.p2photo.helpers.termite.Consts.REFUSED;
import static cmov1819.p2photo.helpers.termite.Consts.REQUEST_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.RID;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_CATALOG;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_CHALLENGE;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_PHOTO;
import static cmov1819.p2photo.helpers.termite.Consts.SEND_SESSION;
import static cmov1819.p2photo.helpers.termite.Consts.SESSION_KEY;
import static cmov1819.p2photo.helpers.termite.Consts.TERMITE_PORT;
import static cmov1819.p2photo.helpers.termite.Consts.USERNAME;

public class ServerTask extends AsyncTask<Void, String, Void> {
    private static final String SERVER_TAG = "SERVER SOCKET";

    private WifiDirectManager wfDirectMgr = null;
    private KeyManager mKeyManager = null;
    private final Map<String, String> expectedChallenges = new ConcurrentHashMap<>();
    private final Map<String, SecretKey> uncommitSessionKeys = new ConcurrentHashMap<>();

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
                            default:
                                doRespond(socket,NO_OPERATION);
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
                    socket.close();
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

        String username = message.getString(USERNAME);
        PrivateKey mPrivateKey = mKeyManager.getmPrivateKey();
        PublicKey sendersPublicKey = tryGetKeyFromLocalMaps(username);

        if (sendersPublicKey == null) {
            return REFUSED;
        }

        if (wfDirectMgr.isValidMessage(SEND_SESSION, message, sendersPublicKey)) {
            try {
                logInfo(SERVER_TAG, "Placing session key from " + username + " in un-commit status...");
                byte[] cipheredSessionKey = base64StringToByteArray(message.getString(SESSION_KEY));
                byte[] decipheredSessionKey = decipherWithRSA(cipheredSessionKey, mPrivateKey);
                SecretKey sessionKey = byteArrayToSecretKey(decipheredSessionKey);
                uncommitSessionKeys.put(username, sessionKey);
            }  catch (RSAException rsa) {
                logWarning(SERVER_TAG, "Unable to decipher un-commit session key with this device private key...");
                return REFUSED;
            }

            try {
                logInfo(SERVER_TAG, "Initiating challenge phase to " + username + "...");
                JSONObject jsonChallenge = wfDirectMgr.newBaselineJson(SEND_CHALLENGE);
                String challenge = newUUIDString();
                expectedChallenges.put(username, challenge);
                challenge = byteArrayToBase64String(cipherWithRSA(challenge, sendersPublicKey));
                jsonChallenge.put(CHALLENGE, challenge);
                wfDirectMgr.conformToTLS(
                        jsonChallenge, message.getInt(RID), wfDirectMgr.getDeviceName(), message.getString(FROM)
                );
                return jsonChallenge.toString();
            } catch (RSAException rsa) {
                logWarning(SERVER_TAG, "This device failed to cipher challenge to send to session proposer...");
                return FAIL;
            } catch (SignatureException se) {
                logWarning(SERVER_TAG, "This device failed to sign  challenge message to be sent to session proposer...");
                return FAIL;
            }
        }
        return REFUSED;
    }

    private String processReceivedCatalog(JSONObject message) throws JSONException {
        logInfo(SERVER_TAG, "Processing incoming catalog...");

        String username = message.getString(USERNAME);
        PublicKey sendersPublicKey = tryGetKeyFromLocalMaps(username);

        if (sendersPublicKey == null) {
            logWarning(SERVER_TAG,"Could not obtain senders public key...");
            return REFUSED;
        }

        if (wfDirectMgr.isValidMessage(SEND_CATALOG, message, sendersPublicKey)) {
            JSONObject catalogFile = message.getJSONObject(CATALOG_FILE);
            String catalogId = catalogFile.getString(CATALOG_ID);
            mergeCatalogFiles(wfDirectMgr.getMainMenuActivity(), catalogId, catalogFile);
            return OKAY;
        }

        return REFUSED;
    }

    private String processPhotoRequest(JSONObject message) throws JSONException {

        String username = message.getString(USERNAME);
        PublicKey sendersPublicKey = tryGetKeyFromLocalMaps(username);

        if (sendersPublicKey == null) {
            logWarning(SERVER_TAG,"Could not obtain senders public key...");
            return REFUSED;
        }

        if (wfDirectMgr.isValidMessage(SEND_CATALOG, message, sendersPublicKey)) {
            String device = message.getString(FROM);
            wfDirectMgr.getDeviceUsernameMap().put(device, username);
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

        return REFUSED;
    }

    private String processSendPhotoRequest(MainMenuActivity activity, JSONObject message) throws JSONException {
        try {
            Bitmap photo = ImageLoading.loadPhoto(activity, message.getString(PHOTO_UUID));
            JSONObject jsonObject = wfDirectMgr.newBaselineJson(SEND_PHOTO);
            jsonObject.put(PHOTO_UUID, message.getString(PHOTO_UUID));
            jsonObject.put(PHOTO_FILE, byteArrayToBase64String(bitmapToByteArray(photo)));
            wfDirectMgr.conformToTLS(
                    jsonObject, message.getInt(RID), activity.getDeviceName(), message.getString(FROM)
            );
            return jsonObject.toString();
        } catch (FileNotFoundException fnfe) {
            logWarning(SERVER_TAG, "Photo not found locally...");
            return NO_HAVE;
        } catch (SignatureException se) {
            LogManager.logError(SERVER_TAG, "Unable to sign message, abort reply...");
            return FAIL;
        }
    }

    /** Helpers */

    private PublicKey tryGetKeyFromLocalMaps(String username) {
        PublicKey key = mKeyManager.getPublicKeys().get(username);
        if (key == null) {
            key = getMemberPublicKey(wfDirectMgr.getMainMenuActivity(), username);
            if (key != null) {
                mKeyManager.getPublicKeys().put(username, key);
            }
        }
        return key;
    }

    private void doRespond(SimWifiP2pSocket socket, String string) throws IOException {
        socket.getOutputStream().write((string + CONFIRM_RCV).getBytes());
    }

}