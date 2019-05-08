package cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture;

import android.app.Activity;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToString;

public class CatalogOperations {

    public static JSONObject readCatalog(Activity activity, String catalogID) throws IOException, JSONException {
        String fileName = String.format("catalog_%s.json", catalogID);
        InputStream inputStream = activity.openFileInput(fileName);
        // TODO - Decrypt. //
        return new JSONObject(inputStreamToString(inputStream));
    }

    public static void writeCatalog(Activity activity, String catalogID, JSONObject contents) throws IOException {
        String fileName = String.format("catalog_%s.json", catalogID);
        FileOutputStream outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
        // TODO - Encrypt. //
        outputStream.write(contents.toString().getBytes("UTF-8"));
        outputStream.close();
    }
}
