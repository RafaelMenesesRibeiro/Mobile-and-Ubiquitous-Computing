package MobileAndUbiquitousComputing.P2Photos;

import MobileAndUbiquitousComputing.P2Photos.Helpers.Login;
import MobileAndUbiquitousComputing.P2Photos.msgtypes.SuccessResponse;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ShowUserAlbumsActivity extends AppCompatActivity {
    private Intent intent;
    private final String WEBSERVER = "https://p2photo-production.herokuapp.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.intent = getIntent();
        HashMap<String, String> catalogsMap = buildCatalogsMap();
        setContentView(R.layout.activity_show_user_albums);
    }

    private HashMap<String, String> buildCatalogsMap() {
        String baseUrl = WEBSERVER + "viewAlbumDetails?calleeUsername=" + Login.getUsername() + "&catalogId=";
        ArrayList<String> catalogIdList = intent.getStringArrayListExtra("catalogs");
        HashMap<String, String> catalogsMap = new HashMap<>();

        for (String catalogId : catalogIdList) {
            try {
                // TODO SET COOKIE VALUE
                HttpURLConnection connection = initiateGETConnection(baseUrl + catalogId);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    SuccessResponse response = getSuccessResponse(connection);
                    String catalogTitle = (String)response.getResult();
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
}
