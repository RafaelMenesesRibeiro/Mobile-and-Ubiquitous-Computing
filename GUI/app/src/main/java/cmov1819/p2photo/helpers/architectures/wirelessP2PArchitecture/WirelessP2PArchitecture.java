package cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cmov1819.p2photo.LimitStorageFragment;
import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.ViewCatalogFragment;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.architectures.BaseArchitecture;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.helpers.managers.WifiDirectManager;
import pt.inesc.termite.wifidirect.service.SimWifiP2pService;

import static cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.CatalogOperations.createPhotoStackFile;
import static cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.CatalogOperations.setReplicationLimitInPhotos;
import static cmov1819.p2photo.helpers.managers.SessionManager.setMaxCacheImageSize;

public class WirelessP2PArchitecture extends BaseArchitecture {
    @Override
    public void handlePendingMemberships(final Activity activity) {
    }

    @Override
    public void onSignUp(final LoginActivity loginActivity) throws FailedOperationException {
        try {
            createPhotoStackFile(loginActivity);
            setMaxCacheImageSize(loginActivity, LimitStorageFragment.DEFAULT_CACHE_VALUE);
            setReplicationLimitInPhotos(loginActivity, LimitStorageFragment.DEFAULT_CACHE_VALUE);
        }
        catch (Exception ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }

    @Override
    public void setup(final View view, final LoginActivity loginActivity) throws FailedOperationException {
        LoginActivity.initializeWifiDirectSetup(loginActivity);
    }

    @Override
    public void setupHome(MainMenuActivity mainMenuActivity) {
        if (mainMenuActivity.getIntent().hasExtra(LoginActivity.WIFI_DIRECT_SV_RUNNING)) {
            mainMenuActivity.unbindService(mainMenuActivity.getmConnection());
        }
        mainMenuActivity.basicTermiteSetup();
        // WiFi is always on - Battery drainage is cool, because people buy new phones
        Intent intent = new Intent(mainMenuActivity, SimWifiP2pService.class);
        mainMenuActivity.bindService(intent, mainMenuActivity.getmConnection(), Context.BIND_AUTO_CREATE);
        // Start Server in AsyncTask
        mainMenuActivity.setmWifiManager(WifiDirectManager.init(mainMenuActivity));
    }

    /**********************************************************
     * ADD PHOTO
     ***********************************************************/

    @Override
    public void addPhoto(final FragmentActivity activity, String catalogId, File file) throws FailedOperationException {
        // Reads the temp image's bytes.
        int fileLength = (int) file.length();
        byte[] fileContents = new byte[fileLength];

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            int bytesRead = inputStream.read(fileContents);
            if (bytesRead != fileLength) {
                throw new FailedOperationException("Couldn't read the image file.");
            }
            inputStream.close();
        }
        catch(IOException ioe){
            throw new FailedOperationException(ioe.getMessage());
        }
        finally {
            try { inputStream.close(); }
            catch (NullPointerException | IOException ex) { /* Nothing can be done. */ }
        }

        // Saves the temp image's bytes to internal storage in a permanent file.
        String username = SessionManager.getUsername(activity);
        String photoName = String.format("%s_%s_%s", catalogId, username, UUID.randomUUID().toString().replace("/", ""));
        try {
            ImageLoading.savePhoto(activity, photoName, fileContents);
        }
        catch (JSONException | IOException ex) {
            throw new FailedOperationException(ex.getMessage());
        }

        updateCatalogFile(activity, catalogId, SessionManager.getUsername(activity), photoName);
    }

    private void updateCatalogFile(final Activity activity,
                                   final String catalogID,
                                   final String owner,
                                   final String photoId) {

        // Retrieve catalog file contents as a JSON Object and compare them to the received catalog file
        try {
            JSONObject catalogFileContents = CatalogOperations.readCatalog(activity, catalogID);
            // Append photoId to the user photoId arrays under memberPhotos dictionary
            catalogFileContents.getJSONObject("membersPhotos").getJSONArray(owner).put(photoId);
            CatalogOperations.writeCatalog(activity, catalogID, catalogFileContents);
        }
        catch (IOException | JSONException exc) {
            LogManager.logError(LogManager.NEW_CATALOG_TAG, exc.getMessage());
            LogManager.toast(activity, "Failed to add photo to catalog");
        }
    }

    /**********************************************************
     * NEW CATALOG SLICE
     ***********************************************************/

    @Override
    public void newCatalogSlice(final Activity activity, String catalogID, String catalogTitle) {
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
            CatalogOperations.writeCatalog(activity, catalogID, catalogFile);
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
    public void viewCatalog(final Activity activity, final View view, String catalogID, String catalogTitle) {
        List<Bitmap> bitmaps = ImageLoading.getBitmapsFromFileStorage(activity, catalogID);
        ViewCatalogFragment.drawImages(view, activity, bitmaps);
        LogManager.logViewCatalog(catalogID, catalogTitle);
    }
}
