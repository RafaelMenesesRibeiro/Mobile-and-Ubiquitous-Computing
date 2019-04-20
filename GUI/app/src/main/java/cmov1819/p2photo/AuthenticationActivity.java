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

        Intent receivedIntent = getIntent();

        if (!receivedIntent.hasExtra(USED_INTENT)) {
            handleAuthorizationResponse(receivedIntent);
            receivedIntent.putExtra(USED_INTENT, true);
        }

        Intent mainMenuActivityIntent = new Intent(AuthenticationActivity.this, MainMenuActivity.class);
        mainMenuActivityIntent.putExtra("initialScreen", SearchUserFragment.class.getName());
        startActivity(mainMenuActivityIntent);

        finish();
    }

    private void handleAuthorizationResponse(Intent intent) {
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        final AuthState authState = new AuthState(response, error);

        Log.i(AUTH_TAG, "Initiating exchange protocol...");
        if (response != null) {
            Log.i(AUTH_TAG, "Handled authorization response " + authState.jsonSerializeString());
            AuthorizationService service = new AuthorizationService(this);
            service.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
                @Override
                public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException error) {
                    String failMessage = "Token exchange failed... Some parts of the application might be unavailable.";
                    if (error != null) {
                        Log.w(AUTH_TAG, failMessage, error);
                        Toast.makeText(getApplicationContext(), failMessage, LENGTH_LONG).show();
                    } else {
                        if (tokenResponse != null) {
                            authState.update(tokenResponse, error);
                            authStateManager.setAuthState(authState);
                            authStateManager.persistAuthState();
                            Log.i(AUTH_TAG, String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                            Toast.makeText(getApplicationContext(), "Authentication complete", LENGTH_LONG).show();
                        }
                        else {
                            Log.i(AUTH_TAG, "Could not obtain OAuth Token from response");
                            Toast.makeText(getApplicationContext(), failMessage, LENGTH_LONG).show();
                        }
                    }
                }

            });
            service.dispose();
            finish();
        }
    }
}
