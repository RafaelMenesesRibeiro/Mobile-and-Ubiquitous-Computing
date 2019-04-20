package cmov1819.p2photo.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.common.collect.ImmutableSet;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;

import cmov1819.p2photo.MainApplication;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.google.api.services.drive.DriveScopes.*;

@SuppressLint("StaticFieldLeak")
@SuppressWarnings("Duplicates")
public class GoogleDriveInteractor {
    private static final String GOOGLE_DRIVE_TAG = "DRIVE INTERACTOR";

    private static final String GOOGLE_API = "https://www.googleapis.com/";

    private static final String SIMPLE_UPLOAD = GOOGLE_API + "upload/drive/v3/files?uploadType=media";
    private static final String MULTIPART_UPLOAD = GOOGLE_API + "upload/drive/v3/files?uploadType=multipart";
    private static final String PART_SEPARATOR = "--part";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";

    private static final String FOLDER_TYPE = "application/vnd.google-apps.folder";
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static GoogleDriveInteractor instance;
    private static DriveServiceHelper driveServiceHelper;
    private static Drive googleDriveService;
    private static HttpTransport httpTransport;

    private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private GoogleDriveInteractor(Context context) {
            httpTransport = AndroidHttp.newCompatibleTransport();

            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    ImmutableSet.of(DRIVE_FILE, DRIVE_APPDATA, DRIVE_PHOTOS_READONLY, DRIVE_READONLY)
            );

            Drive googleDriveService = new Drive.Builder(httpTransport, new GsonFactory(), credential)
                    .setApplicationName(MainApplication.getApplicationName())
                    .build();
            GoogleDriveInteractor.driveServiceHelper = new DriveServiceHelper(googleDriveService);
    }

    public static GoogleDriveInteractor getInstance(Context context) {
        if (instance == null) { instance = new GoogleDriveInteractor(context); }
        return instance;
    }


    public void mkdirWithFreshTokens(final Context context, final String folderName, final String folderId,
                                            AuthorizationService authorizationService, AuthState authState) {

        driveServiceHelper.createFolder(folderName, folderId).addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
            @Override
            public void onSuccess(GoogleDriveFileHolder fileId) {
                driveServiceHelper.readFile(fileId.getId());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e(GOOGLE_DRIVE_TAG, "Couldn't create file.", exception);
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

    private static File newDirectory(String folderId, String folderName) {
        File fileMetadata = new File();
        fileMetadata.setId(folderId); // catalog id
        fileMetadata.setName(folderName); // catalog name
        fileMetadata.setMimeType(FOLDER_TYPE); // folder type
        return fileMetadata;
    }
}
