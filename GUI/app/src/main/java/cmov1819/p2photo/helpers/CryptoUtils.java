package cmov1819.p2photo.helpers;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import static cmov1819.p2photo.helpers.ConvertUtils.*;
import static cmov1819.p2photo.helpers.managers.LogManager.*;

public class CryptoUtils {
    private static final String KEY_STORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_STORE_ALIAS = "MOC_1819_P2PHOTO_ALIAS";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static final String SECRET_KEY_ALGORITHM = "AES";

    private static SecretKey secretKey;

    private CryptoUtils() {
        // Does not allow this class to be instantiated. //
    }

    // TODO //
    public static Key generateSymmetricKey() {
        throw new UnsupportedOperationException();
    }

    // TODO //
    public static KeyPair generateAsymetricKeys() {
        throw new UnsupportedOperationException();
    }

    private static Key getSymmetricKey() {
        return CryptoUtils.secretKey;
    }

    public static SecretKey generateAesKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(SECRET_KEY_ALGORITHM);
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
            cipher = Cipher.getInstance(SECRET_KEY_ALGORITHM);
            cipher.init(mode, key);
            return cipher.doFinal(initialBytes);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException ex) {
            logError(CRYPTO_UTILS_TAG, "Could not cipher / decipher");
            return null;
        }
    }

    /** Public and Private Key Ciphers */

    public static byte[] sign(PrivateKey key, byte[] data) throws SignatureException {
        try {
            Signature sign = Signature.getInstance(SIGNATURE_ALGORITHM);
            sign.initSign(key);
            sign.update(data);
            return sign.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException exc) {
            throw new SignatureException(exc.getMessage());
        }
    }

    public static boolean verifySignature(PublicKey key, JSONObject message) {
        try {
            byte[] signatureBytes = base64StringToByteArray(message.getString("signature"));
            message.remove("signature");
            return verifySignature(key, signatureBytes, message.toString().getBytes());
        } catch (JSONException jsone) {
            logError(CRYPTO_UTILS_TAG, "verifySignature couldn't getString('signature')");
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException exc) {
            logError(CRYPTO_UTILS_TAG, "verifySignature couldn't verify signature due to exceptions");
        }
        return false;
    }

    private static boolean verifySignature(PublicKey key, byte[] signatureBytes, byte[] message)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
            Signature sign = Signature.getInstance(SIGNATURE_ALGORITHM);
            sign.initVerify(key);
            sign.update(message);
            return sign.verify(signatureBytes);
    }
}
