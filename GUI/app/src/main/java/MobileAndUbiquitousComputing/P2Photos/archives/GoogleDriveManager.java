package MobileAndUbiquitousComputing.P2Photos.archives;

import android.os.Environment;
import android.widget.Toast;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.UUID;

import MobileAndUbiquitousComputing.P2Photos.exceptions.GoogleDriveException;
import MobileAndUbiquitousComputing.P2Photos.helpers.AppContext;

@Deprecated
public class GoogleDriveManager {

    private static FileDataStoreFactory dataStoreFactory;
    private static final String MIME_TXT = "text/plain";
    private static final String MIME_JPG = "image/jpeg";
    private static final String MIME_PNG = "image/png";
    private static final String APPLICATION_NAME = "p2photos/1.0";
    private static final String CATALOG_NAME = "catalog";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private HttpTransport httpTransport;
    private Drive drive;
    private java.io.File downloadDirectory;

    /*****************************************************
     *
     * GoogleDriveManager CONSTRUCTORS and PRIVATE METHODS
     *
     *****************************************************/

    private static class GoogleDriveManagerHolder {
        private static final GoogleDriveManager INSTANCE = new GoogleDriveManager();
    }

    public static synchronized GoogleDriveManager getInstance() {
        return GoogleDriveManagerHolder.INSTANCE;
    }

    private GoogleDriveManager() {
        try {
            this.downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            this.drive = newDrive(newCredential());
            this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleDriveManager.dataStoreFactory = newFileDataStoreFactory();
        } catch (IOException | GeneralSecurityException | GoogleDriveException exc) {
            String message = "Could not construct a new Google Drive Manager";
            Toast.makeText(AppContext.getAppContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private File uploadFile(String fileName, String mimeType, boolean useDirectUpload) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(fileName);

        FileContent fileContent = new FileContent(mimeType, new java.io.File("TODO")); // TODO

        Drive.Files.Create insert = drive.files().create(fileMetadata, fileContent);
        MediaHttpUploader uploader = insert.getMediaHttpUploader();
        uploader.setDirectUploadEnabled(useDirectUpload);
        uploader.setProgressListener(null);
        return insert.execute();
    }

    /*****************************************************
     *
     * GoogleDriveManager PUBLIC METHODS
     *
     *****************************************************/

    public void downloadFile(boolean useDirectDownload, String googleDriveUrl) throws GoogleDriveException {

        try {
            if (invalidDownloadDirectory()) {
                throw new GoogleDriveException("Unable to create parent directory");
            }

            java.io.File uploadedFile = newUploadedFile(googleDriveUrl);
            OutputStream out = new FileOutputStream(new java.io.File(downloadDirectory, uploadedFile.getName()));

            MediaHttpDownloader downloader =
                    new MediaHttpDownloader(httpTransport, drive.getRequestFactory().getInitializer());

            downloader.setDirectDownloadEnabled(useDirectDownload);
            downloader.setProgressListener(null);

            // TODO Verify why it used to be uploadedFile.getDownloadUrl() inside GenericUrl
            downloader.download(new GenericUrl(new URL(googleDriveUrl)), out);
        } catch (IOException | URISyntaxException exc) {
            throw new GoogleDriveException("Could not download file from Google Drive");
        }

    }

    public File uploadTXTFile() throws IOException {
        return uploadFile(CATALOG_NAME, MIME_TXT, true);
    }

    public File uploadJPEGFile() throws IOException {
        return uploadFile(UUID.randomUUID().toString(), MIME_JPG, true);
    }

    public File uploadPNGFile() throws IOException {
        return uploadFile(UUID.randomUUID().toString(), MIME_PNG, true);
    }

    /*****************************************************
     *
     * PRIVATE HELPERS
     *
     *****************************************************/

    private boolean validClientID(GoogleClientSecrets clientSecrets) {
        return clientSecrets.getDetails().getClientId().trim().equals("");
    }

    private boolean validClientSecret(GoogleClientSecrets clientSecrets) {
        return clientSecrets.getDetails().getClientSecret().trim().equals("");
    }

    private boolean invalidDownloadDirectory() {
        // TODO is this check necessary in android?
        return !downloadDirectory.exists() && !downloadDirectory.mkdirs();
    }

    /** Authorizes the installed application to access user's protected data. */
    private Credential newCredential() throws IOException, GoogleDriveException {
        // TODO Get credentials the android way - lets google it
        // load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY, new InputStreamReader(
                        GoogleDriveManager.class.getResourceAsStream("/client_secrets.json"))
        );
        // soft validate secrets
        if (!validClientID(clientSecrets) || !validClientSecret(clientSecrets)) {
            throw new GoogleDriveException("Invalid client_secrets.json");
        }

        // set up authorization code flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets,
                Collections.singleton(DriveScopes.DRIVE_FILE)).setDataStoreFactory(dataStoreFactory)
                .build();

        // newCredential
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    private Drive newDrive(Credential credential) {
        return new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
    }

    /** Attempts to instantiate a FileDataStoreFactory that is shared by all GoogleDriveManagers */
    private FileDataStoreFactory newFileDataStoreFactory() throws GoogleDriveException {
        if (GoogleDriveManager.dataStoreFactory == null) {
            try {
                // TODO #1 AppContext.getAppContext() is the only way to get a Context inside a
                // TODO #1... Singleton instance of GoogleDriveManager
                // TODO #2 getFilesDir() android, is this it? What should be here.
                return new FileDataStoreFactory(AppContext.getAppContext().getFilesDir());
            } catch (IOException ioe) {
                String message = "Google Drive Manager FileDataStoreFactory could't be obtained";
                throw new GoogleDriveException(message);
            }
        }
        return GoogleDriveManager.dataStoreFactory;
    }

    private java.io.File newUploadedFile(String urlString) throws MalformedURLException, URISyntaxException {
        URL url = new URL(urlString);
        URI uri = url.toURI();
        return new java.io.File(uri);
    }
}