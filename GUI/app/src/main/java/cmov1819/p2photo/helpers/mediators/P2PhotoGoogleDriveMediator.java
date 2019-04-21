package cmov1819.p2photo.helpers.mediators;

import android.content.Context;

import java.util.concurrent.ConcurrentHashMap;

import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.GoogleDriveManager;

public class P2PhotoGoogleDriveMediator {
    public static ConcurrentHashMap<String, Object> resultsHashMap = new ConcurrentHashMap<>();

    private static P2PhotoGoogleDriveMediator instance;

    private AuthStateManager authStateManagerInstance;
    private GoogleDriveManager googleDriveManagerInstance;

    private P2PhotoGoogleDriveMediator(Context context) {
        this.authStateManagerInstance = AuthStateManager.getInstance(context);
        this.googleDriveManagerInstance = GoogleDriveManager.getInstance();
    }

    public static P2PhotoGoogleDriveMediator getInstance(final Context context) {
        if (instance == null)
            instance = new P2PhotoGoogleDriveMediator(context);
        return instance;
    }

    public String newCatatalog(final Context context, final String catalogName) {/*
        String catalogDriveId = null;
        String rootFolderId = googleDriveManagerInstance.createFolder(
                context, catalogName,authStateManagerInstance.getAuthState()
        );
        return catalogDriveId;*/
        return null;
    }

    public String addPhoto(final Context context,
                           final String photoName,
                           final String photoDriveId,
                           final String photoPath) {
        return null;
    }
}
