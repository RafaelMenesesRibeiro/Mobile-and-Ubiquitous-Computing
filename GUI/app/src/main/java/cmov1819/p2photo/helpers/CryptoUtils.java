package cmov1819.p2photo.helpers;

import android.annotation.TargetApi;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

public class CryptoUtils {
    private static final String SYMMETRIC_CYPHER_PROPS = "AES/CBC/PKCS7Padding";
    private static final String KEY_STORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_STORE_ALIAS = "MOC_1819_P2PHOTO_ALIAS";
    private static final byte[] IV_PARAMETER_SPEC = initializeIV();

    private CryptoUtils() {
        // Does not allow this class to be instantiated. //
    }

    private static byte[] initializeIV() {
        byte[] res = new byte[16];
        for (int i = 0; i < res.length; i++) {
            res[i] = 0;
        }
        return res;
    }

    // TODO - Change this. //
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void initializeSymmetricKey() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_PROVIDER);
        keyStore.load(null);
        keyStore.deleteEntry(KEY_STORE_ALIAS);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static Key getSymmetricKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_PROVIDER);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_STORE_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_PROVIDER);
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder(KEY_STORE_ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                .setRandomizedEncryptionRequired(false)
                                .build());
                return keyGenerator.generateKey();
            }
            else {
                return keyStore.getKey(KEY_STORE_ALIAS, null);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static byte[] cipherWithAes256(byte[] plainBytes) {
        return cipherWithAes256(getSymmetricKey(), plainBytes);
    }

    public static byte[] cipherWithAes256(Key key, byte[] plainBytes) {
        return symmetricCipher(Cipher.ENCRYPT_MODE, key, plainBytes);
    }

    public static byte[] decipherWithAes256(byte[] cipheredBytes) {
        return decipherWithAes256(getSymmetricKey(), cipheredBytes);
    }

    public static byte[] decipherWithAes256(Key key, byte[] cipheredBytes) {
        return symmetricCipher(Cipher.DECRYPT_MODE, key, cipheredBytes);
    }

    // TODO - Change this. //
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static byte[] symmetricCipher(int mode, Key key, byte[] initialBytes) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(SYMMETRIC_CYPHER_PROPS);
            IvParameterSpec spec = new IvParameterSpec(IV_PARAMETER_SPEC);
            cipher.init(mode, key, spec);
            return cipher.doFinal(initialBytes);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }
}
