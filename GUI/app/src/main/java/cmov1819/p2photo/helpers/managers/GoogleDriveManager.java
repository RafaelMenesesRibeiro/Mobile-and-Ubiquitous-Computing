package cmov1819.p2photo.helpers.managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainApplication;
import cmov1819.p2photo.helpers.datastructures.DriveResultsData;
import cmov1819.p2photo.helpers.driveasynctasks.CreateFile;
import cmov1819.p2photo.helpers.driveasynctasks.CreateFolder;
import cmov1819.p2photo.helpers.mediators.P2PhotoGoogleDriveMediator;
import okhttp3.MediaType;

@SuppressLint("StaticFieldLeak")
public class GoogleDriveManager {
    public static final String APPLICATION_NAME = MainApplication.getApplicationName();

    public static final String GOOGLE_DRIVE_TAG = "DRIVE INTERACTOR";

    public static final String GOOGLE_API = "https://www.googleapis.com/";
    public static final String TYPE_GOOGLE_DRIVE_FILE = "application/vnd.google-apps.file";
    public static final String TYPE_GOOGLE_DRIVE_FOLDER = "application/vnd.google-apps.folder";
    public static final String TYPE_GOOGLE_PHOTO = "application/vnd.google-apps.photo";
    public static final String TYPE_GOOGLE_UNKNOWN = "application/vnd.google-apps.unknown";

    public static final String TYPE_JSON = "application/json";
    public static final String TYPE_TXT = "text/plain";
    public static final String TYPE_JPEG = "image/jpeg";
    public static final String TYPE_PNG = "image/png";
    public static final String TYPE_BMP = "image/bmp";
    public static final String TYPE_GIF = "image/gif";
    public static final String TYPE_WEBP = "image/webp";

    public static final String FILE_UPLOAD_ENDPOINT = GOOGLE_API + "drive/v2/files";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";

    public static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static GoogleDriveManager instance;
    private static Drive driveService;

    private GoogleDriveManager() {
        try {
            driveService = new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    null).setApplicationName(APPLICATION_NAME).build();
        } catch (GeneralSecurityException | IOException exc) {
            Log.e(GOOGLE_DRIVE_TAG, "Could not instanciate <GoogleNetHttpTransport>, aborting");
            System.exit(-1);
        }
    }

    public static GoogleDriveManager getInstance() {
        if (instance == null)
            instance = new GoogleDriveManager();
        return instance;
    }

    public void createFolder(final Context context,
                             final String folderName,
                             final Integer requestId,
                             final AuthState authState) {

        authState.performActionWithFreshTokens(new AuthorizationService(context), new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, final AuthorizationException error) {
                AsyncTask<String, Void, JSONObject> request = new CreateFolder(folderName, accessToken, idToken);
                try {
                    if (error != null) {
                        suggestReauthentication(context, requestId, error.getMessage());
                    } else {
                        request.execute(accessToken);
                        JSONObject response = request.get(10, TimeUnit.SECONDS);
                        if (response == null) {
                            setWarning(context, requestId, "Null response received from Google REST API.");
                        } else if (response.has("error")) {
                            processErrorCodes(context, requestId, response);
                        } else {
                            Log.i(GOOGLE_DRIVE_TAG, "Created folder with success");
                            P2PhotoGoogleDriveMediator.requestsMap.get(requestId).setFolderId(response.getString("id"));
                        }
                    }
                } catch (TimeoutException toe) {
                    if (request != null) request.cancel(true);
                    suggestRetry(context, requestId, toe.getMessage());
                } catch (InterruptedException | ExecutionException exc) {
                    setWarning(context, requestId, exc.getMessage());
                } catch (JSONException jsone) {
                    setError(context, requestId, jsone.getMessage());
                }
            }
        });
    }

    public void createFile(final Context context,
                           final Integer requestId,
                           final String fileName,
                           final String rootFolderId,
                           AuthState authState) {

        authState.performActionWithFreshTokens(new AuthorizationService(context), new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, final AuthorizationException error) {
                AsyncTask<String, Void, JSONObject> request = new CreateFile(fileName, rootFolderId, accessToken, idToken);
                try {
                    if (error != null) {
                        suggestReauthentication(context, requestId, error.getMessage());
                    } else {
                        request.execute(accessToken);
                        JSONObject response = request.get(10, TimeUnit.SECONDS);
                        if (response == null) {
                            setWarning(context, requestId, "Null response received from Google REST API.");
                        } else if (response.has("error")) {
                            processErrorCodes(context, requestId, response);
                        } else {
                            Log.i(GOOGLE_DRIVE_TAG, "Created file with success");
                            P2PhotoGoogleDriveMediator.requestsMap.get(requestId).setFileId(response.getString("id"));
                            P2PhotoGoogleDriveMediator.requestsMap.get(requestId).setFileUrl(response.getString("url"));
                        }
                    }
                } catch (TimeoutException toe) {
                    if (request != null) request.cancel(true);
                    suggestRetry(context, requestId, toe.getMessage());
                } catch (InterruptedException | ExecutionException exc) {
                    setWarning(context, requestId, exc.getMessage());
                } catch (JSONException jsone) {
                    setError(context, requestId, jsone.getMessage());
                }
            }
        });
    }

    /**********************************************************
     *  HELPERS
     **********************************************************/

    public void processErrorCodes(Context context, Integer requestId, JSONObject jsonResponse) throws JSONException {
        String message = jsonResponse.getString("message");
        int code = jsonResponse.getInt("code");
        if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            suggestReauthentication(context, requestId, message);
        } else if (code == HttpURLConnection.HTTP_FORBIDDEN && message.startsWith("The user has not granted the app")) {
            suggestReauthentication(context, requestId, message);
        } else {
            setError(context, requestId,"Received unexpected error code from Google API.");
        }
    }

    private void setError(Context context, Integer requestId, String message) {
        Log.e(GOOGLE_DRIVE_TAG, "createFolder or processErrorCodes accessed unexisting fields");
        DriveResultsData driveResultsData = P2PhotoGoogleDriveMediator.requestsMap.get(requestId);
        driveResultsData.setHasError(true);
        driveResultsData.setMessage(message);
        driveResultsData.setSuggestRetry(false);
    }

    private void setWarning(Context context, Integer requestId, String message) {
        Log.w(GOOGLE_DRIVE_TAG, message);
        DriveResultsData driveResultsData = P2PhotoGoogleDriveMediator.requestsMap.get(requestId);
        driveResultsData.setHasError(true);
        driveResultsData.setMessage(message);
        driveResultsData.setSuggestRetry(false);
    }

    private void suggestRetry(Context context, Integer requestId, String message) {
        Log.w(GOOGLE_DRIVE_TAG, "Google Drive REST API timed out");
        DriveResultsData driveResultsData = P2PhotoGoogleDriveMediator.requestsMap.get(requestId);
        driveResultsData.setHasError(true);
        driveResultsData.setMessage(message);
        driveResultsData.setSuggestRetry(true);
    }

    private void suggestReauthentication(Context context, Integer requestId, String message) {
        Log.w(GOOGLE_DRIVE_TAG, message);
        DriveResultsData driveResultsData = P2PhotoGoogleDriveMediator.requestsMap.get(requestId);
        driveResultsData.setHasError(true);
        driveResultsData.setMessage(message);
        driveResultsData.setSuggestedIntent(new Intent(context, LoginActivity.class));
    }

    private static JSONObject newDirectory(String folderName) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", folderName);
        jsonObject.put("mimeType", TYPE_GOOGLE_DRIVE_FOLDER);
        return jsonObject;
    }

}
