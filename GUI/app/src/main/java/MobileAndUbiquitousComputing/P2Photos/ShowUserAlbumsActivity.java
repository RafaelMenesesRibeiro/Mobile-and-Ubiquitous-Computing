package MobileAndUbiquitousComputing.P2Photos;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.dataobjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.helpers.QueryManager;
import MobileAndUbiquitousComputing.P2Photos.msgtypes.SuccessResponse;

import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.getUsername;
import static MobileAndUbiquitousComputing.P2Photos.dataobjects.RequestData.RequestType.GET_CATALOG;
import static MobileAndUbiquitousComputing.P2Photos.dataobjects.RequestData.RequestType.GET_CATALOG_TITLE;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

@SuppressWarnings("unchecked")
public class ShowUserAlbumsActivity extends AppCompatActivity {
    private ArrayList<String> catalogIdList;
    private ArrayList<String> catalogTitleList;
    protected static ArrayList<String> slicesURLList = new ArrayList<>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_user_albums);

        ListView userAlbumsListView = findViewById(R.id.userAlbumsList);

        this.catalogIdList = new ArrayList<>();
        this.catalogTitleList = new ArrayList<>();

        buildCatalogArrays();

        userAlbumsListView.setAdapter(newArrayAdapter(catalogTitleList));
        userAlbumsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String catalogTitle = catalogTitleList.get(position);
                String toast = "Loading: " + catalogTitle + "...";
                Toast.makeText(ShowUserAlbumsActivity.this, toast, LENGTH_SHORT).show();
                setSliceURLList(catalogIdList.get(position));

                if (!ShowUserAlbumsActivity.slicesURLList.isEmpty())  {
                    goToShowAlbumActivity(catalogTitle);
                } else {
                    Toast.makeText(ShowUserAlbumsActivity.this, "BAD", LENGTH_SHORT).show();
                }
            }
        });
    }

    private void buildCatalogArrays() {
        ArrayList<String> intentCatalogIdList = getIntent().getStringArrayListExtra("catalogs");
        String baseUrl =
                getString(R.string.view_album_details_endpoint) + "?calleeUsername=" + getUsername(this);
        for (String catalogId : intentCatalogIdList) {
            String url = baseUrl + "&catalogId=" + catalogId;
            tryAdd(catalogId, new RequestData(this, GET_CATALOG_TITLE, url));
        }
    }

    private void tryAdd(String catalogId, RequestData requestData) {
        try {
            ResponseData result = new QueryManager().execute(requestData).get();
            if (result.getServerCode() == HttpURLConnection.HTTP_OK) {
                SuccessResponse response = (SuccessResponse) result.getPayload();
                this.catalogIdList.add(catalogId);
                this.catalogTitleList.add((String) response.getResult());
            }
        } catch (ExecutionException | InterruptedException e) { /*continue;*/ }
    }

    private void setSliceURLList(String catalogId) {
        String url = getString(
                R.string.view_album_endpoint) +
                "?calleeUsername=" + getUsername(this) +
                "&catalogId=" + catalogId;

        try {
            RequestData requestData = new RequestData(this, GET_CATALOG, url);
            ResponseData responseData = new QueryManager().execute(requestData).get();
            if (responseData.getServerCode() == HttpURLConnection.HTTP_OK) {
                SuccessResponse payload = (SuccessResponse) responseData.getPayload();
                ShowUserAlbumsActivity.slicesURLList = (ArrayList<String>) payload.getResult();
            }
        } catch (ExecutionException | InterruptedException e) {
            // pass;
        }
    }

    private void goToShowAlbumActivity(String catalogTitle) {
        Intent intent = new Intent(ShowUserAlbumsActivity.this, ShowAlbumActivity.class);
        intent.putExtra("title", catalogTitle);
        intent.putStringArrayListExtra("slices", slicesURLList);
        startActivity(intent);
    }

    private ArrayAdapter<String> newArrayAdapter(ArrayList<String> items) {
        return new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
    }
}
