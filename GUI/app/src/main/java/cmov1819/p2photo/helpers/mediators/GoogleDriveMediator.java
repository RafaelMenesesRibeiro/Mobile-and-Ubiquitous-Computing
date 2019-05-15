package cmov1819.p2photo.helpers.mediators;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Pair;
import android.view.View;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainApplication;
import cmov1819.p2photo.ViewCatalogFragment;
import cmov1819.p2photo.helpers.architectures.cloudBackedArchitecture.CloudBackedArchitecture;
import cmov1819.p2photo.helpers.managers.LogManager;
import okhttp3.MediaType;

import static cmov1819.p2photo.helpers.ConvertUtils.byteArrayOutputStreamToBitmap;
import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToBitmap;
import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToString;

@SuppressLint("StaticFieldLeak")
public class GoogleDriveMediator {
    public static final String APPLICATION_NAME = MainApplication.getApplicationName();

    public static final String GOOGLE_DRIVE_TAG = "DRIVE MEDIATOR";

    public static final String TYPE_GOOGLE_DRIVE_FOLDER = "application/vnd.google-apps.folder";

    public static final String TYPE_JSON = "application/json; charset=utf-8";
    public static final String TYPE_TXT = "text/plain";
    public static final String TYPE_JPEG = "image/jpeg";
    public static final String TYPE_PNG = "image/png";
    public static final String TYPE_BMP = "image/bmp";
    public static final String TYPE_GIF = "image/gif";
    public static final String TYPE_WEBP = "image/webp";

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


    public void newCatalogSlice(final Context context,
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

                                String msg = ">>> Creating folder... ID = " + parentFolderFile.getId();
                                LogManager.logInfo(GOOGLE_DRIVE_TAG, msg);

                                String catalogFolderId = parentFolderFile.getId();
                                String catalogJsonContent = newCatalogJsonFile(title, p2photoId, catalogFolderId);
                                File catalogJsonFile = createJsonFile(catalogFolderId,"catalog", catalogJsonContent);

                                if (catalogJsonFile == null) {
                                    setWarning(context,"Null catalog.json file received from Google REST API.");
                                }

                                msg = ">>> Creating catalog... ID = " + catalogJsonFile.getId();
                                LogManager.logInfo(GOOGLE_DRIVE_TAG, msg);

                                Permission userPermission = new Permission()
                                        .setAllowFileDiscovery(true)
                                        .setType("anyone")
                                        .setRole("reader")
                                        .set("shared", true);

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
                            LogManager.toast((Activity) context, "Couldn't create catalog");
                        }
                        else {
                            CloudBackedArchitecture.createCatalogSlice(
                                context,
                                p2photoId,
                                parentFolderFile.getId(),
                                catalogJsonFile.getId(),
                                catalogJsonFile.getWebContentLink()
                            );
                            LogManager.toast((Activity) context, "Catalog created");
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

                                Permission userPermission = new Permission()
                                        .setAllowFileDiscovery(true)
                                        .setType("anyone")
                                        .setRole("reader")
                                        .set("shared", true);

                                driveService.permissions()
                                        .create(newGooglePhotoFile.getId(), userPermission)
                                        .setFields("id")
                                        .execute();

                                if (newGooglePhotoFile == null) {
                                    setWarning(context,"Null response received from Google REST API.");
                                    return null;
                                } else {
                                    JSONObject currentCatalog = new JSONObject(readTxtFileContentsWithId(catalogFileGoogleId));
                                    JSONArray photos = currentCatalog.getJSONArray("photos");
                                    photos.put(newGooglePhotoFile.getWebContentLink());
                                    currentCatalog.put("photos", photos);
                                    String newFileContent = currentCatalog.toString(4);
                                    File googleFile = updateJsonFile(catalogFileGoogleId, "catalog", newFileContent);
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
                            LogManager.toast((Activity) context, "Couldn't upload photo");
                        }
                        else {
                            LogManager.toast((Activity) context, "Upload complete");
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
                        ArrayList<Bitmap> displayablePhotosList = new ArrayList<>();
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

                            String msg = String.format(
                                    "Reading catalog contents: \n%s",
                                    catalogFileContents.toString(4)
                            );
                            LogManager.logInfo(GOOGLE_DRIVE_TAG, msg);

                            if (catalogFileContents.has("photos")) {
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
                            }
                        } catch (IOException | JSONException exc) {
                            setError(context, exc.getMessage());
                        }
                        return displayablePhotosList;
                    }

                    @Override
                    protected void onPostExecute(ArrayList<Bitmap> photosFileIdList) {
                        ViewCatalogFragment.drawImages(view, context, photosFileIdList);
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

        String msg = ">>> Creating folder...";
        LogManager.logInfo(GOOGLE_DRIVE_TAG, msg);

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

        String msg = ">>> Creating text file...";
        LogManager.logInfo(GOOGLE_DRIVE_TAG, msg);

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

        String msg = ">>> Creating image file with content type: " + mimeType + "...";
        LogManager.logInfo(GOOGLE_DRIVE_TAG, msg);

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
        String msg = ">>> Reading text file contents with googleDriveCatalogId...";
        LogManager.logInfo(GOOGLE_DRIVE_TAG, msg);
        // Stream the file contents to a String.
        InputStream inputStream = driveService.files()
                .get(googleDriveCatalogId)
                .executeMediaAsInputStream();

        return inputStreamToString(inputStream);
    }

    private String readTxtFileWithWebContentLink(String webContentLink) throws IOException {
        String msg = ">>> Reading text file contents with webContentLink...";
        LogManager.logInfo(GOOGLE_DRIVE_TAG, msg);
        msg = " ::: " + webContentLink;
        LogManager.logInfo(GOOGLE_DRIVE_TAG, msg);

        InputStream inputStream = new URL(webContentLink).openStream();

        return inputStreamToString(inputStream);
    }

    private Bitmap readImgFileContentsWithId(String fileId, String mimeType) throws IOException {
        String msg = ">>> Reading image file contents using fileId...";
        LogManager.logInfo(GOOGLE_DRIVE_TAG, msg);

        InputStream inputStream = driveService.files()
                .get(fileId)
                .executeMediaAsInputStream();

        byte[] bitmapBytes = IOUtils.toByteArray(inputStream);

        return BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
    }

    private Bitmap readImgFileWithWebContentLink(Context context, String webContentLink) throws IOException {
        LogManager.logInfo(GOOGLE_DRIVE_TAG, ">>> Reading image file contents using webContentLink...");
        InputStream inputStream = new URL(webContentLink).openStream();
        return inputStreamToBitmap(inputStream);
    }

    private Bitmap downloadImgFileWithId(String fileId, String mimeType) throws IOException {
        LogManager.logInfo(GOOGLE_DRIVE_TAG,  ">>> Reading image file contents...");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        driveService.files()
                .get(fileId)
                .executeMediaAndDownloadTo(outputStream);
        return byteArrayOutputStreamToBitmap(outputStream);
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

    private void setError(Context context, String msg) {
        LogManager.logError(GOOGLE_DRIVE_TAG, msg);
    }

    private void setWarning(Context context, String message) {
        LogManager.logWarning(GOOGLE_DRIVE_TAG, message);
    }

    private void suggestRetry(Context context, String message) {
        setWarning(context, "Google Drive REST API timed out.");
    }

    private void suggestReauthentication(Context context, String message) {
        setWarning(context, message);
        context.startActivity(new Intent(context, LoginActivity.class));
    }

}
