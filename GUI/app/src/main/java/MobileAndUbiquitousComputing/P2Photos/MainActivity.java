package MobileAndUbiquitousComputing.P2Photos;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import MobileAndUbiquitousComputing.P2Photos.EntryScreens.LoginScreen;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, ListAlbums.class));
    }
}
