package cmov1819.p2photo.helpers.mediators;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cmov1819.p2photo.helpers.datastructures.DriveResultsData;
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.GoogleDriveManager;

import static android.widget.Toast.LENGTH_SHORT;

public class P2PhotoGoogleDriveMediator {
    private static final String P2P_MEDIATOR_TAG = "P2P Mediator";
    private static P2PhotoGoogleDriveMediator instance;
    private AuthStateManager authStateMgr;
    private GoogleDriveManager googleDriveMgr;
    private AtomicInteger requestCounter;

    public static ConcurrentHashMap<Integer, DriveResultsData> requestsMap;

    private P2PhotoGoogleDriveMediator(Context context) {
        this.authStateMgr = AuthStateManager.getInstance(context);
        this.googleDriveMgr = GoogleDriveManager.getInstance();
        this.requestCounter = new AtomicInteger(0);
        P2PhotoGoogleDriveMediator.requestsMap = new ConcurrentHashMap<>();
    }

    public static P2PhotoGoogleDriveMediator getInstance(final Context context) {
        if (instance == null)
            instance = new P2PhotoGoogleDriveMediator(context);
        return instance;
    }

    @SuppressLint("StaticFieldLeak")
    public void newCatalog(final Context context,
                           final String catalogName,
                           final String catalogSlice,
                           final Integer requestId) {

        // Only creates a folder in Google Drive and a file with the specified name inside it
        new AsyncTask<String, Void, DriveResultsData>() {
            @Override
            protected DriveResultsData doInBackground(String... strings) {
                googleDriveMgr.createFolder(context, catalogName, catalogSlice, requestId, authStateMgr.getAuthState());
                return requestsMap.get(requestId);
            }

            @Override
            protected void onPostExecute(DriveResultsData driveResultsData) {
                if (driveResultsData.getSuggestRetry() && driveResultsData.getAttempts() < 3) {
                    newCatalog(context, catalogName, catalogSlice, requestId);
                } else if (driveResultsData.getSuggestedIntent() != null) {
                    context.startActivity(driveResultsData.getSuggestedIntent());
                    requestsMap.remove(requestId);
                } else if (driveResultsData.getHasError()) {
                    Toast.makeText(context, driveResultsData.getMessage(), LENGTH_SHORT).show();
                    requestsMap.remove(requestId);
                } else {
                    Toast.makeText(context, "Created catalog", LENGTH_SHORT).show();
                    requestsMap.remove(requestId);
                }
            }
        }.execute();
    }

    public DriveResultsData newPhoto(final Context context,
                                     final String photoName,
                                     final String photoDriveId,
                                     final String photoPath) {
        Integer requestId = newDriveResultMapping();
        return null;
    }

    public Integer newDriveResultMapping() {
        Integer requestId = requestCounter.incrementAndGet();
        requestsMap.put(requestId, new DriveResultsData());
        return requestId;
    }
}
