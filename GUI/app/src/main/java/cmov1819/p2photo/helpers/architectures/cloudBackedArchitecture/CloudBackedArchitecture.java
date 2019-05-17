package cmov1819.p2photo.helpers.architectures.cloudBackedArchitecture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.R;
import cmov1819.p2photo.dataobjects.PutRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.architectures.BaseArchitecture;
import cmov1819.p2photo.helpers.managers.ArchitectureManager;
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.mediators.P2PWebServerMediator;
import cmov1819.p2photo.helpers.mediators.GoogleDriveMediator;
import cmov1819.p2photo.msgtypes.ErrorResponse;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_CATALOG;
import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_GOOGLE_IDENTIFIERS;
import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

public class CloudBackedArchitecture extends BaseArchitecture {
    @Override
    public void handlePendingMemberships(final Activity activity) {
        MainMenuActivity.handlePendingMembershipsCloudArch(activity);
    }

    @Override
    public void onSignUp(final LoginActivity loginActivity) {
        /* Nothing to do.  */
    }

    @Override
    public void setup(final View view, final LoginActivity loginActivity) {
        LoginActivity.tryEnablingPostAuthorizationFlows(view, loginActivity);
    }

    @Override
    public void setupHome(MainMenuActivity mainMenuActivity) {
        /* No setup required */
    }

    /**********************************************************
     * ADD PHOTO
     ***********************************************************/

    @Override
    public void addPhoto(final FragmentActivity activity, String catalogId, File androidFilePath) throws FailedOperationException {
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

    private static HashMap<String, String> getGoogleDriveIdentifiers(final Activity activity, String catalogId)
            throws FailedOperationException {
        try {
            String baseUrl = activity.getString(R.string.p2photo_host) + activity.getString(R.string.get_google_identifiers);

            String url = String.format(
                    "%s?calleeUsername=%s&catalogId=%s", baseUrl, getUsername(activity) , catalogId
            );

            RequestData requestData = new RequestData(activity, GET_GOOGLE_IDENTIFIERS, url);
            ResponseData result = new P2PWebServerMediator().execute(requestData).get();

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

    /**********************************************************
     * NEW CATALOG SLICE
     ***********************************************************/

    @Override
    public void newCatalogSlice(final Activity activity, String catalogID, String catalogTitle) {
        GoogleDriveMediator googleDriveMediator = getGoogleDriveMediator(activity);
        AuthStateManager authStateManager = getAuthStateManager(activity);
        googleDriveMediator.newCatalogSlice(
                activity,
                catalogTitle,
                catalogID,
                authStateManager.getAuthState()
        );
    }

    public static void createCatalogSlice(final Context context,
                                          final String catalogId,
                                          final String parentFolderGoogleId,
                                          final String catalogFileGoogleId,
                                          final String webContentLink) {
        try {
            JSONObject requestBody = new JSONObject();

            requestBody.put("parentFolderGoogleId", parentFolderGoogleId);
            requestBody.put("catalogFileGoogleId", catalogFileGoogleId);
            requestBody.put("webContentLink", webContentLink);
            requestBody.put("calleeUsername", getUsername((Activity)context));

            String url =
                    context.getString(R.string.p2photo_host) + context.getString(R.string.new_catalog_slice) + catalogId;

            RequestData requestData = new PutRequestData(
                    (Activity)context, RequestData.RequestType.NEW_CATALOG_SLICE, url, requestBody
            );

            ResponseData result = new P2PWebServerMediator().execute(requestData).get();

            int code = result.getServerCode();

            if (code != HttpURLConnection.HTTP_OK) {
                String reason = ((ErrorResponse) result.getPayload()).getReason();
                if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    LogManager.logError(LogManager.NEW_CATALOG_TAG, reason);
                    LogManager.toast(((Activity) context), "Session timed out, please login again");
                    context.startActivity(new Intent(context, LoginActivity.class));
                }
                else {
                    LogManager.logError(LogManager.NEW_CATALOG_TAG, reason);
                    LogManager.toast(((Activity) context), "Something went wrong");
                }
            }

        }
        catch (JSONException ex) {
            String msg = "JSONException: " + ex.getMessage();
            LogManager.logError(LogManager.NEW_CATALOG_TAG, msg);
            throw new FailedOperationException(ex.getMessage());
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            String msg = "New Catalog unsuccessful. " + ex.getMessage();
            LogManager.logError(LogManager.NEW_CATALOG_TAG, msg);
            throw new FailedOperationException(ex.getMessage());
        }
    }

    /**********************************************************
     * VIEW CATALOG
     ***********************************************************/

    @Override
    public void viewCatalog(final Activity activity, final View view, String catalogID, String catalogTitle) {
        List<String> googleSliceFileIdentifiersList = getGoogleSliceFileIdentifiersList(activity, catalogID);
        GoogleDriveMediator googleDriveMediator = ((CloudBackedArchitecture) ArchitectureManager.systemArchitecture).getGoogleDriveMediator(activity);
        AuthStateManager authStateManager = ((CloudBackedArchitecture) ArchitectureManager.systemArchitecture).getAuthStateManager(activity);

        for (String googleCatalogFileId : googleSliceFileIdentifiersList) {
            googleDriveMediator.viewCatalogSlicePhotos(activity, view, googleCatalogFileId, authStateManager.getAuthState()
            );
        }
        LogManager.logViewCatalog(catalogID, catalogTitle);
    }

    private static List<String> getGoogleSliceFileIdentifiersList(final Activity activity, String catalogID) {
        String url = activity.getString(R.string.p2photo_host) + activity.getString(R.string.view_catalog) +
                "?calleeUsername=" + getUsername(activity) + "&catalogId=" + catalogID;

        try {
            RequestData requestData = new RequestData(activity, GET_CATALOG, url);
            ResponseData responseData = new P2PWebServerMediator().execute(requestData).get();
            if (responseData.getServerCode() == HttpURLConnection.HTTP_OK) {
                SuccessResponse payload = (SuccessResponse) responseData.getPayload();
                return (List<String> ) payload.getResult();
            }
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            LogManager.logError(LogManager.VIEW_CATALOG_TAG, ex.getMessage());
        }
        return new ArrayList<>();
    }

    /**********************************************************
     * GOOGLE DRIVE HELPERS
     ***********************************************************/

    public GoogleDriveMediator getGoogleDriveMediator(final Activity activity) {
        AuthStateManager authStateManager = AuthStateManager.getInstance(activity);
        return GoogleDriveMediator.getInstance(authStateManager.getAuthState().getAccessToken());
    }

    public AuthStateManager getAuthStateManager(final Activity activity) {
        return AuthStateManager.getInstance(activity);
    }


}
