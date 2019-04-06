package MobileAndUbiquitousComputing.P2Photos.archives;
/*
import java.util.ArrayList;
import java.util.concurrent.Callable;

import MobileAndUbiquitousComputing.P2Photos.ShowUserAlbumsActivity;

public class SliceLoader implements Callable<ArrayList<String>> {
    private final String catalogId;
    private final ShowUserAlbumsActivity activity;

    public SliceLoader(String catalogId, ShowUserAlbumsActivity activity) {
        this.catalogId = catalogId;
        this.activity = activity;
    }

    @Override
    public ArrayList<String> call() {

        activity.startProgress();
        ArrayList<String> slicesList = activity.getAlbumSlices(catalogId);
        activity.finishProgress();
        return slicesList;

    }
}
*/