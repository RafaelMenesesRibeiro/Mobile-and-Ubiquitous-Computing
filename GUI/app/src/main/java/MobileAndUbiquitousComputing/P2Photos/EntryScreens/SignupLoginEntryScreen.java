package MobileAndUbiquitousComputing.P2Photos.EntryScreens;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import MobileAndUbiquitousComputing.P2Photos.R;

public class SignupLoginEntryScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_login_entry_screen);
    }

    public void LogInButtonClicked(View view) {
        System.out.println("Log In Button clicked.");
        startActivity(new Intent(this, LogInScreen.class));
    }
}
