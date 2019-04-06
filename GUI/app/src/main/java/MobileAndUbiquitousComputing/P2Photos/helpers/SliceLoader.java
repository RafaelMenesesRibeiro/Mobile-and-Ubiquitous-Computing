package MobileAndUbiquitousComputing.P2Photos.helpers;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import static android.os.SystemClock.sleep;

public class SliceLoader implements Callable<ArrayList<String>> {
    private final String catalogId;

    public SliceLoader(String catalogId) {
        this.catalogId = catalogId;
    }

    @Override
    public ArrayList<String> call() {
        ArrayList<String> slicesList = new ArrayList<>();
        sleep(2000);
        // TODO call WebServer to retrieve slices list.
        return slicesList;
    }
}
