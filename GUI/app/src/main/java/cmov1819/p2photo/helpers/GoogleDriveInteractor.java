package cmov1819.p2photo.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cmov1819.p2photo.MainApplication;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressLint("StaticFieldLeak")
@SuppressWarnings("Duplicates")
public class GoogleDriveInteractor {
    public static final String APPLICATION_NAME = MainApplication.getApplicationName();

    public static final String GOOGLE_DRIVE_TAG = "DRIVE INTERACTOR";

    public static final String GOOGLE_API = "https://www.googleapis.com/";
    public static final String TYPE_GOOGLE_DRIVE_FILE = "application/vnd.google-apps.file";
    public static final String TYPE_GOOGLE_DRIVE_FOLDER = "application/vnd.google-apps.folder";
    public static final String TYPE_PHOTO = "application/vnd.google-apps.photo";
    public static final String TYPE_UNKNOWN = "application/vnd.google-apps.unknown";

    public static final String FILE_UPLOAD_ENDPOINT = GOOGLE_API + "drive/v2/files";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";

    public static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static GoogleDriveInteractor instance;
    private static Drive driveService;

    private GoogleDriveInteractor() {
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

    public static GoogleDriveInteractor getInstance() {
        if (instance == null)
            instance = new GoogleDriveInteractor();
        return instance;
    }

    public static void createFolder(final Context context, final String folderName, AuthState authState) {

        authState.performActionWithFreshTokens(new AuthorizationService(context), new AuthState.AuthStateAction() {
            @Override
            public void execute(@Nullable String accessToken, @Nullable String idToken,
                                @Nullable final AuthorizationException error) {

                new AsyncTask<String, Void, JSONObject>() {
                    @Override
                    protected JSONObject doInBackground(String... tokens) {
                        try {
                            OkHttpClient okHttpClient = new OkHttpClient();
                            RequestBody body = RequestBody.create(JSON_TYPE, newDirectory(folderName).toString());
                            Request request = new Request.Builder()
                                    .url(FILE_UPLOAD_ENDPOINT)
                                    .post(body)
                                    .addHeader(AUTHORIZATION_HEADER, "Bearer " + tokens[0])
                                    .build();
                            Response response = okHttpClient.newCall(request).execute();
                            String jsonBody = response.body().string();
                            Log.i(GOOGLE_DRIVE_TAG, "createFolder response: " + jsonBody);
                            return new JSONObject(jsonBody);
                        } catch (Exception exception) {
                            Log.e(GOOGLE_DRIVE_TAG, exception.getMessage());
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(JSONObject jsonResponse) {
                        if (jsonResponse != null && jsonResponse.has("error")) {
                            processErrorCodes(context, jsonResponse);
                        } else {
                            Log.i(GOOGLE_DRIVE_TAG, "Created folder with success");
                        }
                    }
                }.execute(accessToken);
            }
        });
    }

    private static void processErrorCodes(Context context, JSONObject jsonResponse) {
        try {
            String message = jsonResponse.getString("message");
            int code = jsonResponse.getInt("code");
            switch (code) {
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    Log.e(GOOGLE_DRIVE_TAG, "HTTP_UNAUTHORIZED: " + message);
                    AuthStateManager.getInstance(context).getAuthorization(context, message, true);
                    break;
                case HttpURLConnection.HTTP_FORBIDDEN:
                    Log.e(GOOGLE_DRIVE_TAG, "HTTP_FORBIDDEN: " + message);
                    if (message.startsWith("The user has not granted the app")) {
                        AuthStateManager.getInstance(context).getAuthorization(context, message, true);
                    }
                    break;
                default:
                    Log.e(GOOGLE_DRIVE_TAG, "UNEXPECTED ERROR WITH CODE " + code + ": " + message);
            }
        } catch (JSONException jsone) {
            Log.e(GOOGLE_DRIVE_TAG, "GoogleDriveInteractor#processResponseCodes accessed unexisting field");
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
            Log.e(GOOGLE_DRIVE_TAG, "Could not convert object to json using GoogleDriveInteractor#jsonify");
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
