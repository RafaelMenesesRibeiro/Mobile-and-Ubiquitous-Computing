package cmov1819.p2photo.helpers.mediators;

import android.content.Context;
import android.widget.Toast;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cmov1819.p2photo.helpers.datastructures.DriveResultsData;
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.GoogleDriveManager;

public class P2PhotoGoogleDriveMediator {
    private static P2PhotoGoogleDriveMediator instance;

    private AuthStateManager authStateMgr;
    private GoogleDriveManager googleDriveMgr;
    private AtomicInteger requestCounter;

    private Hashtable<Integer, DriveResultsData> requestsMap;

    private P2PhotoGoogleDriveMediator(Context context) {
        this.authStateMgr = AuthStateManager.getInstance(context);
        this.googleDriveMgr = GoogleDriveManager.getInstance();
        this.requestCounter = new AtomicInteger(0);
        this.requestsMap = new Hashtable<>();
    }

    public static P2PhotoGoogleDriveMediator getInstance() {
        return instance;
    }

    public static P2PhotoGoogleDriveMediator getInstance(final Context context) {
        if (instance == null)
            instance = new P2PhotoGoogleDriveMediator(context);
        return instance;
    }

    public DriveResultsData newCatatalog(final Context context, final String catalogName) {
        Integer requestId = newDriveResultMapping();

        googleDriveMgr.createFolder(context, catalogName, requestId, authStateMgr.getAuthState());
        String rootFolderId = requestsMap.get(requestId).getFolderId();
        if (rootFolderId == null) {
            return null;
        }

        googleDriveMgr.createFile(context, requestId,"catalog", rootFolderId, authStateMgr.getAuthState());
        String fileId = requestsMap.get(requestId).getFileId();
        if (fileId == null) {
            return null;
        }

        return requestsMap.remove(requestId); // remove returns previously value associated with key
    }

    public DriveResultsData addPhoto(final Context context,
                           final String photoName,
                           final String photoDriveId,
                           final String photoPath) {
        Integer requestId = newDriveResultMapping();
        return null;
    }

    private Integer newDriveResultMapping() {
        Integer requestId = requestCounter.incrementAndGet();
        requestsMap.put(requestId, new DriveResultsData());
        return requestId;
    }

    public void putResult(Integer key, DriveResultsData value) {
        requestsMap.put(key, value);
    }

    public DriveResultsData getResult(Integer key) {
        return requestsMap.get(key);
    }

    public DriveResultsData removeResult(Integer key) {
        return requestsMap.remove(key);
    }

    public void replaceResult(Integer key, DriveResultsData value) {
        requestsMap.remove(key);
        requestsMap.put(key, value);
    }
}
