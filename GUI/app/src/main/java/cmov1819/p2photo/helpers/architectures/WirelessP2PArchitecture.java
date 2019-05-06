package cmov1819.p2photo.helpers.architectures;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.NewCatalogFragment;
import cmov1819.p2photo.ViewCatalogFragment;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.SessionManager;

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
            FileInputStream fis = new FileInputStream(file);
            int bytesRead = fis.read(fileContents);
            if (bytesRead != fileLength) {
                String msg = "Could not read the image file.";
                throw new FailedOperationException(msg);
            }
            fis.close();
        }
        catch(IOException ex){
            throw new FailedOperationException(ex.getMessage());
        }

        // Saves the temp image's bytes to internal storage in a permanent file.
        String username = SessionManager.getUsername(activity);
        UUID uuid = UUID.randomUUID();
        String filename = catalogId + "_" + username + "_" + uuid.toString();
        FileOutputStream outputStream;
        try {
            outputStream = activity.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(fileContents);
            outputStream.close();
        }
        catch (IOException ex) {
            throw new FailedOperationException(ex.getMessage());
        }

        updateCatalogFile(activity, catalogId, SessionManager.getUsername(activity), filename);
    }

    @Override
    public void newCatalogSlice(Activity activity, String catalogID, String catalogTitle) {
        NewCatalogFragment.newCatalogSliceWifiDirectArch(activity, catalogID, catalogTitle);
    }

    // TODO - Can only be tested once getCatalogImagePaths() is implemented. //
    @Override
    public void viewCatalog(Activity activity, View view, String catalogID, String catalogTitle) {
        // ArrayList<String> imagePaths = getCatalogImagePaths(catalogID);
        ArrayList<String> imagePaths = new ArrayList<>();
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        for (String imagePath : imagePaths) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            bitmaps.add(bitmap);
        }
        ViewCatalogFragment.drawImages(view, activity, bitmaps);
        LogManager.logViewCatalog(catalogID, catalogTitle);
    }

    public void updateCatalogFile(Activity activity, String catalogId, String owner, String photoId) {
        // Get catalog folder path from application private storage
        String catalogFolderDir = activity.getDir(catalogId, Context.MODE_PRIVATE).getAbsolutePath();
        // Retrieve catalog file contents as a JSON Object and compare them to the received catalog file
        try {
            // Load contents
            String filePath = catalogFolderDir + "/catalog.json";
            InputStream inputStream = new FileInputStream(filePath);
            JSONObject catalogFileContents = new JSONObject(inputStreamToString(inputStream));
            // Append photoId to the user photoId arrays under memberPhotos dictionary
            catalogFileContents.getJSONObject("membersPhotos").getJSONArray(owner).put(photoId);
            // Save to disk
            FileOutputStream outputStream = activity.openFileOutput(filePath, Context.MODE_PRIVATE);
            outputStream.write(catalogFileContents.toString().getBytes("UTF-8"));
            outputStream.close();

        } catch (IOException | JSONException exc) {
            LogManager.logError(LogManager.NEW_CATALOG_TAG, exc.getMessage());
            LogManager.toast(activity, "Failed to add photo to catalog");
        }
    }
}
