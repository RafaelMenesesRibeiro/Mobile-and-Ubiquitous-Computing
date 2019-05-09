package cmov1819.p2photo.helpers;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.common.util.IOUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;

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

    public static byte[] JSONObjectToByteAarray(JSONObject contents) {
        try {
            return contents.toString().getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            // Ignored
            return null;
        }
    }

    public static byte[] JSONObjectToByteAarray(JSONObject contents, int indentSpaces) {
        try {
            return contents.toString(indentSpaces).getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException | JSONException e) {
            // Ignored
            return null;
        }
    }
}
