package cmov1819.p2photo.helpers.mediators;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainApplication;
import okhttp3.MediaType;

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

    public void newCatalog(final Context context,
                           final String title,
                           final String p2photoId,
                           final AuthState authState) {

        authState.performActionWithFreshTokens(new AuthorizationService(context), new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, final AuthorizationException error) {
                new AsyncTask<String, Void, File>() {
                    @Override
                    protected File doInBackground(String... tokens) {
                        try {
                            if (error != null) {
                                suggestReauthentication(context, error.getMessage());
                                return null;
                            }
                            else {
                                credential.setAccessToken(tokens[0]);

                                File catalogFolderFile = createFolder(title, tokens[0]);

                                if (catalogFolderFile == null) {
                                    setWarning(context,"Null response received from Google REST API.");
                                    return null;
                                }

                                String catalogFolderId = catalogFolderFile.getId();
                                String catalogJsonContent = newCatalogJsonFile(title, p2photoId, catalogFolderId);

                                return createTextFile(catalogFolderId,"catalog.json", catalogJsonContent);
                            }
                        } catch (JSONException | IOException exc) {
                            setError(context, exc.getMessage());
                            return null;
                        }
                    }
                }.execute(accessToken);
            }
        });
    }

    /**********************************************************
     * JSON OBJECT CONSTRUCTORS AND REQUESTS
     **********************************************************/

    private File createFolder(String parentId,
                              String folderName) throws IOException {

        Log.i(GOOGLE_DRIVE_TAG, ">>> Creating folder...");

        File fileMetaData = new File()
                .setParents(buildParentsList(parentId))
                .setName(folderName)
                .setMimeType(TYPE_GOOGLE_DRIVE_FOLDER);

        File googleFile = driveService.files()
                .create(fileMetaData)
                .setFields("id")
                .execute();

        return googleFile;
    }

    private File createTextFile(String parentId,
                                String fileName,
                                String fileContent) throws IOException {

        Log.i(GOOGLE_DRIVE_TAG, ">>> Creating text file...");

        File metadata = new File()
                .setParents(buildParentsList(parentId))
                .setName(fileName)
                .setMimeType(TYPE_TXT);

        //InputStream targetStream = new ByteArrayInputStream(fileContent.getBytes(Charset.forName("UTF-8")));
        //AbstractInputStreamContent fileContentStream = new InputStreamContent(TYPE_JSON, targetStream);

        ByteArrayContent contentStream = ByteArrayContent.fromString(TYPE_JSON, fileContent);

        File googleFile = driveService.files()
                .create(metadata, contentStream)
                .setFields("id, parents")
                .execute();

        return googleFile;
    }

    private File createImgFile(String parentId,
                               String filePath,
                               String fileName,
                               String mimeType) throws IOException {

        Log.i(GOOGLE_DRIVE_TAG, ">>> Creating image file with content type: " + mimeType + "...");

        File metadata = new File()
                .setParents(buildParentsList(parentId))
                .setName(fileName)
                .setMimeType(mimeType);

        FileContent mediaContent = new FileContent(mimeType, new java.io.File(filePath));

        File googleFile = driveService.files()
                .create(metadata, mediaContent)
                .setFields("id, parents")
                .execute();

        return googleFile;
    }

    /**********************************************************
     *  HELPERS
     **********************************************************/

    private List<String> buildParentsList(String parents) {
        if (parents == null) {
            return Collections.singletonList("root");
        } else {
            return Collections.singletonList(parents);
        }
    }

    private String newCatalogJsonFile(String title, String p2photoId, String catalogFolderId) throws JSONException {
        JSONObject catalogJson = new JSONObject();
        catalogJson.put("title", title);
        catalogJson.put("p2photoId", p2photoId);
        catalogJson.put("googleDriveId", catalogFolderId);
        catalogJson.put("photos", new ArrayList<String>());
        return catalogJson.toString(4);
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

}
