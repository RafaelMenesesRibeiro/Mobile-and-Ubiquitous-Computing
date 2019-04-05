package MobileAndUbiquitousComputing.P2Photos;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import MobileAndUbiquitousComputing.P2Photos.dataobjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.helpers.QueryManager;
import MobileAndUbiquitousComputing.P2Photos.helpers.SliceLoader;
import MobileAndUbiquitousComputing.P2Photos.msgtypes.SuccessResponse;

import static MobileAndUbiquitousComputing.P2Photos.dataobjects.RequestData.RequestType.GET_CATALOG_TITLE;
import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.getUsername;

public class ShowUserAlbumsActivity extends AppCompatActivity {
    private ArrayList<String> catalogIdList;
    private ArrayList<String> catalogTitleList;
    private ProgressBar progressBar;
    private Handler progressBarHandler = new Handler();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_user_albums);
        ListView userAlbumsListView = findViewById(R.id.userAlbumsList);

        this.catalogIdList = new ArrayList<>();
        this.catalogTitleList = new ArrayList<>();

        buildCatalogMapping();

        userAlbumsListView.setAdapter(newCatalogAdapter(catalogTitleList));

        progressBar = findViewById(R.id.circularProgressBar);

        try {
            ArrayList<String> slicesList = getAlbumSlices(catalogIdList.get(0));
        } catch (ExecutionException e) {
            // TODO 
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /*
        userAlbumsListView.setOnClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String message = "Loading: " + catalogTitleList.get(position);
                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
                toast.show();
            }
        });
        */
    }

    private ArrayList<String> getAlbumSlices(String catalogId) throws ExecutionException, InterruptedException {
        SliceLoader sliceLoader = new SliceLoader(catalogId, this.progressBar, this.progressBarHandler);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<ArrayList<String>> future = executorService.submit(sliceLoader);
        return future.get();
    }

    private void buildCatalogMapping() {
        ArrayList<String> intentedCatalogIdList = getIntent().getStringArrayListExtra("catalogs");
        String baseUrl = getString(R.string.p2photo_host)+"viewAlbumDetails?calleeUsername=" + getUsername(this);
        for (String catalogId : intentedCatalogIdList) {
            tryMap(catalogId, new RequestData(this, GET_CATALOG_TITLE, baseUrl + "&catalogId=" + catalogId));
        }
    }

    private void tryMap(String catalogId, RequestData requestData) {
        try {
            ResponseData result = new QueryManager().execute(requestData).get();
            if (result.getServerCode() == HttpURLConnection.HTTP_OK) {
                SuccessResponse response = (SuccessResponse) result.getPayload();
                this.catalogIdList.add(catalogId);
                this.catalogTitleList.add((String) response.getResult());
            }
        } catch (ExecutionException | InterruptedException e) { /*continue;*/ }
    }

    private ArrayAdapter<String> newCatalogAdapter(ArrayList<String> items) {
        return new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
    }
}
