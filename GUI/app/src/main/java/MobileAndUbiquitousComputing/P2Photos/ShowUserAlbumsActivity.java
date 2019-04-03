package MobileAndUbiquitousComputing.P2Photos;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.util.ArrayList;

public class ShowUserAlbumsActivity extends AppCompatActivity {

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_user_albums);
        this.intent = getIntent();

        showUserAlbums();
    }

    private void showUserAlbums() {
        ArrayList<String> catalogIdList = intent.getStringArrayListExtra("catalogs");
    }

    private static SecureResponse getSecureResponse(HttpURLConnection connection) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = getJSONStringFromHttpResponse(connection);
        SecureResponse secureResponse = objectMapper.readValue(jsonResponse, SecureResponse.class);
        return secureResponse;
    }
}
