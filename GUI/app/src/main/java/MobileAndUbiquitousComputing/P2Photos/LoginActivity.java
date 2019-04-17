package MobileAndUbiquitousComputing.P2Photos;

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
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.dataobjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.exceptions.FailedLoginException;
import MobileAndUbiquitousComputing.P2Photos.exceptions.FailedOperationException;
import MobileAndUbiquitousComputing.P2Photos.helpers.AppContext;
import MobileAndUbiquitousComputing.P2Photos.helpers.AuthorizationCodes;
import MobileAndUbiquitousComputing.P2Photos.helpers.QueryManager;
import MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager;
import MobileAndUbiquitousComputing.P2Photos.msgtypes.ErrorResponse;

import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.AUTH_ENDPOINT;
import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.TOKEN_ENDPOINT;
import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.persistAuthState;
import static android.widget.Toast.LENGTH_LONG;

public class LoginActivity extends AppCompatActivity {
    private static final String LOGIN_TAG = "LOGIN";
    private static final String SIGN_UP_TAG = "SIGN UP";
    private static final String AUTH_REQUEST_TAG = "API AUTH REQUEST";
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
        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case "MobileAndUbiquitousComputing.P2Photos.HANDLE_AUTHORIZATION_RESPONSE":
                    if (!intent.hasExtra(AppContext.USED_INTENT)) {
                        handleAuthorizationResponse(intent);
                        intent.putExtra(AppContext.USED_INTENT, true);
                    }
                    break;
                default:
                    break;
            }
        }
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
                return false;
            } else if (code == 422) {
                Log.i(SIGN_UP_TAG, "Sign Up unsuccessful. The chosen username already exists.");
                Toast.makeText(getApplicationContext(), "Chosen username already exists. Choose another...", LENGTH_LONG).show();
                return false;
            } else {
                Log.i(SIGN_UP_TAG,"Sign Up unsuccessful. Server response code: " + code);
                Toast.makeText(getApplicationContext(), "Unexpected error, try later...", LENGTH_LONG).show();
                return false;
            }
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
        tryLogin(usernameValue, passwordValue);
        tryAuthorizeDriveManagement();
    }

    public void tryLogin(String username, String password) throws FailedLoginException {
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
                SessionManager.updateUserName(this, username);
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Log.i(LOGIN_TAG, "Login operation failed. The username or password are incorrect.");
                Toast.makeText(getApplicationContext(), "Incorrect credential combination", LENGTH_LONG).show();
            } else {
                Log.i(LOGIN_TAG,"Login operation failed. Server error with response code: " + code);
            }

        } catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedLoginException(ex.getMessage());
        }
    }

    /**********************************************************
     * GOOGLE API OAUTH HELPERS
     ***********************************************************/

    private static void tryAuthorizeDriveManagement() {
        if (existsPersistedAuthState()) {
            tryRefreshAuthStateTokens();
        } else {
            tryGetAuthState();
        }
    }

    private static boolean existsPersistedAuthState() {
        return false;
    }

    private static void tryRefreshAuthStateTokens() {
        // TODO
    }

    /*
     * Generate an authorization request with scopes the user should authorize this app to manage;
     * Ideally, there is one instance of AuthorizationService per Activity;
     * PendingIntent is used to handle the authorization request response
     */
    private static void tryGetAuthState() {
        Context currentContext = AppContext.getAppContext();
        AuthorizationRequest request = newAuthorizationRequest();
        AuthorizationService authorizationService = new AuthorizationService(currentContext);
        String action = "MobileAndUbiquitousComputing.P2Photos.HANDLE_AUTHORIZATION_RESPONSE";
        Intent postAuthorizationIntent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getActivity(currentContext, request.hashCode(), postAuthorizationIntent, 0);
        authorizationService.performAuthorizationRequest(request, pendingIntent);
    }

    /*
     * Describes an authorization request, including the application clientId for the OAuth and the respective scopes
     */
    private static AuthorizationRequest newAuthorizationRequest() {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                new AuthorizationServiceConfiguration(Uri.parse(AUTH_ENDPOINT), Uri.parse(TOKEN_ENDPOINT), null),
                APP_ID,
                AuthorizationCodes.RESPONSE_TYPE_CODE,
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
        // Obtain the AuthorizationResponse from the Intent
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        final AuthState authState = new AuthState(response, error);
        // Exchange authorization code for the refresh and access tokens, and update the AuthState instance
        if (response != null) {
            Log.i(AUTH_REQUEST_TAG, "Handled Authorization Response " + authState.jsonSerialize().toString());
            AuthorizationService service = new AuthorizationService(this);
            service.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
                @Override
                public void onTokenRequestCompleted(@Nullable TokenResponse token, @Nullable AuthorizationException exc) {
                    if (exc != null) {
                        Log.w(AUTH_REQUEST_TAG, "Token Exchange failed", exc);
                    } else {
                        if (token != null) {
                            authState.update(token, exc);
                            persistAuthState(authState, LoginActivity.this);
                            enablePostAuthorizationFlows();
                            Log.i(AUTH_REQUEST_TAG, "Token Response [ Access Token: " + token.accessToken + ", ID Token: " + token.idToken + " ]");
                        }
                    }
                }
            });
        }
    }
    private void enablePostAuthorizationFlows() {
        // TODO
        /*
        mAuthState = restoreAuthState(MainMenuActivity.this);
        if (mAuthState != null && mAuthState.isAuthorized()) {
            if (mMakeApiCall.getVisibility() == View.GONE) {
                mMakeApiCall.setVisibility(View.VISIBLE);
                mMakeApiCall.setOnClickListener(new MakeApiCallListener(this, mAuthState, new AuthorizationService(this)));
            }
            if (mSignOut.getVisibility() == View.GONE) {
                mSignOut.setVisibility(View.VISIBLE);
                mSignOut.setOnClickListener(new SignOutListener(this));
            }
        } else {
            mMakeApiCall.setVisibility(View.GONE);
            mSignOut.setVisibility(View.GONE);
        }
        */
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
