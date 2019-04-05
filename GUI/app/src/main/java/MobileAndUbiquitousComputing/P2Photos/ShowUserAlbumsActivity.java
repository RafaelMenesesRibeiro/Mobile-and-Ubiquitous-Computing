package MobileAndUbiquitousComputing.P2Photos;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.Helpers.QueryManager;
import MobileAndUbiquitousComputing.P2Photos.MsgTypes.SuccessResponse;

import static MobileAndUbiquitousComputing.P2Photos.Helpers.SessionManager.getUsername;

public class ShowUserAlbumsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_user_albums);

        ListView userAlbumsListView = findViewById(R.id.userAlbumsList);
        ArrayList<HashMap<String, String>> catalogsList = new ArrayList<>();
        HashMap<String, String> catalogIdNameMap = buildCatalogsMap();
        for (Map.Entry keyValuePair : catalogIdNameMap.entrySet()) {
            HashMap<String, String> catalog = new HashMap<>();
            catalog.put("catalogId", keyValuePair.getKey().toString());
            catalog.put("catalogName", keyValuePair.getValue().toString());
            catalogsList.add(catalog);
        }
        SimpleAdapter simpleAdapter = newCatalogAdapter(catalogsList);
        userAlbumsListView.setAdapter(simpleAdapter);
    }

    private HashMap<String, String> buildCatalogsMap() {
        HashMap<String, String> catalogsMap = new HashMap<>();
        ArrayList<String> catalogIdList = getIntent().getStringArrayListExtra("catalogs");
        String baseUrl = getString(R.string.p2photo_host)+"viewAlbumDetails?calleeUsername="+getUsername(this);
        for (String catalogId : catalogIdList) {
            String requestUrl = baseUrl + "&catalogId=" + catalogId;
            RequestData requestData = new RequestData(this, RequestData.RequestType.GET_CATALOG_TITLE, requestUrl);
            tryPut(catalogId, catalogsMap, requestData);
        }
        return catalogsMap;
    }

    private void tryPut(String catalogId, HashMap<String, String> catalogsMap, RequestData requestData) {
        try {
            ResponseData result = new QueryManager().execute(requestData).get();
            if (result.getServerCode() == HttpURLConnection.HTTP_OK) {
                SuccessResponse response = (SuccessResponse)result.getPayload();
                String catalogTitle = (String) response.getResult();
                catalogsMap.put(catalogId, catalogTitle);
            }
        } catch (ExecutionException | InterruptedException e) { /*continue;*/ }
    }

    private SimpleAdapter newCatalogAdapter(ArrayList<HashMap<String, String>> itemsMap) {
        return new SimpleAdapter(this,
                itemsMap,
                R.layout.hashmap_array_adapter_layout,
                new String[]{"catalogId", "catalogName"},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
    };
}
