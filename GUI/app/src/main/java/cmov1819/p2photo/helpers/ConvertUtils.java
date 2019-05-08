package cmov1819.p2photo.helpers;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.android.gms.common.util.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

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

}
