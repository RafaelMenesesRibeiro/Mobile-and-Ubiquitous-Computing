package MobileAndUbiquitousComputing.P2Photos.EntryScreens;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import MobileAndUbiquitousComputing.P2Photos.R;

public class LoginScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_log_in_screen);
    }
}
