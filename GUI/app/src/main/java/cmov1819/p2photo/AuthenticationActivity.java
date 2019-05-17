package cmov1819.p2photo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.LogManager;

import static android.widget.Toast.LENGTH_SHORT;

public class AuthenticationActivity extends AppCompatActivity {
    private static final String AUTH_TAG = "AUTHENTICATION";
    private static final String USED_INTENT = "usedIntent";

    private AuthStateManager authStateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        this.authStateManager = AuthStateManager.getInstance(this);

        String msg = "Started Authentication Activity...";
        LogManager.logInfo(AUTH_TAG, msg);
        LogManager.toast(this, "Trying to authenticate");

        Intent appAuthIntent = getIntent();

        if (!appAuthIntent.hasExtra(USED_INTENT)) {
            authStateManager.handleAuthorizationResponse(this, appAuthIntent);
            appAuthIntent.putExtra(USED_INTENT, true);
        }

        Intent mainMenuActivityIntent = new Intent(AuthenticationActivity.this, MainMenuActivity.class);
        startActivity(mainMenuActivityIntent);

        finish();
    }
}
