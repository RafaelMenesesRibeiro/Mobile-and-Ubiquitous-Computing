package cmov1819.p2photo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenResponse;

import cmov1819.p2photo.helpers.AuthStateManager;

import static android.widget.Toast.LENGTH_LONG;
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

        Log.i(AUTH_TAG, "Started Authentication Activity...");
        Toast.makeText(this, "Trying to authenticate", LENGTH_SHORT).show();

        Intent appAuthIntent = getIntent();

        if (!appAuthIntent.hasExtra(USED_INTENT)) {
            AuthStateManager.getInstance(this).handleAuthorizationResponse(this, appAuthIntent);
            appAuthIntent.putExtra(USED_INTENT, true);
        }

        Intent mainMenuActivityIntent = new Intent(AuthenticationActivity.this, MainMenuActivity.class);
        mainMenuActivityIntent.putExtra("initialScreen", SearchUserFragment.class.getName());
        startActivity(mainMenuActivityIntent);

        finish();
    }
}
