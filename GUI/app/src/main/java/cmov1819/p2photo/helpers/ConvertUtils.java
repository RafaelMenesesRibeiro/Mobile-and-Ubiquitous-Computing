package cmov1819.p2photo.helpers;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.android.gms.common.util.IOUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import static cmov1819.p2photo.helpers.CryptoUtils.ASYMMETRIC_ALGORITHM;

public class ConvertUtils {
    public static String inputStreamToString(InputStream inputStream) throws IOException {
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }

    public static Bitmap inputStreamToBitmap(InputStream inputStream) throws IOException {
        byte[] bitmapBytes = IOUtils.toByteArray(inputStream);
        return BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
    }

    public byte[] bitmapToByteArray(Bitmap bitmap, int quality) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, quality, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static Bitmap byteArrayOutputStreamToBitmap(ByteArrayOutputStream outputStream) throws IOException {
        byte[] bitmapBytes = outputStream.toByteArray();
        return BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
    }

    public static byte[] utf8StringToByteArray(String string) {
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            return null;
        }
    }

    public static String byteArrayToBase64String(byte[] data) {
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    public static byte[] base64StringToByteArray(String string) {
        return Base64.decode(string, Base64.DEFAULT);
    }

    public static String byteArrayToUtf8(byte[] data) {
        try {
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            return null;
        }
    }

    public static byte[] JSONObjectToByteArray(JSONObject contents) {
        try {
            return contents.toString().getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            // Ignored
            return null;
        }
    }

    public static byte[] JSONObjectToByteArray(JSONObject contents, int indentSpaces) {
        try {
            return contents.toString(indentSpaces).getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException | JSONException e) {
            // Ignored
            return null;
        }
    }

    public static PrivateKey base64StringToPrivateKey(String base64PrivateKey) throws RSAException {
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(base64StringToByteArray(base64PrivateKey));
            KeyFactory keyFactory = KeyFactory.getInstance(ASYMMETRIC_ALGORITHM);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception exc) {
            throw new RSAException(exc.getMessage());
        }
    }

    public static PublicKey base64StringToPublicKey(String base64PublicKey) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(base64StringToByteArray(base64PublicKey));
        KeyFactory keyFactory = KeyFactory.getInstance(ASYMMETRIC_ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }
}
