package cmov1819.p2photo;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedLoginException;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.AppContext;
import cmov1819.p2photo.helpers.QueryManager;
import cmov1819.p2photo.helpers.SessionManager;
import cmov1819.p2photo.msgtypes.ErrorResponse;

import static android.widget.Toast.LENGTH_LONG;
import static cmov1819.p2photo.helpers.SessionManager.AUTH_ENDPOINT;
import static cmov1819.p2photo.helpers.SessionManager.TOKEN_ENDPOINT;
import static cmov1819.p2photo.helpers.SessionManager.persistAuthState;

public class LoginActivity extends AppCompatActivity {
    private static final String USED_INTENT = "used";
    private static final String LOGIN_TAG = "LOGIN";
    private static final String SIGN_UP_TAG = "SIGN UP";
    private static final String AUTH_REQUEST_TAG = "OAUTH";
    private static final String APP_ID = "327056365677-stsv6tntebv1f2jj8agkcr84vrbs3llk.apps.googleusercontent.com";
    private static final Uri REDIRECT_URI = Uri.parse("https://127.0.0.1");

    @Override
    public void onBackPressed() {
        // Do nothing. Prevents going back after logging out.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        final Button loginButton = findViewById(R.id.LoginButton);
        final Button signUpButton = findViewById(R.id.SignUpBottom);
        final EditText usernameInput = findViewById(R.id.usernameInputBox);
        final EditText passwordInput = findViewById(R.id.passwordInputBox);

        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /*
            Do nothing. */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ActivateButtons(usernameInput, passwordInput, loginButton, signUpButton);
            }

