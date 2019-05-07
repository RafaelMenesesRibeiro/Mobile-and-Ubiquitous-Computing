package cmov1819.p2photo.helpers.architectures;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.AddPhotosFragment;
import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.R;
import cmov1819.p2photo.ViewCatalogFragment;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.managers.ArchitectureManager;
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.helpers.mediators.GoogleDriveMediator;
import cmov1819.p2photo.msgtypes.ErrorResponse;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_GOOGLE_IDENTIFIERS;
import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

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
        HashMap<String, String> googleDriveIdentifiers = getGoogleDriveIdentifiers(activity, catalogId);

        if (googleDriveIdentifiers == null) {
            String msg = "Failed to obtain googleDriveIdentifiers. Found ErrorResponse";
            LogManager.logError(LogManager.ADD_PHOTO_TAG, msg);
            LogManager.toast(activity, "Failed to add photo");
            throw new FailedOperationException();
        }

        Map.Entry<String, String> onlyEntry = googleDriveIdentifiers.entrySet().iterator().next();
        GoogleDriveMediator googleDriveMediator = ((CloudBackedArchitecture) ArchitectureManager.systemArchitecture).getGoogleDriveMediator(activity);
        AuthStateManager authStateManager = ((CloudBackedArchitecture) ArchitectureManager.systemArchitecture).getAuthStateManager(activity);
        googleDriveMediator.newPhoto(
                activity,
                onlyEntry.getKey(),
                onlyEntry.getValue(),
                androidFilePath.getName(),
                GoogleDriveMediator.TYPE_PNG,
                androidFilePath,
                authStateManager.getAuthState()
        );
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

    @Override
    public void viewCatalog(Activity activity, View view, String catalogID, String catalogTitle) {
        ViewCatalogFragment.populateGridCloudArch(activity, view, catalogID, catalogTitle);
    }

    public GoogleDriveMediator getGoogleDriveMediator(Activity activity) {
        AuthStateManager authStateManager = AuthStateManager.getInstance(activity);
        return GoogleDriveMediator.getInstance(authStateManager.getAuthState().getAccessToken());
    }

    public AuthStateManager getAuthStateManager(Activity activity) {
        return AuthStateManager.getInstance(activity);
    }

    private static HashMap<String, String> getGoogleDriveIdentifiers(Activity activity, String catalogId) throws FailedOperationException {
        try {
            String baseUrl = activity.getString(R.string.p2photo_host) + activity.getString(R.string.get_google_identifiers);

            String url = String.format(
                    "%s?calleeUsername=%s&catalogId=%s", baseUrl, getUsername(activity) , catalogId
            );

            RequestData requestData = new RequestData(activity, GET_GOOGLE_IDENTIFIERS, url);
            ResponseData result = new QueryManager().execute(requestData).get();

            int code = result.getServerCode();
            if (code != HttpURLConnection.HTTP_OK) {
                String reason = ((ErrorResponse) result.getPayload()).getReason();
                if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    LogManager.logError(LogManager.ADD_PHOTO_TAG, reason);
                    LogManager.toast(activity, "Session timed out, please login again");
                    activity.startActivity(new Intent(activity, LoginActivity.class));
                    return null;
                }
                else {
                    LogManager.logError(LogManager.ADD_PHOTO_TAG, reason);
                    LogManager.toast(activity, "Something went wrong");
                    return null;
                }
            }
            else {
                Object resultObject = ((SuccessResponse)result.getPayload()).getResult();
                return (HashMap<String, String>) resultObject;
            }
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            String msg = "Operation unsuccessful. " + ex.getMessage();
            LogManager.logError(LogManager.ADD_PHOTO_TAG, msg);
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
