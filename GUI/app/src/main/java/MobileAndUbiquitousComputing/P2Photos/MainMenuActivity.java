package MobileAndUbiquitousComputing.P2Photos;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import static MobileAndUbiquitousComputing.P2Photos.Helpers.Logout.LogoutUser;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
    }

    @Override
    public void onBackPressed() {
        // Do nothing.
        // Prevents going back to the login screen, or the previously completed
        // activity.
    }

    public void viewAlbumClicked(View view) {
        Intent intent = new Intent(this, AlbumViewActivity.class);
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
        // TODO - Design GUI for this. //
    }

    public void AddUsersClicked(View view) {
        // TODO - Design GUI for this. //
    }

    public void ListAlbumsClicked(View view) {
        Intent intent = new Intent(this, ListAlbums.class);
        startActivity(intent);
    }

    public void LogoutClicked(View view) {
        LogoutUser();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
}
