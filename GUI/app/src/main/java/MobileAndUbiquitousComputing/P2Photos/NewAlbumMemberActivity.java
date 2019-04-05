package MobileAndUbiquitousComputing.P2Photos;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedOperationException;
import MobileAndUbiquitousComputing.P2Photos.Helpers.QueryManager;
import MobileAndUbiquitousComputing.P2Photos.Helpers.SessionManager;

public class NewAlbumMemberActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_album_member);
    }

    public void addUserClicked(View view) {
        EditText albumIDInput = findViewById(R.id.albumIDInputBox);
        EditText usernameInput = findViewById(R.id.toAddUsernameInputBox);
        String albumID = albumIDInput.getText().toString();
        String username = usernameInput.getText().toString();

        if (albumID.equals("") || username.equals("")) {
            albumIDInput.setText("");
            usernameInput.setText("");
        }

        try {
            addMember(albumID, username);
        }
        catch (FailedOperationException foex) {
            Toast toast = Toast.makeText(this, "The add user to album operation failed. Try again later", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void addMember(String albumID, String username) throws FailedOperationException{
        Log.i("MSG", "Add User to Album: " + albumID + ", " + username);
        String url = getString(R.string.p2photo_host) + getString(R.string.add_member_operation);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("catalogId", albumID);
            requestBody.put("newMemberUsername", username);
            requestBody.put("calleeUsername", SessionManager.getUsername(this));
            RequestData requestData = new PostRequestData(this, RequestData.RequestType.NEW_ALBUM_MEMBER, url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();

            if (code == HttpURLConnection.HTTP_OK) {
                Log.i("STATUS", "The add user to album operation was successful");
            }
            else if (code == HttpURLConnection.HTTP_BAD_REQUEST) {
                // TODO //
                Log.i("STATUS", "The add user to album operation was unsuccessful. " +
                        "HTTP_BARD_REQUEST. Server response code: " + code);
            }
            else {
                Log.i("STATUS", "The add user to album operation was unsuccessful. Server response code: " + code);
                throw new FailedOperationException();
            }
        }
        catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
