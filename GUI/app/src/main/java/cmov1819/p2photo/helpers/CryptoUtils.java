package cmov1819.p2photo.helpers;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class CryptoUtils {
    private static final String SYMMETRIC_CYPHER_PROPS = "AES/CBC/PKCS5Padding";
    private static final String KEY_STORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_STORE_ALIAS = "MOC_1819_P2PHOTO_ALIAS";

    private static final IvParameterSpec IV_PARAMETER_SPEC = new IvParameterSpec(new byte[]
            { 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00 });

    private static  SecretKey secretKey;

    // TODO - Change this. //
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void initializeSymmetricKey() throws SignatureException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_PROVIDER);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_STORE_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_PROVIDER);
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder(KEY_STORE_ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .setRandomizedEncryptionRequired(false)
                                .build());
                CryptoUtils.secretKey = keyGenerator.generateKey();
            }
            else {
                CryptoUtils.secretKey = (SecretKey) keyStore.getKey(KEY_STORE_ALIAS, null);
            }
        }
        catch (Exception ex) {
            // TODO - Remove. //
            ex.printStackTrace();
            throw new SignatureException(ex.getMessage());
        }
    }

    public static byte[] encrypt(byte[] plainBytes) throws SignatureException {
        return symmetricCipher(Cipher.ENCRYPT_MODE, plainBytes);
    }

    public static byte[] decrypt(byte[] cipheredBytes) throws SignatureException {
        return symmetricCipher(Cipher.DECRYPT_MODE, cipheredBytes);
    }

    private static byte[] symmetricCipher(int mode, byte[] initialBytes) throws SignatureException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(SYMMETRIC_CYPHER_PROPS);
            cipher.init(mode, CryptoUtils.secretKey, CryptoUtils.IV_PARAMETER_SPEC);
            return cipher.doFinal(initialBytes);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException |
                IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException ex) {
            // TODO - Change throw type//
            throw new SignatureException(ex.getMessage());
        }
    }
}
