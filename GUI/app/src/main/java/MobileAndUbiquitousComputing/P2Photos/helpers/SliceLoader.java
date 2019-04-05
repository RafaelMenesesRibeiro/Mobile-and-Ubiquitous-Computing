package MobileAndUbiquitousComputing.P2Photos.helpers;

import android.widget.ProgressBar;
import android.os.Handler;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class SliceLoader implements Callable<ArrayList<String>> {
    private final String catalogId;
    private final ProgressBar progressBar;
    private final Handler progressBarHandler;
    private ArrayList<String> slicesList = new ArrayList<>();
    private static final int LOAD_COMPLETE = 100;

    public SliceLoader(String catalogId, ProgressBar progressBar, Handler progressBarHandler) {
        this.catalogId = catalogId;
        this.progressBar = progressBar;
        this.progressBarHandler = progressBarHandler;
    }

    @Override
    public ArrayList<String> call() throws Exception {
        // TODO call WebServer to retrieve slices list.
        progressBarHandler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(LOAD_COMPLETE);
            }
        });
        return slicesList;
    }
}
