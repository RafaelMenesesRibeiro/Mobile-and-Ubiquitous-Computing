package cmov1819.p2photo.helpers.mediators;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.util.IOUtils;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainApplication;
import cmov1819.p2photo.NewCatalogFragment;
import cmov1819.p2photo.ViewCatalogFragment;
import okhttp3.MediaType;

@SuppressLint("StaticFieldLeak")
public class GoogleDriveMediator {
    public static final String APPLICATION_NAME = MainApplication.getApplicationName();

    public static final String GOOGLE_DRIVE_TAG = "DRIVE MEDIATOR";

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
                new AsyncTask<String, Void, Pair<File, File>>() {
                    @Override
                    protected Pair<File, File> doInBackground(String... tokens) {
                        try {
                            if (error != null) {
                                suggestReauthentication(context, error.getMessage());
                                return null;
                            }
                            else {
                                credential.setAccessToken(tokens[0]);

                                File parentFolderFile = createFolder(null, title);

                                if (parentFolderFile == null) {
                                    setWarning(context,"Null catalog folder file received from Google REST API.");
                                    return null;
                                }

                                Log.i(GOOGLE_DRIVE_TAG, ">>> Creating folder... ID = " + parentFolderFile.getId());

                                String catalogFolderId = parentFolderFile.getId();
                                String catalogJsonContent = newCatalogJsonFile(title, p2photoId, catalogFolderId);
                                File catalogJsonFile = createJsonFile(catalogFolderId,"catalog", catalogJsonContent);

                                if (catalogJsonFile == null) {
                                    setWarning(context,"Null catalog.json file received from Google REST API.");
                                }

                                Log.i(GOOGLE_DRIVE_TAG, ">>> Creating catalog... ID = " + catalogJsonFile.getId());

                                Permission userPermission = new Permission()
                                        .setAllowFileDiscovery(true)
                                        .setType("anyone")
                                        .setRole("reader")
                                        .set("shared", true);
                                //.set("viewersCanCopyContent", true);

                                driveService.permissions()
                                        .create(catalogJsonFile.getId(), userPermission)
                                        .setFields("id")
                                        .execute();

                                catalogJsonFile = driveService.files()
                                        .get(catalogJsonFile.getId())
                                        .setFields("id, parents, webContentLink")
                                        .execute();

                                return new Pair<>(parentFolderFile, catalogJsonFile);
                            }
                        } catch (JSONException | IOException exc) {
                            setError(context, exc.getMessage());
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(Pair<File, File> files) {
                        File parentFolderFile = files.first;
                        File catalogJsonFile = files.second;
                        if (catalogJsonFile == null) {
                            Toast.makeText(context, "Couldn't create catalog", Toast.LENGTH_LONG).show();
                        }
                        else {
                            NewCatalogFragment.newCatalogSlice(
                                    context,
                                    p2photoId,
                                    parentFolderFile.getId(),
                                    catalogJsonFile.getId(),
                                    catalogJsonFile.getWebContentLink()
                            );
                            Toast.makeText(context, "Catalog created", Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute(accessToken);
            }
        });
    }

    public void newPhoto(final Context context,
                         final String parentFolderGoogleId,
                         final String catalogFileGoogleId,
                         final String photoName,
                         final String mimeType,
                         final java.io.File androidFilePath,
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

                                File newGooglePhotoFile = createImgFile(
                                        parentFolderGoogleId, photoName, mimeType, androidFilePath
                                );

                                if (newGooglePhotoFile == null) {
                                    setWarning(context,"Null response received from Google REST API.");
                                    return null;
                                } else {
                                    JSONObject currentCatalog = new JSONObject(readTxtFileContentsWithId(catalogFileGoogleId));
                                    JSONArray photos = currentCatalog.getJSONArray("photos");
                                    photos.put(newGooglePhotoFile.getWebContentLink());
                                    currentCatalog.put("photos", photos);
                                    String newFileContent = currentCatalog.toString(4);
                                    File googleFile = updateJsonFile(catalogFileGoogleId, photoName, newFileContent);
                                    return googleFile;
                                }
                            }
                        } catch (JSONException | IOException exc) {
                            setError(context, exc.getMessage());
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(File file) {
                        if (file == null) {
                            Toast.makeText(context, "Couldn't upload photo", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, "Upload complete", Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute(accessToken);
            }
        });
    }

    public void viewCatalogSlicePhotos(final Context context,
                                       final View view,
                                       final String webContentLink,
                                       final AuthState authState) {

        authState.performActionWithFreshTokens(new AuthorizationService(context), new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, final AuthorizationException error) {
                new AsyncTask<String, Void, ArrayList<Bitmap> >() {
                    @Override
                    protected ArrayList<Bitmap>  doInBackground(String... tokens) {
                        try {
                            if (error != null) {
                                suggestReauthentication(context, error.getMessage());
                                return null;
                            }

                            credential.setAccessToken(tokens[0]);
                            // Retrieve the metadata as a File object.
                            String readContents = readTxtFileWithWebContentLink(webContentLink);

                            if (readContents == null || readContents.equals("")) {
                                setWarning(context, "catalog file not found or malformed, null readTxtFileContentsWithId");
                                return null;
                            }

                            JSONObject catalogFileContents = new JSONObject(readContents);

                            if (catalogFileContents.has("photos")) {
                                ArrayList<Bitmap> displayablePhotosList = new ArrayList<>();
                                JSONArray photosArray = catalogFileContents.getJSONArray("photos");
                                if (photosArray != null) {
                                    int photoCount = photosArray.length();
                                    for (int photoIdx=0; photoIdx < photoCount; photoIdx++){
                                        String webContentLink = photosArray.getString(photoIdx);
                                        displayablePhotosList.add(
                                                readImgFileWithWebContentLink(context, webContentLink)
                                        );
                                    }
                                }
                                return displayablePhotosList;
                            }
                        } catch (IOException | JSONException exc) {
                            setError(context, exc.getMessage());
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(ArrayList<Bitmap>  photosFileIdList) {
                        if (photosFileIdList == null) {
                            setError(context, "catalog slice does not have a photos field");
                        } else {
                            Bitmap[] displayablePhotosArray = photosFileIdList.toArray(new Bitmap[photosFileIdList.size()]);
                            ViewCatalogFragment.drawImages(view, context, displayablePhotosArray);
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

    private File createJsonFile(String parentId,
                                String fileName,
                                String fileContent) throws IOException {

        Log.i(GOOGLE_DRIVE_TAG, ">>> Creating text file...");

        File metadata = new File()
                .setParents(buildParentsList(parentId))
                .setName(fileName)
                .setMimeType(TYPE_TXT);

        InputStream targetStream = new ByteArrayInputStream(fileContent.getBytes(Charset.forName("UTF-8")));
        AbstractInputStreamContent fileContentStream = new InputStreamContent(TYPE_JSON, targetStream);

        File googleFile = driveService.files()
                .create(metadata, fileContentStream)
                .setFields("id, parents, webContentLink")
                .execute();

        return googleFile;
    }

    private File createImgFile(String parentId,
                               String fileName,
                               String mimeType,
                               java.io.File filePath) throws IOException {

        Log.i(GOOGLE_DRIVE_TAG, ">>> Creating image file with content type: " + mimeType + "...");

        File metadata = new File()
                .setParents(buildParentsList(parentId))
                .setName(fileName)
                .setMimeType(mimeType);

        FileContent mediaContent = new FileContent(mimeType, filePath);

        File googleFile = driveService.files()
                .create(metadata, mediaContent)
                .setFields("id, parents, webContentLink")
                .execute();

        return googleFile;
    }

    private File updateJsonFile(String fileId,
                                String fileName,
                                String fileContent) throws IOException {

        File metadata = new File()
                .setName(fileName)
                .setMimeType(TYPE_TXT);

        InputStream targetStream = new ByteArrayInputStream(fileContent.getBytes(Charset.forName("UTF-8")));
        AbstractInputStreamContent newFileContentStream = new InputStreamContent(TYPE_JSON, targetStream);

        File googleFile = driveService.files()
                .update(fileId, metadata, newFileContentStream)
                .setFields("id, parents, webContentLink")
                .execute();

        return googleFile;
    }

    //     webContentLink
    private String readTxtFileContentsWithId(String googleDriveCatalogId) throws IOException {
        Log.i(GOOGLE_DRIVE_TAG, ">>> Reading text file contents with googleDriveCatalogId...");
        // Stream the file contents to a String.
        InputStream inputStream = driveService.files()
                .get(googleDriveCatalogId)
                .executeMediaAsInputStream();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }

        return stringBuilder.toString();
    }

    private String readTxtFileWithWebContentLink(String webContentLink) throws IOException {
        Log.i(GOOGLE_DRIVE_TAG, ">>> Reading text file contents with webContentLink...");
        Log.i(GOOGLE_DRIVE_TAG, " ::: " + webContentLink);

        InputStream inputStream = new URL(webContentLink).openStream();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder stringBuilder = new StringBuilder();

        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }

        return stringBuilder.toString();
    }

    private Bitmap readImgFileContentsWithId(String fileId, String mimeType) throws IOException {
        Log.i(GOOGLE_DRIVE_TAG, ">>> Reading image file contents using fileId...");

        InputStream inputStream = driveService.files()
                .get(fileId)
                .executeMediaAsInputStream();

        byte[] bitmapBytes = IOUtils.toByteArray(inputStream);

        return BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
    }

    private Bitmap readImgFileWithWebContentLink(Context context, String webContentLink) throws IOException {
        Log.i(GOOGLE_DRIVE_TAG, ">>> Reading image file contents using webContentLink...");
        Log.i(GOOGLE_DRIVE_TAG, "::: " + webContentLink);

        InputStream inputStream = context.getContentResolver().openInputStream(Uri.parse(webContentLink));

        byte[] bitmapBytes = IOUtils.toByteArray(inputStream);

        return BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
    }

    private Bitmap downloadImgFileWithId(String fileId, String mimeType) throws IOException {
        Log.i(GOOGLE_DRIVE_TAG, ">>> Reading image file contents...");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        driveService.files()
                .get(fileId)
                .executeMediaAndDownloadTo(outputStream);

        byte[] bitmapBytes = outputStream.toByteArray();

        return BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
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
        catalogJson.put("parentGoogleFolderId", catalogFolderId);
        catalogJson.put("photos", new JSONArray());
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