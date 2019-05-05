package cmov1819.p2photo.helpers.architectures;

import android.app.Activity;
import android.view.View;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.mediators.GoogleDriveMediator;

public class CloudBackedArchitecture extends BaseArchitecture {
    @Override
    public void handlePendingMemberships(Activity activity) {
        MainMenuActivity.handlePendingMembershipsCloudArch(activity);
    }

    @Override
    public void setup(View view, LoginActivity loginActivity) {
        LoginActivity.tryEnablingPostAuthorizationFlows(view, loginActivity);
    }

    public GoogleDriveMediator getGoogleDriveMediator(Activity activity) {
        AuthStateManager authStateManager = AuthStateManager.getInstance(activity);
        return GoogleDriveMediator.getInstance(authStateManager.getAuthState().getAccessToken());
    }

    public AuthStateManager getAuthStateManager(Activity activity) {
        return AuthStateManager.getInstance(activity);
    }
}
