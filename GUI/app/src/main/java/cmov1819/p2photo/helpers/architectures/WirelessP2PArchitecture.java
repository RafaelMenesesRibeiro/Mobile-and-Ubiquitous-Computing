package cmov1819.p2photo.helpers.architectures;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.NewCatalogFragment;
import cmov1819.p2photo.ViewCatalogFragment;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.SessionManager;

public class WirelessP2PArchitecture extends BaseArchitecture {
    @Override
    public void handlePendingMemberships(Activity activity) {
        MainMenuActivity.handlePendingMembershipsWifiDirect(activity);
    }

    @Override
    public void setup(View view, LoginActivity loginActivity) {
        // Nothing to setup. //
    }

    // TODO - Can only be tested once UpdateCatalog and ViewCatalogWifiDirectArch are implemented. //
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

        // TODO - Update catalog file. //
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

    // TODO - @Francisco Barros //
    public void updateCatalog(Activity activity, String catalogID, String username, String imageName) {
        // TODO //
    }
}
