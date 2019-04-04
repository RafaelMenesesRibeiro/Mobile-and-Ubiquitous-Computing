package MobileAndUbiquitousComputing.P2Photos;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import MobileAndUbiquitousComputing.P2Photos.Exceptions.BadInputException;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedOperationException;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.NoResultsException;
import MobileAndUbiquitousComputing.P2Photos.Helpers.FindUser;

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
            LinkedHashMap<String, ArrayList> usernames = FindUser.findUser(this, username, true);
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
}
