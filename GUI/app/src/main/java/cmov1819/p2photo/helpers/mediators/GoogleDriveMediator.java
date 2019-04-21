package cmov1819.p2photo.helpers.mediators;

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

    private static GoogleDriveMediator instance;
    private static Drive driveService;

    private GoogleDriveMediator() {
        try {
            driveService = new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    null).setApplicationName(APPLICATION_NAME).build();
        } catch (GeneralSecurityException | IOException exc) {
            Log.e(GOOGLE_DRIVE_TAG, exc.getCause().toString());
        }
    }

    public static GoogleDriveMediator getInstance() {
        if (instance == null)
            instance = new GoogleDriveMediator();
        return instance;
    }

    public void newCatalog(final Context context, final String catalogTitle, final AuthState authState) {
        authState.performActionWithFreshTokens(new AuthorizationService(context), new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, final AuthorizationException error) {
                new AsyncTask<String, Void, JSONObject>() {
                    @Override
                    protected JSONObject doInBackground(String... tokens) {
                        try {
                            if (error != null) {
                                suggestReauthentication(context, error.getMessage());
                            }
                            else {
                                JSONObject createFolderResult = createFolder(catalogTitle, tokens[0]);
                                if (createFolderResult == null) {
                                    setWarning(context,"Null response received from Google REST API.");
                                }
                                else if (createFolderResult.has("error")) {
                                    processErrorCodes(context, createFolderResult);
                                }
                                else {
                                    String parentFolderId = createFolderResult.getString("id");
                                    Log.i(GOOGLE_DRIVE_TAG, String.format("Created %s folder with success", parentFolderId));
                                    return createFile(parentFolderId, "catalog.json", tokens[0]);
                                }
                            }
                        } catch (JSONException | IOException jsone) {
                            setError(context,  jsone.getMessage());
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(JSONObject createFileResult) {
                        try {
                            if (createFileResult == null) {
                                setWarning(context,"Null response received from Google REST API.");
                            }
                            else if (createFileResult.has("error")) {
                                processErrorCodes(context, createFileResult);
                            }
                            else {
                                String fileId = createFileResult.getString("id");
                                Log.i(GOOGLE_DRIVE_TAG, String.format("Created %s file with success", fileId));
                            }
                        } catch (JSONException jsone) {
                            setError(context, jsone.getMessage());
                        }
                    }
                }.execute(accessToken);
            }
        });
    }

    /**********************************************************
     * JSON OBJECT CONSTRUCTORS AND REQUESTS
     **********************************************************/

    private JSONObject createFolder(String folderName, String token) throws IOException, JSONException {
        // CREATE FOLDER REQUEST
        Log.i(GOOGLE_DRIVE_TAG, ">>> Initiating GoogleDriveMediator#createFolder request");
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON_TYPE, newDirectory(folderName).toString());
        Request createFolderRequest = new Request.Builder()
                .url(FILE_UPLOAD_ENDPOINT)
                .post(body)
                .addHeader(AUTHORIZATION_HEADER, "Bearer " + token)
                .build();
        Response createFolderResponse = okHttpClient.newCall(createFolderRequest).execute();
        String createFolderResponseString = createFolderResponse.body().string();
        return new JSONObject(createFolderResponseString);
    }

    private JSONObject createFile(String parentFolderId, String fileName, String token) throws IOException, JSONException {
        Log.i(GOOGLE_DRIVE_TAG, ">>> Initiating GoogleDriveMediator#createFile request");
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON_TYPE, newFile(fileName, parentFolderId).toString());
        Request request = new Request.Builder()
                .url(FILE_UPLOAD_ENDPOINT)
                .post(body)
                .addHeader(AUTHORIZATION_HEADER, "Bearer " + token)
                .build();
        Response response = okHttpClient.newCall(request).execute();;
        String jsonBody = response.body().string();
        Log.i(GOOGLE_DRIVE_TAG, "createFile response: " + jsonBody);
        return new JSONObject(jsonBody);
    }

    private JSONObject newDirectory(String folderName, String parentFolder) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", folderName);
        jsonObject.put("mimeType", TYPE_GOOGLE_DRIVE_FOLDER);

        if (!parentFolder.equals("root")) {
            jsonObject.put("parents", String.format("[{ \"id\" : \"%s\" }]", parentFolder));
        }

        return jsonObject;
    }

    private JSONObject newFile(String fileName, String parentFolder) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", fileName);
        jsonObject.put("mimeType", TYPE_TXT);

        if (!parentFolder.equals("root")) {
            jsonObject.put("parents", String.format("[{ \"id\" : \"%s\" }]", parentFolder));
        }

        return jsonObject;
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
