package cmov1819.p2photo.helpers.architectures;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.NewCatalogFragment;
import cmov1819.p2photo.ViewCatalogFragment;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.SessionManager;

import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToBitmap;
import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToString;

public class WirelessP2PArchitecture extends BaseArchitecture {
    @Override
    public void handlePendingMemberships(Activity activity) {
        MainMenuActivity.handlePendingMembershipsWifiDirect(activity);
    }

    @Override
    public void setup(View view, LoginActivity loginActivity) {
        LoginActivity.goHome(loginActivity);
    }

    @Override
    public void addPhoto(FragmentActivity activity, String catalogId, File file) throws FailedOperationException {
        // Reads the temp image's bytes.
        int fileLength = (int) file.length();
        byte[] fileContents = new byte[fileLength];

        try {
            InputStream inputStream = new FileInputStream(file);
            int bytesRead = inputStream.read(fileContents);
            if (bytesRead != fileLength) {
                throw new FailedOperationException("Couldn't read the image file.");
            }
            inputStream.close();
        }
        catch(IOException ioe){
            throw new FailedOperationException(ioe.getMessage());
        }

        // Saves the temp image's bytes to internal storage in a permanent file.
        String username = SessionManager.getUsername(activity);
        String photoName = String.format("%s_%s_%s", catalogId, username, UUID.randomUUID().toString().replace("/", ""));
        FileOutputStream outputStream;
        try {
            outputStream = activity.openFileOutput(photoName, Context.MODE_PRIVATE);
            outputStream.write(fileContents);
            outputStream.close();
        }
        catch (IOException ex) {
            throw new FailedOperationException(ex.getMessage());
        }

        updateCatalogFile(activity, catalogId, SessionManager.getUsername(activity), photoName);
    }

    @Override
    public void newCatalogSlice(Activity activity, String catalogID, String catalogTitle) {
        NewCatalogFragment.newCatalogSliceWifiDirectArch(activity, catalogID, catalogTitle);
    }

    @Override
    public void viewCatalog(Activity activity, View view, String catalogID, String catalogTitle) {
        List<Bitmap> bitmaps = getBitmapsFromFileStorage(activity, catalogID);
        ViewCatalogFragment.drawImages(view, activity, bitmaps);
        LogManager.logViewCatalog(catalogID, catalogTitle);
    }

    private void updateCatalogFile(final Activity activity,
                                  final String catalogId,
                                  final String owner,
                                  final String photoId) {

        // Retrieve catalog file contents as a JSON Object and compare them to the received catalog file
        try {
            // Load contents
            String fileName = String.format("catalog_%s.json", catalogId);
            InputStream inputStream = activity.openFileInput(fileName);
            JSONObject catalogFileContents = new JSONObject(inputStreamToString(inputStream));
            // Append photoId to the user photoId arrays under memberPhotos dictionary
            catalogFileContents.getJSONObject("membersPhotos").getJSONArray(owner).put(photoId);
            // Save to disk
            FileOutputStream outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(catalogFileContents.toString().getBytes("UTF-8"));
            outputStream.close();

        } catch (IOException | JSONException exc) {
            LogManager.logError(LogManager.NEW_CATALOG_TAG, exc.getMessage());
            LogManager.toast(activity, "Failed to add photo to catalog");
        }
    }

    private List<Bitmap> getBitmapsFromFileStorage(final Activity activity,
                                                  final String catalogId) {
        // Retrieve catalog file contents as a JSON Object and compare them to the received catalog file
        try {
            // Load contents
            String fileName = String.format("catalog_%s.json", catalogId);
            InputStream inputStream = activity.openFileInput(fileName);
            JSONObject catalogFileContents = new JSONObject(inputStreamToString(inputStream));
            // Append photoId to the user photoId arrays under memberPhotos dictionary
            JSONObject membersPhotosMap = catalogFileContents.getJSONObject("membersPhotos");
            return loadMapPhotos(activity, membersPhotosMap);
        } catch (IOException | JSONException exc) {
            LogManager.logError(LogManager.NEW_CATALOG_TAG, exc.getMessage());
            LogManager.toast(activity, "Failed to add photo to catalog");
            return new ArrayList<>();
        }
    }

    private List<Bitmap> loadMapPhotos(final Activity activity,
                                       final JSONObject membersPhotosMap) {

        List<Bitmap> loadedPhotos = new ArrayList<>();
        Map<String, String> photosNotFoundMap = new HashMap<>();

        Iterator<String> receivedMembers = membersPhotosMap.keys();
        while (receivedMembers.hasNext()) {
            String fileName = "";
            String currentMember = receivedMembers.next();
            try {
                JSONArray currentMemberPhotos = membersPhotosMap.getJSONArray(currentMember);
                for (int photoIdx = 0; photoIdx < currentMemberPhotos.length(); photoIdx++) {
                    fileName = currentMemberPhotos.getString(photoIdx);
                    try {
                        Bitmap loadedPhoto = loadPhoto(activity, fileName);
                        loadedPhotos.add(loadedPhoto);
                    } catch (FileNotFoundException fnfe) {
                        photosNotFoundMap.put(currentMember, fileName);
                    }
                }
            } catch (JSONException jsone) {
                continue;
            }
        }
        loadedPhotos.addAll(requestMissingPhotosToPeers(photosNotFoundMap));
        return loadedPhotos;
    }


    private Bitmap loadPhoto(final Activity activity, final String fileName) throws FileNotFoundException {
        InputStream inputStream = activity.openFileInput(fileName);
        try {
            return inputStreamToBitmap(inputStream);
        } catch (IOException ieo) {
            throw new FileNotFoundException("IOException when converting inputStream to bitmap");
        }
    }

    private List<Bitmap> requestMissingPhotosToPeers(Map<String, String> photoNotFoundList) {
        // TODO for each fileName in list, request photo, convert it to bit map, append to list and store it;
        return new ArrayList<>();
    }

}
