package MobileAndUbiquitousComputing.P2Photos;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import MobileAndUbiquitousComputing.P2Photos.Helpers.Login;
import MobileAndUbiquitousComputing.P2Photos.Helpers.SessionID;
import MobileAndUbiquitousComputing.P2Photos.MsgTypes.SuccessResponse;

import static MobileAndUbiquitousComputing.P2Photos.Helpers.ConnectionManager.P2PHOTO_DOMAIN;
import static MobileAndUbiquitousComputing.P2Photos.Helpers.ConnectionManager.P2PHOTO_HOST;
import static MobileAndUbiquitousComputing.P2Photos.Helpers.SessionManager.username;

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
        String baseUrl = P2PHOTO_HOST + "viewAlbumDetails?calleeUsername=" + username + "&catalogId=";
        ArrayList<String> catalogIdList = getIntent().getStringArrayListExtra("catalogs");
        HashMap<String, String> catalogsMap = new HashMap<>();
        for (String catalogId : catalogIdList) {
            try {
                HttpURLConnection connection = initiateGETConnection(baseUrl + catalogId);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    SuccessResponse response = getSuccessResponse(connection);
                    String catalogTitle = (String) response.getResult();
                    catalogsMap.put(catalogId, catalogTitle);
                }
            } catch (IOException ioe) {
                // continue;
            }
        }
        return catalogsMap;
    }

    private SuccessResponse getSuccessResponse(HttpURLConnection connection) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = getJSONStringFromHttpResponse(connection);
        return objectMapper.readValue(jsonResponse, SuccessResponse.class);
    }

    private HttpURLConnection initiateGETConnection(String requestUrl) throws IOException {
        setCookie(); // TODO FINISH SET COOKIE WHEN OTHER CODE IS STABLE
        URL url = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(2000);
        return connection;
    }

    private void setCookie() {
        HttpCookie cookie = new HttpCookie("sessionId", SessionID.getSessionID(this));
        cookie.setDomain(P2PHOTO_DOMAIN);
        cookie.setPath("/");
        cookie.setVersion(0);
        try {
            Login.cookieManager.getCookieStore().add(new URI(P2PHOTO_HOST), cookie);
        } catch (URISyntaxException use) {
            use.printStackTrace();
        }
    }

    private String getJSONStringFromHttpResponse(HttpURLConnection connection) throws IOException {
        String currentLine;
        StringBuilder jsonResponse = new StringBuilder();
        InputStreamReader inputStream = new InputStreamReader(connection.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputStream);
        while ((currentLine = bufferedReader.readLine()) != null) {
            jsonResponse.append(currentLine);
        }
        bufferedReader.close();
        inputStream.close();
        return jsonResponse.toString();
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
