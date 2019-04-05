package MobileAndUbiquitousComputing.P2Photos;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.BadInputException;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedOperationException;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.NoResultsException;
import MobileAndUbiquitousComputing.P2Photos.Helpers.QueryManager;
import MobileAndUbiquitousComputing.P2Photos.Helpers.SessionManager;
import MobileAndUbiquitousComputing.P2Photos.MsgTypes.SuccessResponse;

public class SearchUserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);
    }

    public void SearchUser(View view) throws BadInputException {
        String username = ((EditText) findViewById(R.id.usernameInputBox)).getText().toString();
        if (username.equals("")) {
            throw new BadInputException("The username to find cannot be empty.");
        }
        try {
            // TODO - Design tick box for 'bringAlmbums'. //
            LinkedHashMap<String, ArrayList> usernames = searchUser(username, true);
        }
        catch (NoResultsException nrex) {
            Toast toast = Toast.makeText(this, "No results were found", Toast.LENGTH_LONG);
            toast.show();
        }
        catch (FailedOperationException foex) {
            Toast toast = Toast.makeText(this, "The find users operation failed. Try again later", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public LinkedHashMap<String, ArrayList> searchUser(String usernameToFind, boolean bringAlbums)
            throws FailedOperationException, NoResultsException {
        Log.i("MSG", "Finding user " + usernameToFind + ".");
        String url = getString(R.string.p2photo_host) + getString(R.string.find_users_operation) + "?searchPattern="
                + usernameToFind + "&bringAlbums=" + bringAlbums + "&calleeUsername=" + SessionManager.username;

        try {
            RequestData requestData = new RequestData(this, RequestData.RequestType.SEARCH_USERS, url);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            // TODO - Response codes are now in a ResponseEntity. //
            if (code == 200) {
                Log.i("STATUS", "The find users operation was successful");

                SuccessResponse payload = (SuccessResponse) result.getPayload();
                Object object = payload.getResult();

                if (bringAlbums) {
                    LinkedHashMap<String, ArrayList> map = (LinkedHashMap<String, ArrayList>) object;
                    Log.i("MSG", "Users and respective albums: " + map.toString());
                    if (map.size() == 0) {
                        throw new NoResultsException();
                    }
                    return map;
                }
                else {
                    ArrayList<String> usernames = (ArrayList) object;
                    if (usernames.size() == 0) {
                        throw new NoResultsException();
                    }
                    LinkedHashMap<String, ArrayList> map = new LinkedHashMap<>();
                    for (String username : usernames) {
                        map.put(username, new ArrayList());
                    }
                    return map;
                }
            }
            else {
                Log.i("STATUS", "The find users operation was unsuccessful. Unknown error.");
                throw new FailedOperationException("URL: " + url);
            }
        }
        catch (ExecutionException | InterruptedException | ClassCastException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
