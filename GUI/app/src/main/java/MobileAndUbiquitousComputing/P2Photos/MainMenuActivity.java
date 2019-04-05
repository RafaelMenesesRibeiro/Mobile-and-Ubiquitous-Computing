package MobileAndUbiquitousComputing.P2Photos;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedLogoutException;
import MobileAndUbiquitousComputing.P2Photos.Helpers.ConnectionManager;
import MobileAndUbiquitousComputing.P2Photos.Helpers.QueryManager;
import MobileAndUbiquitousComputing.P2Photos.Helpers.SessionID;
import MobileAndUbiquitousComputing.P2Photos.Helpers.SessionManager;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
    }

    @Override
    public void onBackPressed() {
        // Do nothing.
        // Prevents going back to the login screen, or the previously completed
        // activity.
    }

    public void viewAlbumClicked(View view) {
        Intent intent = new Intent(this, AlbumViewActivity.class);
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
        // TODO - Design GUI for this. //
    }

    public void AddUsersClicked(View view) {
        // TODO - Design GUI for this. //
    }

    public void ListAlbumsClicked(View view) {
        Intent intent = new Intent(this, ShowUserAlbumsActivity.class);
        startActivity(intent);
    }

    public void LogoutClicked(View view) {
        logout();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void logout() {
        String username = SessionManager.username;
        Log.i("MSG", "Logout: " + username);

        String url = ConnectionManager.P2PHOTO_HOST + ConnectionManager.LOGOUT_OPERATION + username;
        RequestData rData = new RequestData(this, RequestData.RequestType.LOGOUT, url);
        try {
            ResponseData result = new QueryManager().execute(rData).get();
            int code = result.getServerCode();
            if (code == 200) {
                Log.i("STATUS", "The logout operation was successful");
                SessionID.deleteSessionID(this);
            }
            else {
                Log.i("STATUS", "The login operation was unsuccessful. Unknown error.");
                throw new FailedLogoutException();
            }
        }
        catch (ExecutionException | InterruptedException ex) {
            throw new FailedLogoutException(ex.getMessage());
        }
    }
}
