package cmov1819.p2photo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.QueryManager;
import cmov1819.p2photo.helpers.SessionManager;

import static cmov1819.p2photo.ViewAlbumFragment.CATALOG_ID_EXTRA;
import static cmov1819.p2photo.ViewAlbumFragment.NO_ALBUM_SELECTED;
import static cmov1819.p2photo.ViewAlbumFragment.TITLE_EXTRA;

public class MainMenuActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final String START_SCREEN = "initialScreen";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Does not redraw the fragment when the screen rotates.
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent.getStringExtra(START_SCREEN).equals(ViewAlbumFragment.class.getName())) {
                String catalogID = intent.getStringExtra(CATALOG_ID_EXTRA);
                String catalogTitle = intent.getStringExtra(TITLE_EXTRA);
                Fragment viewAlbumFragment = new ViewAlbumFragment();
                Bundle data = new Bundle();
                data.putString(CATALOG_ID_EXTRA, catalogID);
                data.putString(TITLE_EXTRA, catalogTitle);
                viewAlbumFragment.setArguments(data);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, viewAlbumFragment).commit();
                return;
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SearchUserFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_search_user);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_create_album:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new NewAlbumFragment()).commit();
                break;
            case R.id.nav_search_user:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SearchUserFragment()).commit();
                break;
            case R.id.nav_add_photos:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new AddPhotosFragment()).commit();
                break;
            case R.id.nav_new_album_member:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new NewAlbumMemberFragment()).commit();
                break;
            case R.id.nav_view_album:
                Fragment viewAlbumFragment = new ViewAlbumFragment();
                Bundle viewAlbumData = new Bundle();
                viewAlbumData.putString(CATALOG_ID_EXTRA, NO_ALBUM_SELECTED);
                viewAlbumData.putString(TITLE_EXTRA, NO_ALBUM_SELECTED);
                viewAlbumFragment.setArguments(viewAlbumData);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, viewAlbumFragment).commit();
                break;
            case R.id.nav_view_user_albums:
                Fragment viewUserAlbumsFragment = new ViewUserAlbumsFragment();
                Bundle viewUserAlbumsData = new Bundle();
                // TODO - Fetch this in ViewUserAlbumsFragment. //
                ArrayList<String> items = new ArrayList<>(Arrays.asList("239287741","401094244","519782246"));
                viewUserAlbumsData.putStringArrayList("catalogs", items);
                viewUserAlbumsFragment.setArguments(viewUserAlbumsData);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, viewUserAlbumsFragment).commit();
                break;
            case R.id.nav_logout:
                try {
                    logout();
                }
                catch (FailedOperationException ex) {
                    Log.i("ERROR", "LOGOUT: Failed to logout, proceeding");
                }
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                break;
            default:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SearchUserFragment()).commit();
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        // Only closes the drawer, does not go back further.
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    public void logout() throws FailedOperationException {
        String username = SessionManager.getUsername(this);
        Log.i("MSG", "Logout: " + username);

        String url = getString(R.string.p2photo_host) + getString(R.string.logout_operation) + username;
        RequestData rData = new RequestData(this, RequestData.RequestType.LOGOUT, url);
        try {
            ResponseData result = new QueryManager().execute(rData).get();
            int code = result.getServerCode();
            if (code == 200) {
                Log.i("STATUS", "The logout operation was successful");
                SessionManager.deleteSessionID(this);
                SessionManager.deleteUsername(this);
            }
            else {
                Log.i("STATUS", "The login operation was unsuccessful. Unknown error.");
                throw new FailedOperationException();
            }
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FailedOperationException(ex.getMessage());
        }
    }

    public void changeFragment(Fragment fragment, int menuItemID) {
        navigationView.setCheckedItem(menuItemID);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }
}
