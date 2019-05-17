package cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cmov1819.p2photo.helpers.ConvertUtils;
import cmov1819.p2photo.helpers.CryptoUtils;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.SessionManager;

import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToString;
import static cmov1819.p2photo.helpers.DateUtils.generateTimestamp;
import static cmov1819.p2photo.helpers.DateUtils.isOneTimestampBeforeAnother;

public class CatalogOperations {
    private static final String PHOTO_STACK_FILE_NAME = "photosStack.json";
    private static int replicationLimitInPhotos = 25;

    public static JSONObject readCatalog(Activity activity, String catalogID) throws IOException, JSONException {
        String fileName = String.format("catalog_%s.json", catalogID);
        File file = activity.getApplicationContext().getFileStreamPath(fileName);
        if (file == null || !file.exists()) {
            LogManager.logInfo(LogManager.VIEW_CATALOG_TAG, "Catalog " + fileName + "not found");
            JSONObject emptyCatalog = new JSONObject();
            emptyCatalog.put("catalogId", catalogID);
            emptyCatalog.put("catalogTitle", "");
            Map<String, List<String>> membersPhotos = new HashMap<>();
            membersPhotos.put(SessionManager.getUsername(activity), new ArrayList<String>());
            emptyCatalog.put("membersPhotos", new JSONObject(membersPhotos));
            return emptyCatalog;
        }

        InputStream inputStream = activity.openFileInput(fileName);
        String read = inputStreamToString(inputStream);
        return new JSONObject(read);
    }

    public static void writeCatalog(Activity activity, String catalogID, JSONObject contents) throws IOException {
        String fileName = String.format("catalog_%s.json", catalogID);
        byte[] toWrite = ConvertUtils.JSONObjectToByteArray(contents, 4);
        FileOutputStream outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
        outputStream.write(toWrite);
        outputStream.close();
    }

    public static void setReplicationLimitInPhotos(final Activity activity, int replicationLimitInBytes) throws IOException, JSONException {
        CatalogOperations.replicationLimitInPhotos = (int) Math.ceil(replicationLimitInBytes / 5);
        checkIfStorageOverflows(activity);
    }

    private static void checkIfStorageOverflows(final Activity activity) throws IOException, JSONException {
        JSONObject jsonObject = readPhotoStack(activity);
        int photosInStack = jsonObject.length();
        int delta = photosInStack - replicationLimitInPhotos;
        if (delta < 0) {
            return;
        }
        deletePhotosFromStack(activity, delta);
    }

    public static void createPhotoStackFile(final Activity activity) throws IOException {
        JSONObject jsonObject = new JSONObject();
        writePhotoStack(activity, jsonObject);
    }

    private static JSONObject readPhotoStack(final Activity activity) throws IOException, JSONException {
        InputStream inputStream = activity.openFileInput(PHOTO_STACK_FILE_NAME);
        String encodedNEncrypted = inputStreamToString(inputStream);
        byte[] encrypted = Base64.decode(encodedNEncrypted, Base64.DEFAULT);
        byte[] decrypted = CryptoUtils.decipherWithAes(encrypted);
        return new JSONObject(new String(decrypted));
    }

    private static void writePhotoStack(final Activity activity, JSONObject jsonObject) throws IOException {
        byte[] decrypted = ConvertUtils.JSONObjectToByteArray(jsonObject, 4);
        byte[] encrypted = CryptoUtils.cipherWithAes(decrypted);
        String encodedNEncrypted = Base64.encodeToString(encrypted, Context.MODE_PRIVATE);
        FileOutputStream outputStream = activity.openFileOutput(PHOTO_STACK_FILE_NAME, Context.MODE_PRIVATE);
        outputStream.write(encodedNEncrypted.getBytes());
        outputStream.close();
    }

    public static void addPhotoToStack(final Activity activity, String photoName) throws IOException, JSONException{
        JSONObject jsonObject = readPhotoStack(activity);
        String timestamp = generateTimestamp();
        jsonObject.put(photoName, timestamp);
        writePhotoStack(activity, jsonObject);
        checkIfStorageOverflows(activity);
    }

    private static void deletePhotosFromStack(final Activity activity, int numberToDelete) throws IOException, JSONException {
        JSONObject jsonObject = readPhotoStack(activity);
        for (int i = 0; i < numberToDelete; i++) {
            String key = findOldest(jsonObject);
            jsonObject.remove(key);
            ImageLoading.deletePhoto(activity, key);
        }
    }

    private static String findOldest(JSONObject jsonObject) throws JSONException {
        String oldest = "";
        for (Iterator key = jsonObject.keys(); key.hasNext(); ) {
            String keyS = (String) key.next();
            String timestamp = jsonObject.getString(keyS);
            if (isOneTimestampBeforeAnother(timestamp, oldest)) {
                oldest = keyS;
            }
        }
        return oldest;
    }
}
