package cmov1819.p2photo.helpers.architectures;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import java.io.File;

import cmov1819.p2photo.AddPhotosFragment;
import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.exceptions.FailedOperationException;
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

    @Override
    public void addPhoto(FragmentActivity activity, String catalogId, File androidFilePath) throws FailedOperationException {
        AddPhotosFragment.addPhotoCloudArch(activity, catalogId, androidFilePath);
    }

    @Override
    public void newCatalogSlice(Activity activity, String catalogID, String catalogTitle) {
        GoogleDriveMediator googleDriveMediator = getGoogleDriveMediator(activity);
        AuthStateManager authStateManager = getAuthStateManager(activity);
        googleDriveMediator.newCatalogSlice(
                activity,
                catalogTitle,
                catalogID,
                authStateManager.getAuthState()
        );
    }

    public GoogleDriveMediator getGoogleDriveMediator(Activity activity) {
        AuthStateManager authStateManager = AuthStateManager.getInstance(activity);
        return GoogleDriveMediator.getInstance(authStateManager.getAuthState().getAccessToken());
    }

    public AuthStateManager getAuthStateManager(Activity activity) {
        return AuthStateManager.getInstance(activity);
    }
}
