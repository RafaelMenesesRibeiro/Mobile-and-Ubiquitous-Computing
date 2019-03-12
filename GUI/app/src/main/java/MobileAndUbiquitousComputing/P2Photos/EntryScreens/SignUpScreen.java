package MobileAndUbiquitousComputing.P2Photos.EntryScreens;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import MobileAndUbiquitousComputing.P2Photos.R;

public class SignUpScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_sign_up_screen);
    }
}
