package cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.ViewCatalogFragment;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.architectures.BaseArchitecture;
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

    /**********************************************************
     * ADD PHOTO
     ***********************************************************/

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

    /**********************************************************
     * NEW CATALOG SLICE
     ***********************************************************/

    @Override
    public void newCatalogSlice(Activity activity, String catalogID, String catalogTitle) {
        final String username = SessionManager.getUsername(activity);

        // Make catalog folder if it doesn't exist in private storage, otherwise retrieve it
        java.io.File catalogFolder = activity.getDir(catalogID, Context.MODE_PRIVATE);
        // Create catalog.json file
        try {

            // Create file content representation
            Map<String, List<String>> membersPhotosMap = new HashMap<>();
            membersPhotosMap.put(username, new ArrayList<String>());
            JSONObject memberPhotosMapObject = new JSONObject(membersPhotosMap);

            JSONObject catalogFile = new JSONObject();
            catalogFile.put("catalogId", catalogID);
            catalogFile.put("catalogTitle", catalogTitle);
            catalogFile.put("membersPhotos", memberPhotosMapObject);
            // Write them to application storage space
            String fileName = String.format("catalog_%s.json", catalogID);
            FileOutputStream outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(catalogFile.toString().getBytes("UTF-8"));
            outputStream.close();
        }
        catch (JSONException | IOException exc) {
            LogManager.logError(LogManager.NEW_CATALOG_TAG, exc.getMessage());
            LogManager.toast(activity, "Failed to create catalog slice");
        }
    }

    /**********************************************************
     * VIEW CATALOG
     ***********************************************************/

    @Override
    public void viewCatalog(Activity activity, View view, String catalogID, String catalogTitle) {
        List<Bitmap> bitmaps = ImageLoading.getBitmapsFromFileStorage(activity, catalogID);
        ViewCatalogFragment.drawImages(view, activity, bitmaps);
        LogManager.logViewCatalog(catalogID, catalogTitle);
    }
}
