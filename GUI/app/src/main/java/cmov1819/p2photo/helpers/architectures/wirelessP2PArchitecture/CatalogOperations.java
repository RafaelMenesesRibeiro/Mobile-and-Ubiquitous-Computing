package cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SignatureException;
import java.util.Base64;

import cmov1819.p2photo.helpers.ConvertUtils;
import cmov1819.p2photo.helpers.CryptoUtils;
import cmov1819.p2photo.helpers.managers.LogManager;

import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToString;

public class CatalogOperations {

    // TODO - Change this. //
    @TargetApi(Build.VERSION_CODES.O)
    public static JSONObject readCatalog(Activity activity, String catalogID) throws IOException, JSONException {
        String fileName = String.format("catalog_%s.json", catalogID);
        InputStream inputStream = activity.openFileInput(fileName);
        byte[] encrypted = inputStreamToString(inputStream).getBytes("UTF-8");
        /*
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        return new JSONObject(new String(decoded));
        */
        return new JSONObject(new String(encrypted));
    }

    // TODO - Change this //
    @TargetApi(Build.VERSION_CODES.O)
    public static void writeCatalog(Activity activity, String catalogID, JSONObject contents) throws IOException {
        String fileName = String.format("catalog_%s.json", catalogID);

        byte[] decrypted = ConvertUtils.JSONObjectToByteAarray(contents, 4);
        // byte[] encrypted = CryptoUtils.cipherWithAes256(decrypted);
        FileOutputStream outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
        // outputStream.write(encrypted);
        outputStream.write(decrypted);
        outputStream.close();
    }
}
