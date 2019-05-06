package cmov1819.p2photo.helpers.architectures;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import java.io.File;

import cmov1819.p2photo.AddPhotosFragment;
import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainMenuActivity;

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
}
