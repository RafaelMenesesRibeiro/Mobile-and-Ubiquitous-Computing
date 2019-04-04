package MobileAndUbiquitousComputing.P2Photos;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import MobileAndUbiquitousComputing.P2Photos.Exceptions.BadInputException;
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
            FindUser.findUser(this, username, false);
        }
        catch (Exception e) {
            // TODO - Deal with this. //
        }

    }
}
