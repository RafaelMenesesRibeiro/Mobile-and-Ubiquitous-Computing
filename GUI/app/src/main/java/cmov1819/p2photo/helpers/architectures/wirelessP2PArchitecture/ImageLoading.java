package cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture;

import android.app.Activity;
import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cmov1819.p2photo.helpers.managers.LogManager;

import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToBitmap;

public class ImageLoading {
    private ImageLoading() {
        // Does not allow this class to be instantiated. //
    }

    public static List<Bitmap> getBitmapsFromFileStorage(final Activity activity,
                                                         final String catalogId) {
        // Retrieve catalog file contents as a JSON Object and compare them to the received catalog file
        try {
            JSONObject catalogFileContents = CatalogOperations.readCatalog(activity, catalogId);
            // Append photoId to the user photoId arrays under memberPhotos dictionary
            JSONObject membersPhotosMap = catalogFileContents.getJSONObject("membersPhotos");
            return loadMapPhotos(activity, membersPhotosMap);
        }
        catch (IOException | JSONException exc) {
            LogManager.logError(LogManager.NEW_CATALOG_TAG, exc.getMessage());
            LogManager.toast(activity, "Failed to add photo to catalog");
            return new ArrayList<>();
        }
    }

    private static List<Bitmap> loadMapPhotos(final Activity activity,
                                              final JSONObject membersPhotosMap) {

        List<Bitmap> loadedPhotos = new ArrayList<>();
        List<String>  photoNotFoundList = new ArrayList<>();

        Iterator<String> receivedMembers = membersPhotosMap.keys();
        while (receivedMembers.hasNext()) {
            String fileName;
            String currentMember = receivedMembers.next();
            try {
                JSONArray currentMemberPhotos = membersPhotosMap.getJSONArray(currentMember);
                for (int photoIdx = 0; photoIdx < currentMemberPhotos.length(); photoIdx++) {
                    fileName = currentMemberPhotos.getString(photoIdx);
                    try {
                        Bitmap loadedPhoto = loadPhoto(activity, fileName);
                        loadedPhotos.add(loadedPhoto);
                    } catch (FileNotFoundException fnfe) {
                        photoNotFoundList.add(fileName);
                    }
                }
            } catch (JSONException jsone) {
                continue;
            }
        }
        loadedPhotos.addAll(requestMissingPhotosToPeers(photoNotFoundList));
        return loadedPhotos;
    }


    public static Bitmap loadPhoto(final Activity activity, final String fileName) throws FileNotFoundException {
        InputStream inputStream = activity.openFileInput(fileName);
        try {
            return inputStreamToBitmap(inputStream);
        } catch (IOException ieo) {
            throw new FileNotFoundException("IOException when converting inputStream to bitmap");
        }
    }

    private static List<Bitmap> requestMissingPhotosToPeers(List<String> photoNotFoundList) {
        // TODO for each fileName in list, request photo, convert it to bit map, append to list and store it;
        return new ArrayList<>();
    }
}
