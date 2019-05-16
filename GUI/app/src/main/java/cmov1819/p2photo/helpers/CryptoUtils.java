package cmov1819.p2photo.helpers;

import android.app.Activity;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.InputStream;
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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.exceptions.RSAException;

import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToPrivateKey;
import static cmov1819.p2photo.helpers.ConvertUtils.base64StringToPublicKey;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBase64String;
import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToByteArray;
import static cmov1819.p2photo.helpers.managers.LogManager.CRYPTO_UTILS_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;

public class CryptoUtils {
    private static final String PRIVATE_KEY_FILENAME = "P2Photo_PrivateKey.key";
    private static final String PUBLIC_KEY_FILENAME = "P2Photo_PublicKey.pub";
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

    public static SecretKey generateAesKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
            keyGenerator.init(256);
            SecretKey key = keyGenerator.generateKey();
            CryptoUtils.secretKey = key;
            return key;
        }
        catch (NoSuchAlgorithmException e) {
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

    public void storeRSAKeys(final Activity activity, KeyPair keys) throws FailedOperationException {
        try {
            FileOutputStream outputStream = activity.openFileOutput(PRIVATE_KEY_FILENAME, Context.MODE_PRIVATE);
            PrivateKey privateKey = keys.getPrivate();
            outputStream.write(privateKey.getEncoded());

            outputStream = activity.openFileOutput(PUBLIC_KEY_FILENAME, Context.MODE_PRIVATE);
            PublicKey publicKey = keys.getPublic();
            outputStream.write(publicKey.getEncoded());
        }
        catch (Exception ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }

    public KeyPair loadRSAKeys(final Activity activity) throws FailedOperationException {
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

    public KeyPair generateRSAKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM);
        keyGen.initialize(RSA_KEY_SIZE);
        return keyGen.generateKeyPair(); // use getPrivate() and getPublic() to obtain keys;
    }

    public static byte[] cipherWithRSA(String data, String base64PublicKey) throws RSAException {
        return cipherWithRSA(data.getBytes(), base64PublicKey);
    }

    public static byte[] cipherWithRSA(byte[] data, String base64PublicKey) throws RSAException {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, base64StringToPublicKey(base64PublicKey));
            return cipher.doFinal(data);
        } catch (Exception exc) {
            throw new RSAException(exc.getMessage());
        }
    }

    public static String decipherWithRSA(String data, String base64PrivateKey) throws RSAException {
        return decipherWithRSA(data.getBytes(), base64PrivateKey);
    }

    public static String decipherWithRSA(byte[] data, String base64PrivateKey) throws RSAException {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, base64StringToPrivateKey(base64PrivateKey));
            return new String(cipher.doFinal(data));
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
