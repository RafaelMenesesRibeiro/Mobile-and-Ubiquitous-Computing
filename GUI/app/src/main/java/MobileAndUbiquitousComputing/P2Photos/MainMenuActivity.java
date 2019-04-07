package MobileAndUbiquitousComputing.P2Photos;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.dataobjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.exceptions.FailedOperationException;
import MobileAndUbiquitousComputing.P2Photos.helpers.QueryManager;
import MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager;

import static com.google.android.gms.common.GooglePlayServicesUtil.getErrorDialog;

public class MainMenuActivity extends AppCompatActivity {

    private static final int GPS_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        GoogleApiAvailability apiAvailabilityInstance = GoogleApiAvailability.getInstance();

        int resultCode = apiAvailabilityInstance.isGooglePlayServicesAvailable(getApplicationContext());
        if (!(resultCode == ConnectionResult.SUCCESS)) {
            Dialog errorDialog = apiAvailabilityInstance.getErrorDialog(
                    this, resultCode, GPS_REQUEST_CODE, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    });

            if (errorDialog != null) { errorDialog.show(); }
        }
    }

    @Override
    public void onBackPressed() {
        // Do nothing.
        // Prevents going back to the login screen, or the previously completed
        // activity.
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
        // TODO - Implement this. //
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
