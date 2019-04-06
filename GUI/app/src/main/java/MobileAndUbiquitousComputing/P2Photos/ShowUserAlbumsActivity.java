package MobileAndUbiquitousComputing.P2Photos;

        import android.content.Intent;
        import android.os.Bundle;
        import android.os.Handler;
        import android.support.v7.app.AppCompatActivity;
        import android.view.View;
        import android.widget.AdapterView;
        import android.widget.ArrayAdapter;
        import android.widget.ListView;
        import android.widget.ProgressBar;
        import android.widget.Toast;

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
    private ListView userAlbumsListView;
    private ArrayList<String> catalogIdList;
    private ArrayList<String> catalogTitleList;
    private ProgressBar progressBar;
    private Handler progressBarHandler = new Handler();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_user_albums);

        progressBar = findViewById(R.id.progressBar);
        userAlbumsListView = findViewById(R.id.userAlbumsList);
        this.catalogIdList = new ArrayList<>();
        this.catalogTitleList = new ArrayList<>();

        buildCatalogMapping();

        userAlbumsListView.setAdapter(newArrayAdapter(catalogTitleList));

        userAlbumsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String toast = "Loading: " + catalogTitleList.get(position) + "...";
                Toast.makeText(ShowUserAlbumsActivity.this, toast, Toast.LENGTH_LONG).show();

                ArrayList<String> slicesList = getAlbumSlices(catalogIdList.get(position));

                Intent intent = new Intent(ShowUserAlbumsActivity.this, ShowAlbumActivity.class);
                intent.putStringArrayListExtra("slices", slicesList);
                startActivity(intent);
            }
        });
    }

    private ArrayList<String> getAlbumSlices(String catalogId) {
        SliceLoader sliceLoader = new SliceLoader(catalogId, this);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<ArrayList<String>> future = executorService.submit(sliceLoader);
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException exc) {
            String toast = "Unable to load catalog...";
            Toast.makeText(ShowUserAlbumsActivity.this, toast, Toast.LENGTH_LONG).show();
            return new ArrayList<>();
        }
    }

    private void buildCatalogMapping() {
        ArrayList<String> intentCatalogIdList = getIntent().getStringArrayListExtra("catalogs");
        String baseUrl =
                getString(R.string.view_album_details_endpoint) + "?calleeUsername=" + getUsername(this);

        for (String catalogId : intentCatalogIdList) {
            String requestUrl = baseUrl + "&catalogId=" + catalogId;
            tryMap(catalogId, new RequestData(this, GET_CATALOG_TITLE, requestUrl));
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

    public void startProgress() {
        progressBarHandler.post(new Runnable() {
            @Override
            public void run() {
                userAlbumsListView.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    public void finishProgress() {
        progressBarHandler.post(new Runnable() {
            @Override
            public void run() {
                userAlbumsListView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    private ArrayAdapter<String> newArrayAdapter(ArrayList<String> items) {
        return new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
    }
}
