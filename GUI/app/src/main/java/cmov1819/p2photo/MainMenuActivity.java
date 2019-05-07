package cmov1819.p2photo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.architectures.cloudBackedArchitecture.CloudBackedArchitecture;
import cmov1819.p2photo.helpers.managers.ArchitectureManager;
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.helpers.mediators.GoogleDriveMediator;
import cmov1819.p2photo.msgtypes.ErrorResponse;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static cmov1819.p2photo.ListUsersFragment.USERS_EXTRA;
import static cmov1819.p2photo.ViewCatalogFragment.CATALOG_ID_EXTRA;
import static cmov1819.p2photo.ViewCatalogFragment.CATALOG_TITLE_EXTRA;
import static cmov1819.p2photo.ViewCatalogFragment.NO_CATALOG_SELECTED;
import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

public class MainMenuActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final String MAIN_MENU_TAG = "MAIN MENU ACTIVITY";
    public static final String START_SCREEN = "initialScreen";
    public static final String HOME_SCREEN = SearchUserFragment.class.getName();

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private static Resources resources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        MainMenuActivity.resources = getResources();

        ArchitectureManager.systemArchitecture.handlePendingMemberships(this);

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
            goHome();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_create_catalog:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new NewCatalogFragment()).commit();
                break;
            case R.id.nav_search_user:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SearchUserFragment()).commit();
                break;
            case R.id.nav_add_photos:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new AddPhotosFragment()).commit();
                break;
            case R.id.nav_new_catalog_member:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new NewCatalogMemberFragment()).commit();
                break;
            case R.id.nav_view_catalog:
                goToCatalog(NO_CATALOG_SELECTED, NO_CATALOG_SELECTED);
                break;
            case R.id.nav_view_user_catalogs:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ViewUserCatalogsFragment()).commit();
                break;
            case R.id.nav_logout:
                try {
                    logout();
                }
                catch (FailedOperationException ex) {
                    String msg = "Failed to logout, proceeding.";
                    LogManager.logError(MAIN_MENU_TAG, msg);
                }
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_show_app_log:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ViewAppLogFragment()).commit();
                break;
            case R.id.nav_show_server_log:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ViewServerLogFragment()).commit();
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

    /**********************************************************
    * MEMBERSHIP VERIFICATION METHODS
    ***********************************************************/

    public static void handlePendingMembershipsCloudArch(Activity activity) {
        Map<String, String> membershipMapping = ViewUserCatalogsFragment.getMembershipGoogleDriveIDs(activity);
        for (Map.Entry<String, String> entry : membershipMapping.entrySet()) {
            try {
                if (entry.getValue().equals("")) {
                    completeJoinProcessCloudArch(activity, entry.getKey());
                }
            }
            catch (ExecutionException | InterruptedException exc) {
                // pass;
            }
        }
    }

    private static void completeJoinProcessCloudArch(Activity activity, String catalogId) throws ExecutionException, InterruptedException {
        String baseUrl = activity.getString(R.string.p2photo_host) + activity.getString(R.string.view_catalog_details);
        String url = String.format("%s?catalogId=%s&calleeUsername=%s", baseUrl, catalogId, getUsername(activity));
        RequestData requestData = new RequestData(activity, RequestData.RequestType.GET_CATALOG_TITLE, url);
        ResponseData result = new QueryManager().execute(requestData).get();
        int code = result.getServerCode();
        if (code == HttpURLConnection.HTTP_OK) {
            String catalogTitle = (String)((SuccessResponse)result.getPayload()).getResult();
            // TODO - If this is throwing exception, it may be because of the method CloudBackedArchitecture returns these. //
            GoogleDriveMediator googleDriveMediator = ((CloudBackedArchitecture) ArchitectureManager.systemArchitecture).getGoogleDriveMediator(activity);
            AuthStateManager authStateManager = ((CloudBackedArchitecture) ArchitectureManager.systemArchitecture).getAuthStateManager(activity);
            googleDriveMediator.newCatalogSlice(activity, catalogTitle, catalogId, authStateManager.getAuthState());
        }
        else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            String msg = ((ErrorResponse)result.getPayload()).getReason();
            LogManager.logWarning(MAIN_MENU_TAG, msg);
            LogManager.toast(activity, "Session timed out, please login again");
            activity.startActivity(new Intent(activity, LoginActivity.class));
        }
    }

    public static void handlePendingMembershipsWifiDirect(Activity activity) {
        // TODO //
    }

    private static void completeJoinProcessWifiDirect(Activity activity, String catalogId) {
        // TODO //
    }

    /**********************************************************
     * LOGOUT HELPER
     ***********************************************************/

    public void logout() throws FailedOperationException {
        String username = SessionManager.getUsername(this);

        String url = getString(R.string.p2photo_host) + getString(R.string.logout) + username;
        RequestData rData = new RequestData(this, RequestData.RequestType.LOGOUT, url);
        try {
            ResponseData result = new QueryManager().execute(rData).get();
            int code = result.getServerCode();
            if (code == 200) {
                LogManager.logLogout(username);
                SessionManager.deleteSessionID(this);
                SessionManager.deleteUsername(this);
            }
            else {
                String msg = "The login operation was unsuccessful. Unknown error.";
                LogManager.logError(MAIN_MENU_TAG, msg);
                throw new FailedOperationException();
            }
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FailedOperationException(ex.getMessage());
        }
    }

    /**********************************************************
     * SCREEN CHANGE HELPERS
     ***********************************************************/

    private void changeFragment(Fragment fragment, int menuItemID) {
        navigationView.setCheckedItem(menuItemID);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    public void goToListUsers(ArrayList<String> usersList) {
        Fragment listUsersFragment = new ListUsersFragment();
        Bundle data = new Bundle();
        data.putStringArrayList(USERS_EXTRA, usersList);
        listUsersFragment.setArguments(data);
        changeFragment(listUsersFragment, R.id.nav_search_user);
    }

    public void goToAddPhoto(String catalogID) {
        Fragment addPhotoFragment = new AddPhotosFragment();
        Bundle addPhotoData = new Bundle();
        addPhotoData.putString(AddPhotosFragment.CATALOG_ID_EXTRA, catalogID);
        addPhotoFragment.setArguments(addPhotoData);
        changeFragment(addPhotoFragment, R.id.nav_add_photos);
    }

    public void goToAddUser(String catalogID) {
        Fragment newCatalogMemberFragment = new NewCatalogMemberFragment();
        Bundle newCatalogMemberData = new Bundle();
        newCatalogMemberData.putString(NewCatalogMemberFragment.CATALOG_ID_EXTRA, catalogID);
        newCatalogMemberFragment.setArguments(newCatalogMemberData);
        changeFragment(newCatalogMemberFragment, R.id.nav_new_catalog_member);
    }

    public void goToCatalog(String catalogID, String catalogTitle) {
        Fragment viewCatalogFragment = new ViewCatalogFragment();
        Bundle data = new Bundle();
        data.putString(CATALOG_ID_EXTRA, catalogID);
        data.putString(CATALOG_TITLE_EXTRA, catalogTitle);
        viewCatalogFragment.setArguments(data);
        changeFragment(viewCatalogFragment, R.id.nav_view_catalog);
    }

    public void goHome() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SearchUserFragment()).commit();
        navigationView.setCheckedItem(R.id.nav_search_user);
    }

    /**********************************************************
     * BUTTON ACTIVATION / INACTIVATION HELPERS
     ***********************************************************/

    public static void activateButton(Button button) {
        button.setEnabled(true);
        button.setBackgroundColor(MainMenuActivity.resources.getColor(R.color.colorButtonActive));
        button.setTextColor(MainMenuActivity.resources.getColor(R.color.white));
    }

    public static void inactiveButton(Button button) {
        button.setEnabled(false);
        button.setBackgroundColor(MainMenuActivity.resources.getColor(R.color.colorButtonInactive));
        button.setTextColor(MainMenuActivity.resources.getColor(R.color.almostBlack));
    }

    public static void bingEditTextWithButton(final EditText editText, final Button button) {
        inactiveButton(button);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do nothing */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (editText.getText().toString().isEmpty()) {
                    MainMenuActivity.inactiveButton(button);
                    return;
                }
                MainMenuActivity.activateButton(button);
            }

            @Override
            public void afterTextChanged(Editable s) { /* Do nothing */ }
        });
    }
}
