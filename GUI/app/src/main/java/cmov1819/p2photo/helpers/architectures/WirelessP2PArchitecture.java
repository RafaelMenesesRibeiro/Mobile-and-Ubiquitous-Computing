package cmov1819.p2photo.helpers.architectures;

import android.app.Activity;
import android.view.View;

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
}
