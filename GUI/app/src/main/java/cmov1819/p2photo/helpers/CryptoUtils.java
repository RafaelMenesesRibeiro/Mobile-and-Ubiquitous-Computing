package cmov1819.p2photo.helpers;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import cmov1819.p2photo.helpers.managers.LogManager;

public class CryptoUtils {
    private static final String SECRET_KEY_ALGORITHM = "AES";
    private static final String KEY_STORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_STORE_ALIAS = "MOC_1819_P2PHOTO_ALIAS";

    private static SecretKey secretKey;

    private CryptoUtils() {
        // Does not allow this class to be instantiated. //
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
            LogManager.logError(LogManager.CRYPTO_UTILS, "Could not cipher / decipher");
            return null;
        }
    }
}