            @Override
            public void afterTextChanged(Editable s) { /* Do nothing */ }
        });

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /*
            Do nothing. */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ActivateButtons(usernameInput, passwordInput, loginButton, signUpButton);
            }

            @Override
            public void afterTextChanged(Editable s) { /* Do nothing. */ }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        checkIntent(intent);
    }

    private void checkIntent(@Nullable Intent intent) {
        Log.i(AUTH_REQUEST_TAG, "Checking intent...");
        if (intent != null) {
            String action = intent.getAction();
            Log.i(AUTH_REQUEST_TAG, "Found intent, with action action: " + action + "...");
            switch (action) {
                case "cmov1819.p2photo.HANDLE_AUTHORIZATION_RESPONSE":
                    // If this intent hasn't been processed yet, process it.
                    if (!intent.hasExtra(USED_INTENT)) {
                        handleAuthorizationResponse(intent);
                        intent.putExtra(USED_INTENT, true);
                    }
                    break;
                default:
                    break;
            }
        }
        Log.i(AUTH_REQUEST_TAG, "Null intent... Continueing");
    }

    /**********************************************************
     * SIGN UP HELPERS
     ***********************************************************/

    public void onSignUpPressed(View view) {
        EditText usernameEditText = findViewById(R.id.usernameInputBox);
        EditText passwordEditText = findViewById(R.id.passwordInputBox);
        String usernameValue = usernameEditText.getText().toString().trim();
        String passwordValue = passwordEditText.getText().toString().trim();
        if (trySignUp(usernameValue, passwordValue)) {
            usernameEditText.setVisibility(View.INVISIBLE);
            passwordEditText.setVisibility(View.INVISIBLE);
            tryLogin(usernameValue, passwordValue);
        }
    }

    private boolean trySignUp(String usernameValue, String passwordValue) {
        try {
            Log.i(SIGN_UP_TAG, "Starting Sign Up operation for user: " + usernameValue + "...");

            JSONObject requestBody = new JSONObject();
            requestBody.put("username", usernameValue);
            requestBody.put("password", passwordValue);

            String url = getString(R.string.p2photo_host) + getString(R.string.signup_operation);
            RequestData requestData = new PostRequestData(this, RequestData.RequestType.SIGNUP, url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                Log.i(SIGN_UP_TAG, "Sign Up successful");
                Toast.makeText(getApplicationContext(), "Created account successfully", LENGTH_LONG).show();
                return true;
            } else if (code == HttpURLConnection.HTTP_BAD_REQUEST) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String reason = errorResponse.getReason();
                if (reason.equals(getString(R.string.bad_user))) {
                    Log.i(SIGN_UP_TAG, "Sign Up unsuccessful. Username does not abide the rules... ");
                    Toast.makeText(getApplicationContext(), getString(R.string.bad_user), LENGTH_LONG).show();
                } else if (reason.equals(getString(R.string.bad_pass))) {
                    Log.i(SIGN_UP_TAG,"Sign Up unsuccessful the password does not abide the rules... ");
                    Toast.makeText(getApplicationContext(), getString(R.string.bad_pass), LENGTH_LONG).show();
                }
            } else if (code == 422) {
                Log.i(SIGN_UP_TAG, "Sign Up unsuccessful. The chosen username already exists.");
                Toast.makeText(getApplicationContext(), "Chosen username already exists. Choose another...", LENGTH_LONG).show();
            } else {
                Log.i(SIGN_UP_TAG,"Sign Up unsuccessful. Server response code: " + code);
                Toast.makeText(getApplicationContext(), "Unexpected error, try later...", LENGTH_LONG).show();
            }

            return false;

        } catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }

    /**********************************************************
     * LOGIN HELPERS
     ***********************************************************/

    public void onLoginPressed(View view) {
        EditText usernameEditText = findViewById(R.id.usernameInputBox);
        EditText passwordEditText = findViewById(R.id.passwordInputBox);
        String usernameValue = usernameEditText.getText().toString().trim();
        String passwordValue = passwordEditText.getText().toString().trim();
        if (tryLogin(usernameValue, passwordValue)) {
            Intent intent = new Intent(LoginActivity.this, MainMenuActivity.class);
            intent.putExtra("initialScreen", SearchUserFragment.class.getName());
            startActivity(intent);
        }
    }

    public boolean tryLogin(String username, String password) throws FailedLoginException {
        try {
            Log.i(LOGIN_TAG, "Starting Login operation for user: " + username + "...");

            JSONObject requestBody = new JSONObject();
            requestBody.put("username", username);
            requestBody.put("password", password);

            String url = getString(R.string.p2photo_host) + getString(R.string.login_operation);
            RequestData requestData = new PostRequestData(this, RequestData.RequestType.LOGIN, url, requestBody);
            ResponseData result = new QueryManager().execute(requestData).get();

            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                Log.i(LOGIN_TAG, "Login operation succeded");
                Toast.makeText(getApplicationContext(), "Welcome " + username, LENGTH_LONG).show();
                SessionManager.updateUsername(this, username);
                return true;
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Log.i(LOGIN_TAG, "Login operation failed. The username or password are incorrect.");
                Toast.makeText(getApplicationContext(), "Incorrect credential combination", LENGTH_LONG).show();
            } else {
                Log.i(LOGIN_TAG,"Login operation failed. Server error with response code: " + code);
                Toast.makeText(getApplicationContext(), "Unexpected error... Try again later", LENGTH_LONG).show();
            }

            return false;

        } catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedLoginException(ex.getMessage());
        }

        // tryAuthorizeDriveManagement();
    }

    /**********************************************************
     * GOOGLE API OAUTH HELPERS
     ***********************************************************/

    private void tryAuthorizeDriveManagement() {
        if (existsPersistedAuthState()) {
            tryRefreshAuthStateTokens();
        } else {
            tryGetAuthState();
        }
    }

    private boolean existsPersistedAuthState() {
        return false;
    }

    private void tryRefreshAuthStateTokens() {
        // TODO
    }

    /*
     * Generate an authorization request with scopes the user should authorize this app to manage;
     * Ideally, there is one instance of AuthorizationService per Activity;
     * PendingIntent is used to handle the authorization request response
     */
    private void tryGetAuthState() {
        AuthorizationRequest authRequest = newAuthorizationRequest();

        AuthorizationService authorizationService = new AuthorizationService(this);

        Intent postAuthorizationIntent = new Intent("cmov1819.p2photo.HANDLE_AUTHORIZATION_RESPONSE");
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, authRequest.hashCode(), postAuthorizationIntent, 0
        );
        authorizationService.performAuthorizationRequest(authRequest, pendingIntent);
    }

    /*
     * Describes an authorization request, including the application clientId for the OAuth and the respective scopes
     */
    private AuthorizationRequest newAuthorizationRequest() {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                new AuthorizationServiceConfiguration(Uri.parse(AUTH_ENDPOINT), Uri.parse(TOKEN_ENDPOINT), null),
                APP_ID,
                ResponseTypeValues.CODE,
                REDIRECT_URI
        );
        builder.setScopes(new ArrayList<>(Arrays.asList(
                "email", "profile", "openid",
                "https://www.googleapis.com/auth/drive.photos.readonly",
                "https://www.googleapis.com/auth/drive.metadata.readonly",
                "https://www.googleapis.com/auth/drive.readonly",
                "https://www.googleapis.com/auth/drive.file",
                "https://www.googleapis.com/auth/drive.appdata"
        )));
        return builder.build();
    }

    /*
     * AppAuth provides the AuthorizationResponse to this activity, via the provided RedirectUriReceiverActivity.
     * From it we can ultimately obtain a TokenResponse which we can use to make calls to the API;
     * The AuthState object that is created from the response can be used to store details from the auth session to
     * reuse it between application runs and it may be changed overtime as new OAuth results are received.
     */
    private void handleAuthorizationResponse(@NonNull Intent intent) {
        // The authorization response is provided to this activity via Intent extra data, extract it with fromIntent()
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        // Provided the response for an AuthState instance for easy persistence and further processing
        final AuthState authState = new AuthState(response, error);
        // Exchange authorization code for the refresh and access tokens, and update the AuthState instance
        if (response != null) {
            Log.i(AUTH_REQUEST_TAG, "Handled authorization response: " + authState.jsonSerialize().toString());
            tryExchangeAuthCodeForAuthTokens(response, authState);
        } else {
            Log.i(AUTH_REQUEST_TAG, "Authorization failed with error: " + error.getMessage());
            String msg = "You must authorize this app to manage some google drive files to use";
            Toast.makeText(this, msg , LENGTH_LONG).show();
        }
    }

    private void tryExchangeAuthCodeForAuthTokens(AuthorizationResponse response, final AuthState authState) {
        Log.i(AUTH_REQUEST_TAG, "Initiating exchange protocol...");
        AuthorizationService authService = new AuthorizationService(this);
        authService.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
            @Override
            public void onTokenRequestCompleted(@Nullable TokenResponse token, @Nullable AuthorizationException exc) {
                if (exc != null) {
                    Log.w(AUTH_REQUEST_TAG, "Token exchange authorization failed", exc);
                    String msg = "Could not exchange auth code for an auth token, some features might be unavailable";
                    Toast.makeText(getApplicationContext(), msg, LENGTH_LONG).show();
                } else {
                    if (token != null) {
                        authState.update(token, exc);
                        persistAuthState(authState, LoginActivity.this);
                        Log.i(AUTH_REQUEST_TAG, "Token Response [ Access Token: " + token.accessToken + ", ID Token: " + token.idToken + " ]");
                    } else {
                        Log.w(AUTH_REQUEST_TAG, "Received token is null");
                        String msg = "Could not exchange auth code for an auth token, some features might be unavailable";
                        Toast.makeText(getApplicationContext(), msg, LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    /**********************************************************
     * UI HELPERS
     ***********************************************************/

    private void ActivateButtons(EditText usernameInput, EditText passwordInput, Button loginButton, Button signupButton) {
        if (!usernameInput.getText().toString().isEmpty() && !passwordInput.getText().toString().isEmpty()) {
            loginButton.setEnabled(true);
            loginButton.setBackgroundColor(getResources().getColor(R.color.colorButtonActive));
            loginButton.setTextColor(getResources().getColor(R.color.white));
        } else {
            loginButton.setEnabled(false);
            loginButton.setBackgroundColor(getResources().getColor(R.color.colorButtonInactive));
            loginButton.setTextColor(getResources().getColor(R.color.almostBlack));
        }
    }

}
