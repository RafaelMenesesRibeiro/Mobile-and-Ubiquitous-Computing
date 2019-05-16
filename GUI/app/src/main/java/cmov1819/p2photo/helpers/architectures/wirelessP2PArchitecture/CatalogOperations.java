package cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture;

import android.app.Activity;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.util.Base64;

import cmov1819.p2photo.helpers.ConvertUtils;
import cmov1819.p2photo.helpers.CryptoUtils;

import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToString;

public class CatalogOperations {

    public static JSONObject readCatalog(Activity activity, String catalogID) throws IOException, JSONException {
        String fileName = String.format("catalog_%s.json", catalogID);
        InputStream inputStream = activity.openFileInput(fileName);
        String encodedNEncrypted = inputStreamToString(inputStream);
        byte[] encrypted = Base64.decode(encodedNEncrypted, Base64.DEFAULT);
        byte[] decrypted = CryptoUtils.decipherWithAes(encrypted);
        return new JSONObject(new String(decrypted));
    }

    public static void writeCatalog(Activity activity, String catalogID, JSONObject contents) throws IOException {
        String fileName = String.format("catalog_%s.json", catalogID);
        byte[] decrypted = ConvertUtils.JSONObjectToByteAarray(contents, 4);
        byte[] encrypted = CryptoUtils.cipherWithAes(decrypted);
        String encodedNEncrypted = Base64.encodeToString(encrypted, Base64.DEFAULT);
        FileOutputStream outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
        outputStream.write(encodedNEncrypted.getBytes());
        outputStream.close();
    }


}
