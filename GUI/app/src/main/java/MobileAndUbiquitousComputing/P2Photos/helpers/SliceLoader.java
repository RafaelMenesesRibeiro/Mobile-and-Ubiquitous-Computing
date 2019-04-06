package MobileAndUbiquitousComputing.P2Photos.helpers;

import android.view.View;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import MobileAndUbiquitousComputing.P2Photos.ShowUserAlbumsActivity;

import static android.os.SystemClock.sleep;

public class SliceLoader implements Callable<ArrayList<String>> {
    private final String catalogId;
    private final ShowUserAlbumsActivity activity;

    public SliceLoader(String catalogId, ShowUserAlbumsActivity activity) {
        this.catalogId = catalogId;
        this.activity = activity;
    }

    @Override
    public ArrayList<String> call() {
        ArrayList<String> slicesList = new ArrayList<>();
        activity.startProgress();
        // TODO call WebServer to retrieve slices list.
        sleep(2000);
        activity.finishProgress();
        return slicesList;
    }
}
