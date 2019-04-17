package MobileAndUbiquitousComputing.P2Photos;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.dataobjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.exceptions.FailedOperationException;
import MobileAndUbiquitousComputing.P2Photos.helpers.QueryManager;
import MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
    }

    @Override
    public void onBackPressed() {

    }

    public void viewAlbumClicked(View view) {
        Intent intent = new Intent(this, ShowAlbumActivity.class);
        startActivity(intent);
    }

    public void createAlbumClicked(View view) {
        Intent intent = new Intent(this, NewAlbumActivity.class);
        startActivity(intent);
    }

    public void FindUserClicked(View view) {
        Intent intent = new Intent(this, SearchUserActivity.class);
        startActivity(intent);
    }

    public void AddPhotosClicked(View view) {
        Intent intent = new Intent(this, AddPhotosActivity.class);
        startActivity(intent);
    }

    public void AddUsersClicked(View view) {
        Intent intent = new Intent(this, NewAlbumMemberActivity.class);
        startActivity(intent);
    }

    public void ListAlbumsClicked(View view) {
        ArrayList<String> items = new ArrayList<>(Arrays.asList("239287741","401094244","519782246"));
        Intent intent = new Intent(this, ShowUserAlbumsActivity.class);
        intent.putStringArrayListExtra("catalogs", items);
        startActivity(intent);
    }

    public void LogoutClicked(View view) {
        logout();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void logout() {
        String username = SessionManager.getUsername(this);
        Log.i("MSG", "Logout: " + username);

        String url =
                getString(R.string.p2photo_host) + getString(R.string.logout_operation) + username;
        RequestData rData = new RequestData(this, RequestData.RequestType.LOGOUT, url);
        try {
            ResponseData result = new QueryManager().execute(rData).get();
            int code = result.getServerCode();
            if (code == 200) {
                Log.i("STATUS", "The logout operation was successful");
                SessionManager.deleteSessionID(this);
                SessionManager.deleteUserName(this);
            } else {
                Log.i("STATUS", "The login operation was unsuccessful. Unknown error.");
                throw new FailedOperationException();
            }
        } catch (ExecutionException | InterruptedException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
