package cmov1819.p2photo.helpers.managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import cmov1819.p2photo.MainApplication;
import cmov1819.p2photo.helpers.driveasynctasks.CreateFile;
import cmov1819.p2photo.helpers.driveasynctasks.CreateFolder;
import okhttp3.MediaType;

@SuppressLint("StaticFieldLeak")
@SuppressWarnings("Duplicates")
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

    public void createFolder(final Context context, final String folderName, AuthState authState) {

        authState.performActionWithFreshTokens(new AuthorizationService(context), new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, final AuthorizationException error) {
                AsyncTask<String, Void, JSONObject> request = new CreateFolder(folderName, accessToken, idToken);
                try {
                    if (error != null) {
                        Log.w(GOOGLE_DRIVE_TAG, "negotiation for fresh tokens failed, check ex for more details");
                    }
                    else {
                        request.execute(accessToken);
                        JSONObject response = request.get(10, TimeUnit.SECONDS);
                        String folderId = CreateFolder.processResponse(context, response);
                    }
                } catch (InterruptedException | TimeoutException | ExecutionException exc) {
                    if (request != null)
                        request.cancel(true);
                    Log.e(GOOGLE_DRIVE_TAG, "API Calling threads timed out or were interrutped.");
                } catch (JSONException jsone) {
                    Log.e(GOOGLE_DRIVE_TAG, "createFolder or processErrorCodes accessed unexisting fields");
                }
            }
        });
    }

    public void createFile(final Context context, final String fileName, final String rootFolderId, AuthState authState) {

        authState.performActionWithFreshTokens(new AuthorizationService(context), new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, final AuthorizationException error) {
                AsyncTask<String, Void, JSONObject> request = new CreateFile(fileName, rootFolderId, accessToken, idToken);
                try {
                    if (error != null) {
                        Log.w(GOOGLE_DRIVE_TAG, "negotiation for fresh tokens failed, check ex for more details");
                    }
                    else {
                        request.execute(accessToken);
                        JSONObject response = request.get(10, TimeUnit.SECONDS);
                        String folderId = CreateFolder.processResponse(context, response);
                    }
                } catch (InterruptedException | TimeoutException | ExecutionException exc) {
                    if (request != null)
                        request.cancel(true);
                    Log.e(GOOGLE_DRIVE_TAG, "API Calling threads timed out or were interrutped.");
                } catch (JSONException jsone) {
                    Log.e(GOOGLE_DRIVE_TAG, "createFile or processErrorCodes accessed unexisting fields");
                }
            }
        });
    }

    public static void processErrorCodes(Context context, JSONObject jsonResponse) throws JSONException {
        String message = jsonResponse.getString("message");
        int code = jsonResponse.getInt("code");
        switch (code) {
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                AuthStateManager.getInstance(context).getAuthorization(context, message, true);
                break;
            case HttpURLConnection.HTTP_FORBIDDEN:
                if (message.startsWith("The user has not granted the app")) {
                    AuthStateManager.getInstance(context).getAuthorization(context, message, true);
                }
                break;
            default:
                Log.e(GOOGLE_DRIVE_TAG, "UNEXPECTED ERROR WITH CODE " + code + ": " + message);
        }
    }

    /**********************************************************
     *  HELPERS
     **********************************************************/

    private static String jsonify(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            Log.e(GOOGLE_DRIVE_TAG, "Could not convert object to json using GoogleDriveManager#jsonify");
            return "{}";
        }
    }

    private static JSONObject newDirectory(String folderName) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", folderName);
        jsonObject.put("mimeType", TYPE_GOOGLE_DRIVE_FOLDER);
        return jsonObject;
    }

}
