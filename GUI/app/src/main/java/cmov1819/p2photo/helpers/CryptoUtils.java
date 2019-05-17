package cmov1819.p2photo.helpers;

import android.app.Activity;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import cmov1819.p2photo.R;
import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.exceptions.RSAException;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.mediators.P2PWebServerMediator;
import cmov1819.p2photo.msgtypes.ErrorResponse;

import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToPrivateKey;
import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToPublicKey;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToByteArray;
import static cmov1819.p2photo.helpers.managers.LogManager.CRYPTO_UTILS_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

public class CryptoUtils {
    private static final String PRIVATE_KEY_FILENAME = "P2Photo_PrivateKey.key";
    private static final String PUBLIC_KEY_FILENAME = "P2Photo_PublicKey.pub";
    private static final String SECRECT_KEY_FILENAME = "P2PHOTO_SecrectKey";
    public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    public static final String SYMMETRIC_ALGORITHM = "AES";
    public static final String ASYMMETRIC_ALGORITHM = "RSA";
    public static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    public static final int RSA_KEY_SIZE = 2048;

    private static SecretKey secretKey;

    /** UUID  Methods */

    public static String newUUIDString() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    /** Symmetric Key Cipher Methods */

    private CryptoUtils() {
        // Does not allow this class to be instantiated. //
    }

    private static Key getSymmetricKey() {
        return CryptoUtils.secretKey;
    }

    public static void storeAESKey(final Activity activity, SecretKey key) throws FailedOperationException {
        try {
            FileOutputStream outputStream = activity.openFileOutput(SECRECT_KEY_FILENAME, Context.MODE_PRIVATE);
            outputStream.write(key.getEncoded());
            outputStream.close();
        }
        catch (Exception ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }

    public static SecretKey loadAESKeys(final Activity activity) throws FailedOperationException {
        try {
            InputStream inputStream = activity.openFileInput(SECRECT_KEY_FILENAME);
            byte[] bytes = inputStreamToByteArray(inputStream);
            return new SecretKeySpec(bytes, SYMMETRIC_ALGORITHM);
        }
        catch (Exception ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }

    public static SecretKey generateAesKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
            keyGenerator.init(256);
            SecretKey key = keyGenerator.generateKey();
            CryptoUtils.secretKey = key;
            return key;
        }
        catch (Exception e) {
            // Should never be here.
            return null;
        }
    }

    public static byte[] cipherWithAes(byte[] plainBytes) {
        return cipherWithAes(getSymmetricKey(), plainBytes);
    }

    public static byte[] cipherWithAes(Key key, byte[] plainBytes) {
        return useSymmetricCipher(Cipher.ENCRYPT_MODE, key, plainBytes);
    }

    public static byte[] decipherWithAes(byte[] cipheredBytes) {
        return decipherWithAes(getSymmetricKey(), cipheredBytes);
    }

    public static byte[] decipherWithAes(Key key, byte[] cipheredBytes) {
        return useSymmetricCipher(Cipher.DECRYPT_MODE, key, cipheredBytes);
    }

