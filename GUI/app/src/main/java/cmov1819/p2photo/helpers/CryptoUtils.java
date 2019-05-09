package cmov1819.p2photo.helpers;

import android.annotation.TargetApi;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import cmov1819.p2photo.helpers.managers.LogManager;

public class CryptoUtils {
    private static final String SYMMETRIC_CYPHER_PROPS = "AES/CBC/NoPadding";
    private static final String KEY_STORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_STORE_ALIAS = "MOC_1819_P2PHOTO_ALIAS";

    private static final byte[] AES_IV = initIv();

    public static final IvParameterSpec IV_PARAMETER_SPEC = new IvParameterSpec(new byte[]
            { 0, 0, 0, 0,
                    0, 0, 0,
                    0, 0, 0,
                    0, 0, 0,
                    0, 0, 0 });

    private static  SecretKey secretKey;

    private static byte[] kkk;


    private static byte[] initIv() {

        try {
            Cipher cipher = Cipher.getInstance(SYMMETRIC_CYPHER_PROPS);
            int blockSize = cipher.getBlockSize();
            byte[] iv = new byte[blockSize];
            for (int i = 0; i < blockSize; ++i) {
                iv[i] = 0;
            }
            return iv;
        }
        catch (Exception e) {
            int blockSize = 16;
            byte[] iv = new byte[blockSize];
            for (int i = 0; i < blockSize; ++i) {
                iv[i] = 0;
            }
            return iv;
        }
    }






    // TODO - Change this. //
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void initializeSymmetricKey() throws SignatureException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_PROVIDER);
            keyStore.load(null);
            keyStore.deleteEntry(KEY_STORE_ALIAS);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        /*
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
        */
    }


    @TargetApi(Build.VERSION_CODES.M)
    private static Key getSymmetricKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_PROVIDER);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_STORE_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator
                        .getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_PROVIDER);
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder(KEY_STORE_ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
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



















    @TargetApi(Build.VERSION_CODES.M)
    public static SecretKey generateAes256Key() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES);
            return keyGenerator.generateKey();
        }
        catch (NoSuchAlgorithmException e) {
            // Should never be here.
            return null;
        }
    }

    public static byte[] cipherWithAes256(SecretKey key, byte[] plainBytes) throws SignatureException {
        return symmetricCipher(Cipher.ENCRYPT_MODE, key, plainBytes);
    }

    public static byte[] cipherWithAes256(byte[] plainBytes) throws SignatureException {
        return symmetricCipher(Cipher.ENCRYPT_MODE, CryptoUtils.secretKey, plainBytes);
    }

    public static byte[] decipherWithAes256(SecretKey key, byte[] cipheredBytes) throws SignatureException {
        return symmetricCipher(Cipher.DECRYPT_MODE, key, cipheredBytes);
    }

    public static byte[] decipherWithAes256(byte[] cipheredBytes) throws SignatureException {
        return symmetricCipher(Cipher.DECRYPT_MODE, CryptoUtils.secretKey, cipheredBytes);
    }

    private static byte[] symmetricCipher(int mode, SecretKey key, byte[] initialBytes) throws SignatureException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(SYMMETRIC_CYPHER_PROPS);
            Key key2 = getSymmetricKey();
            cipher.init(mode, key2, IV_PARAMETER_SPEC);
            return cipher.doFinal(initialBytes);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException ex) {
            // TODO - Change throw type//
            throw new SignatureException(ex.getMessage());
        }
    }



    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static byte[] encrypt2(byte[] plainText) {
        try {
            SecureRandom secureRandom = new SecureRandom();
            byte[] key = new byte[16];
            secureRandom.nextBytes(key);
            kkk = key;

            SecretKey secretKey = new SecretKeySpec(key, "AES");
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);

            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv); //128 bit auth tag length
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText);
            ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + cipherText.length);
            byteBuffer.putInt(iv.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            byte[] cipherMessage = byteBuffer.array();
            return cipherMessage;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-43);
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static byte[] decypt2(byte[] cipherMessage) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
            int ivLength = byteBuffer.getInt();
            if (ivLength < 12 || ivLength >= 16) { // check input parameter
                LogManager.logError("SDASDASDASD", "JIODJAPDJAPDJPSAJDPAJPDJ  \n\n\n\n SFOPFOPAKOPSAPOKSA");
                throw new IllegalArgumentException("invalid iv length");
            }
            byte[] iv = new byte[ivLength];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] key = kkk;

            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] plainText = cipher.doFinal(cipherText);
            return plainText;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-43);
            return null;
        }
    }


}
