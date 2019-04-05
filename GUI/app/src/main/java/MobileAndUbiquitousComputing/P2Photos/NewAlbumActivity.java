package MobileAndUbiquitousComputing.P2Photos;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedOperationException;
import MobileAndUbiquitousComputing.P2Photos.Helpers.QueryManager;
import MobileAndUbiquitousComputing.P2Photos.Helpers.SessionManager;

public class NewAlbumActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_album);
    }

    public void newAlbumClicked(View view) {
        EditText titleInput = (EditText) findViewById(R.id.titleInputBox);
        String title = titleInput.getText().toString();

        if (title.equals("")) {
            Toast toast = Toast.makeText(this, "Enter a name for the album", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        try {
            newAlbum(title);
            Intent intent = new Intent(this, MainMenuActivity.class);
            startActivity(intent);
        }
        catch (FailedOperationException foex) {
            Toast toast = Toast.makeText(this, "The create album operation failed. Try again later", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void newAlbum(String albumName) {
        Log.i("MSG", "Create album: " + albumName);
        String url = getString(R.string.p2photo_host) + getString(R.string.new_album_operation);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("catalogTitle", albumName);
            // TODO - Implement adding slice to Cloud Provider. //
            requestBody.put("sliceUrl", "http://www.acloudprovider.com/a_album_slice");
            requestBody.put("calleeUsername", SessionManager.getUserName(this));
            RequestData requestData = new PostRequestData(this, RequestData.RequestType.NEW_ALBUM, url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            // TODO - Response codes are now in a ResponseEntity. //
            if (code == 200) {
                Log.i("STATUS", "The new album operation was successful");
            }
            else {
                Log.i("STATUS", "The new album operation was unsuccessful. Unknown error.");
                throw new FailedOperationException();
            }
        }
        catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