    private static byte[] useSymmetricCipher(int mode, Key key, byte[] initialBytes) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
            cipher.init(mode, key);
            return cipher.doFinal(initialBytes);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException ex) {
            logError(CRYPTO_UTILS_TAG, "Could not cipher / decipher");
            return null;
        }
    }

    /** Asymmetric Key Cipher Methods */

    public static void storeRSAKeys(final Activity activity, KeyPair keys) throws FailedOperationException {
        try {
            FileOutputStream outputStream = activity.openFileOutput(PRIVATE_KEY_FILENAME, Context.MODE_PRIVATE);
            PrivateKey privateKey = keys.getPrivate();
            outputStream.write(privateKey.getEncoded());

            outputStream = activity.openFileOutput(PUBLIC_KEY_FILENAME, Context.MODE_PRIVATE);
            PublicKey publicKey = keys.getPublic();
            outputStream.write(publicKey.getEncoded());
            outputStream.close();
        }
        catch (Exception ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }

    public static KeyPair loadRSAKeys(final Activity activity) throws FailedOperationException {
        try {
            InputStream inputStream = activity.openFileInput(PRIVATE_KEY_FILENAME);
            byte[] bytes = inputStreamToByteArray(inputStream);
            PKCS8EncodedKeySpec privateKS = new PKCS8EncodedKeySpec(bytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ASYMMETRIC_ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKS);

            inputStream = activity.openFileInput(PUBLIC_KEY_FILENAME);
            bytes = inputStreamToByteArray(inputStream);
            X509EncodedKeySpec publicKS = new X509EncodedKeySpec(bytes);
            keyFactory = KeyFactory.getInstance(ASYMMETRIC_ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(publicKS);

            return new KeyPair(publicKey, privateKey);
        }
        catch (Exception ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }

    public static void sendPublicKeyToServer(final Activity activity, PublicKey publicKey) {
        String url = activity.getString(R.string.p2photo_host) + activity.getString(R.string.new_member_key);

        try {
            String pKey = byteArrayToBase64String(publicKey.getEncoded());

            JSONObject requestBody = new JSONObject();
            requestBody.put("calleeUsername", getUsername(activity));
            requestBody.put("publicKey", pKey);
            RequestData requestData = new PostRequestData(activity, RequestData.RequestType.NEW_MEMBER_PUBLIC_KEY, url, requestBody);
            ResponseData result = new P2PWebServerMediator().execute(requestData).get();

            int code = result.getServerCode();
            if (code != HttpURLConnection.HTTP_OK) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String msg = "The new member key  operation was unsuccessful. Server response code: " + code + ".\n" + result.getPayload().getMessage() + "\n" + errorResponse.getReason();
                LogManager.logError(LogManager.NEW_MEMBER_KEY, msg);
                throw new FailedOperationException();
            }
        }
        catch (JSONException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FailedOperationException(ex.getMessage());
        }
    }

    public static KeyPair generateRSAKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM);
        keyGen.initialize(RSA_KEY_SIZE);
        return keyGen.generateKeyPair();
    }

    public static byte[] cipherWithRSA(String data, String base64PublicKey) throws RSAException {
        return cipherWithRSA(data.getBytes(), base64StringToPublicKey(base64PublicKey));
    }

    public static byte[] cipherWithRSA(byte[] data, String base64PublicKey) throws RSAException {
        return cipherWithRSA(data, base64StringToPublicKey(base64PublicKey));
    }

    public static byte[] cipherWithRSA(String data, PublicKey key) throws RSAException {
        return cipherWithRSA(data.getBytes(), key);
    }

    public static byte[] cipherWithRSA(byte[] data, PublicKey key) throws RSAException {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (Exception exc) {
            throw new RSAException(exc.getMessage());
        }
    }

    public static byte[] decipherWithRSA(String data, String base64PrivateKey) throws RSAException {
        return decipherWithRSA(data.getBytes(), base64StringToPrivateKey(base64PrivateKey));
    }

    public static byte[] decipherWithRSA(byte[] data, String base64PrivateKey) throws RSAException {
        return decipherWithRSA(data, base64StringToPrivateKey(base64PrivateKey));
    }

    public static byte[] decipherWithRSA(String data, PrivateKey privateKey) throws RSAException {
        return decipherWithRSA(data.getBytes(), privateKey);
    }

    public static byte[] decipherWithRSA(byte[] data, PrivateKey privateKey) throws RSAException {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(data);
        } catch (Exception exc) {
            throw new RSAException(exc.getMessage());
        }
    }

    public static String signData(PrivateKey key, JSONObject data) throws SignatureException {
        return byteArrayToBase64String(signWithSHA1withRSA(key, data));
    }

    public static byte[] signWithSHA1withRSA(PrivateKey key, JSONObject data) throws SignatureException {
        return signWithSHA1withRSA(key, data.toString().getBytes());
    }

    public static byte[] signWithSHA1withRSA(PrivateKey key, byte[] data) throws SignatureException {
        try {
            Signature sign = Signature.getInstance(SIGNATURE_ALGORITHM);
            sign.initSign(key);
            sign.update(data);
            return sign.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException exc) {
            throw new SignatureException(exc.getMessage());
        }
    }

    public static boolean verifySignatureWithSHA1withRSA(PublicKey key, JSONObject message) {
        try {
            byte[] signatureBytes = base64StringToByteArray(message.getString("signature"));
            message.remove("signature");
            return verifySignatureWithSHA1withRSA(key, signatureBytes, message.toString().getBytes());
        } catch (JSONException jsone) {
            logError(CRYPTO_UTILS_TAG, "verifySignatureWithSHA1withRSA couldn't getString('signature')");
        } catch (SignatureException exc) {
            logError(CRYPTO_UTILS_TAG, "verifySignatureWithSHA1withRSA couldn't verify signature due to exceptions");
        }
        return false;
    }

    private static boolean verifySignatureWithSHA1withRSA(PublicKey key, byte[] signatureBytes, byte[] message) throws SignatureException {
        try {
            Signature sign = Signature.getInstance(SIGNATURE_ALGORITHM);
            sign.initVerify(key);
            sign.update(message);
            return sign.verify(signatureBytes);
        } catch (Exception exc) {
            throw new SignatureException(exc.getMessage());
        }
    }
}
