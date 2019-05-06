package cmov1819.p2photo.helpers.architectures;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import java.io.File;

import cmov1819.p2photo.AddPhotosFragment;
import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.NewCatalogFragment;
import cmov1819.p2photo.ViewCatalogFragment;

public class WirelessP2PArchitecture extends BaseArchitecture {
    @Override
    public void handlePendingMemberships(Activity activity) {
        MainMenuActivity.handlePendingMembershipsWifiDirect(activity);
    }

    @Override
    public void setup(View view, LoginActivity loginActivity) {
        // Nothing to setup. //
    }

    @Override
    public void addPhoto(FragmentActivity activity, String catalogId, File androidFilePath) {
        AddPhotosFragment.addPhotoWifiDirectArch(activity, catalogId, androidFilePath);
    }

    @Override
    public void newCatalogSlice(Activity activity, String catalogID, String catalogTitle) {
        NewCatalogFragment.newCatalogSliceWifiDirectArch(activity, catalogID, catalogTitle);
    }

    @Override
    public void viewCatalog(Activity activity, View view, String catalogID, String catalogTitle) {
        ViewCatalogFragment.populateGridWifiDirectArch(activity, view, catalogID, catalogTitle);
    }
}
