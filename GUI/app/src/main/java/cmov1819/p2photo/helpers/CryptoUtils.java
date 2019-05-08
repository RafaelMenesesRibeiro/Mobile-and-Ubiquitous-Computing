package cmov1819.p2photo.helpers;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class CryptoUtils {
    private static final String SYMMETRIC_KEY = "AES/CBC/PKCS5Padding";
    private static final IvParameterSpec IV_PARAMETER_SPEC = new IvParameterSpec(new byte[]
            { 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00 });

    private static  SecretKey secretKey;

    private static void generateSymmetricKey() throws SignatureException {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_KEY);
            CryptoUtils.secretKey = keyGenerator.generateKey();
        }
        catch (NoSuchAlgorithmException ex) {
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
            cipher = Cipher.getInstance(SYMMETRIC_KEY);
            cipher.init(mode, CryptoUtils.secretKey, CryptoUtils.IV_PARAMETER_SPEC);
            return cipher.doFinal(initialBytes);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException |
                IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException ex) {
            throw new SignatureException(ex.getMessage());
        }
    }
}
