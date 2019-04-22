package cmov1819.p2photo.helpers.mediators;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainApplication;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressLint("StaticFieldLeak")
public class GoogleDriveMediator {
    public static final String APPLICATION_NAME = MainApplication.getApplicationName();

    public static final String GOOGLE_DRIVE_TAG = "DRIVE INTERACTOR";

    public static final String GOOGLE_API = "https://www.googleapis.com/";
    public static final String TYPE_GOOGLE_DRIVE_FILE = "application/vnd.google-apps.file";
    public static final String TYPE_GOOGLE_DRIVE_FOLDER = "application/vnd.google-apps.folder";
    public static final String TYPE_GOOGLE_PHOTO = "application/vnd.google-apps.photo";
    public static final String TYPE_GOOGLE_UNKNOWN = "application/vnd.google-apps.unknown";

    public static final String TYPE_JSON = "application/json; charset=utf-8";
    public static final String TYPE_TXT = "text/plain";
    public static final String TYPE_JPEG = "image/jpeg";
    public static final String TYPE_PNG = "image/png";
    public static final String TYPE_BMP = "image/bmp";
    public static final String TYPE_GIF = "image/gif";
    public static final String TYPE_WEBP = "image/webp";

    public static final MediaType JSON_TYPE = MediaType.parse(TYPE_JSON);

    public static final String FILE_UPLOAD_ENDPOINT = GOOGLE_API + "drive/v2/files";
    public static final String MEDIA_FILE_UPLOAD_ENDPOINT = GOOGLE_API + "/upload/drive/v3/files?uploadType=media";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";


    private static GoogleDriveMediator instance;
    private static Credential credential;
    private static Drive driveService;

    private GoogleDriveMediator(String accessToken) {
        credential = new GoogleCredential().setAccessToken(accessToken);
        driveService = new Drive.Builder(
                new com.google.api.client.http.javanet.NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                credential).setApplicationName(APPLICATION_NAME).build();
    }

    public static GoogleDriveMediator getInstance(String accessToken) {
        if (instance == null)
            instance = new GoogleDriveMediator(accessToken);
        return instance;
    }

    public void newCatalog(final Context context, final String title, final AuthState authState) {
        authState.performActionWithFreshTokens(new AuthorizationService(context), new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, final AuthorizationException error) {
            new AsyncTask<String, Void, File>() {
                @Override
                protected File doInBackground(String... tokens) {
                    try {
                        if (error != null) {
                            suggestReauthentication(context, error.getMessage());
                        }
                        else {
                            credential.setAccessToken(tokens[0]);

                            File catalogFolderFile = createFolder(title, tokens[0]);

                            if (catalogFolderFile == null) {
                                setWarning(context,"Null response received from Google REST API.");
                            } else {
                                Log.i(GOOGLE_DRIVE_TAG, "Created folder with success");
                                String catalogFolderId = catalogFolderFile.getId();
                                JSONObject catalogJson = new JSONObject();
                                catalogJson.put("title", title);
                                catalogJson.put("p2photoId", "SOMETHING IDK WHAT FOR");
                                catalogJson.put("googleDriveId", catalogFolderId);
                                catalogJson.put("photos", new ArrayList<String>());

                                InputStream targetStream = new ByteArrayInputStream(
                                        catalogJson.toString(4).getBytes(Charset.forName("UTF-8"))
                                );
                                AbstractInputStreamContent catalogFile = new InputStreamContent(TYPE_JSON, targetStream);

                                return createFile(catalogFolderId, "catalog.json", catalogFile);
                            }
                        }
                    } catch (JSONException | IOException exc) {
                        setError(context, exc.getMessage());
                    }
                    return null;
                }
            }.execute(accessToken);
            }
        });
    }

    /**********************************************************
     * JSON OBJECT CONSTRUCTORS AND REQUESTS
     **********************************************************/

    private File createFolder(String folderName, String token) throws IOException {

        Log.i(GOOGLE_DRIVE_TAG, ">>> Initiating GoogleDriveMediator#createFolder request");
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        File file = driveService.files().create(fileMetadata)
                .setFields("id")
                .execute();

        Log.i(GOOGLE_DRIVE_TAG, "Folder ID: " + file.getId());

        return file;
    }

    private File createFile(String parentFolderId, String fileName, AbstractInputStreamContent fileContent) throws IOException {
        Log.i(GOOGLE_DRIVE_TAG, ">>> Initiating GoogleDriveMediator#createFile request");

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(parentFolderId));
        File file = driveService.files().create(fileMetadata, fileContent)
                .setFields("id, parents")
                .execute();

        Log.i(GOOGLE_DRIVE_TAG, "File ID: " + file.getId());
        return file;
    }

    /**********************************************************
     *  HELPERS
     **********************************************************/

    public void processErrorCodes(Context context, JSONObject jsonResponse) throws JSONException {
        String message = jsonResponse.getString("message");
        int code = jsonResponse.getInt("code");
        if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            suggestReauthentication(context, message);
        } else if (code == HttpURLConnection.HTTP_FORBIDDEN && message.startsWith("The user has not granted the app")) {
            suggestReauthentication(context, message);
        } else {
            setError(context,"Received unexpected error code from Google API.");
        }
    }

    private void setError(Context context, String message) {
        Log.e(GOOGLE_DRIVE_TAG, message);
    }

    private void setWarning(Context context, String message) {
        Log.w(GOOGLE_DRIVE_TAG, message);
    }

    private void suggestRetry(Context context, String message) {
        Log.w(GOOGLE_DRIVE_TAG, "Google Drive REST API timed out.");
    }

    private void suggestReauthentication(Context context, String message) {
        Log.w(GOOGLE_DRIVE_TAG, message);
        context.startActivity(new Intent(context, LoginActivity.class));
    }

    private static JSONObject newDirectory(String folderName) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", folderName);
        jsonObject.put("mimeType", TYPE_GOOGLE_DRIVE_FOLDER);
        return jsonObject;
    }

}
