package cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture;

import android.app.Activity;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SignatureException;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.ConvertUtils;
import cmov1819.p2photo.helpers.CryptoUtils;
import cmov1819.p2photo.helpers.managers.LogManager;

import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToString;

public class CatalogOperations {

        public static JSONObject readCatalog(Activity activity, String catalogID) throws IOException, JSONException {
        String fileName = String.format("catalog_%s.json", catalogID);
        InputStream inputStream = activity.openFileInput(fileName);
        byte[] encrypted = inputStreamToString(inputStream).getBytes();
        /*
        byte[] decrypted = new byte[0];
        try {
            decrypted = CryptoUtils.decipherWithAes256(encrypted);
        }
        catch (SignatureException e) {
            e.printStackTrace();
        }
        return new JSONObject(new String(decrypted));
        */
        return new JSONObject(new String(encrypted));
    }

    public static void writeCatalog(Activity activity, String catalogID, JSONObject contents) throws IOException {
        String fileName = String.format("catalog_%s.json", catalogID);
        FileOutputStream outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
        byte[] decrypted = ConvertUtils.JSONObjectToByteAarray(contents, 4);
        /*
        byte[] encrypted = new byte[0];
        try {
            encrypted = CryptoUtils.cipherWithAes256(decrypted);
        }
        catch (SignatureException e) {
            e.printStackTrace();
            // TODO //
            LogManager.logError("CATALOG", "pajdpasjdpoajdposadpaosdas \n\n\n\n sdnianisajoaisjosada");
        }
        */
        outputStream.write(decrypted);
        outputStream.close();
    }


}
