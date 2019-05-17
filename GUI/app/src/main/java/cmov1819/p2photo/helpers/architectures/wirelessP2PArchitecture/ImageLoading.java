package cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;

import com.google.android.gms.common.util.IOUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.SecretKey;

import cmov1819.p2photo.helpers.managers.KeyManager;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.helpers.managers.WifiDirectManager;

import static cmov1819.p2photo.helpers.ConvertUtils.bitmapToByteArray;
import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayToBitmap;
import static cmov1819.p2photo.helpers.ConvertUtils.jsonArrayToArrayList;
import static cmov1819.p2photo.helpers.CryptoUtils.cipherWithAes;
import static cmov1819.p2photo.helpers.CryptoUtils.decipherWithAes;
import static cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.CatalogOperations.addPhotoToStack;
import static cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.CatalogOperations.readCatalog;
import static cmov1819.p2photo.helpers.managers.LogManager.NEW_CATALOG_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.toast;
import static cmov1819.p2photo.helpers.termite.Consts.MEMBERS_PHOTOS;

public class ImageLoading {
    private ImageLoading() {
        // Does not allow this class to be instantiated. //
    }

    public static List<Bitmap> getBitmapsFromFileStorage(final Activity activity, final String catalogId) {
        // Retrieve catalog file contents as a JSON Object and compare them to the received catalog file
        try {
            JSONObject catalogFileContents = readCatalog(activity, catalogId);
            // Append photoId to the user photoId arrays under memberPhotos dictionary
            JSONObject membersPhotosMap = catalogFileContents.getJSONObject(MEMBERS_PHOTOS);
            return loadMapPhotos(activity, membersPhotosMap, catalogId);
        }
        catch (IOException | JSONException exc) {
            logError(NEW_CATALOG_TAG, exc.getMessage());
            toast(activity, "Failed to add photo to catalog");
            return new ArrayList<>();
        }
    }

    private static List<Bitmap> loadMapPhotos(final Activity activity,
                                              final JSONObject memberPhotos,
                                              final String catalogId) {

        List<Bitmap> loadedPhotos = new ArrayList<>();
        List<String>  missingPhotos = new ArrayList<>();

        Iterator<String> receivedMembers = memberPhotos.keys();
        while (receivedMembers.hasNext()) {

            String fileName;
            String currentMember = receivedMembers.next();
            try {
                JSONArray jsonArray = memberPhotos.getJSONArray(currentMember);
                List<String> currentMemberPhotos = jsonArrayToArrayList(jsonArray);
                for (String photoName : currentMemberPhotos) {
                    try {
                        Bitmap loadedPhoto = loadPhoto(activity, photoName);
                        loadedPhotos.add(loadedPhoto);
                    } catch (FileNotFoundException fnfe) {
                        missingPhotos.add(photoName);
                    }
                }
            }
            catch (JSONException jsone) {
                // swallow
            }
        }

        if (!missingPhotos.isEmpty()) {
            WifiDirectManager.pullPhotos(missingPhotos, catalogId);
        }

        return loadedPhotos;
    }

    public static void savePhoto(Activity activity, String fileName, Bitmap bitmap) throws IOException, JSONException {
        byte[] bitmapBytes = bitmapToByteArray(bitmap);
        savePhoto(activity, fileName, bitmapBytes);
    }

    public static void savePhoto(Activity activity, String fileName, byte[] contents) throws IOException, JSONException {
        SecretKey secretKey = KeyManager.getInstance().getmSecretKey();
        byte[] cipheredBitmap = cipherWithAes(secretKey, contents);

        String representsMyPhoto = "_" + SessionManager.getUsername(activity) + "_";
        if (!fileName.contains(representsMyPhoto)) {
            addPhotoToStack(activity, fileName);
        }
        FileOutputStream outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
        outputStream.write(cipheredBitmap);
        outputStream.close();
    }

    public static Bitmap loadPhoto(final Activity activity, final String fileName) throws FileNotFoundException {
        InputStream inputStream = activity.openFileInput(fileName);
        try {
            byte[] cipheredBitmap = IOUtils.toByteArray(inputStream);
            SecretKey secretKey = KeyManager.getInstance().getmSecretKey();
            byte[] bitmapBytes = decipherWithAes(secretKey, cipheredBitmap);
            return byteArrayToBitmap(bitmapBytes);
        } catch (IOException ieo) {
            throw new FileNotFoundException("IOException when converting inputStream to bitmap");
        }
    }

    public static void deletePhoto(final Activity activity, final String filename) {
        File dir = activity.getFilesDir();
        File file = new File(dir, filename);
        try {
            file.delete();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        LogManager.logError("IMAGE LOADING", "tried to delete file: " + filename);
    }
}
