package MobileAndUbiquitousComputing.P2Photos;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

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
import static android.widget.Toast.LENGTH_LONG;

public class LoginActivity extends AppCompatActivity {
    private static final String LOGIN_TAG = "LOGIN";
    private static final String SIGN_UP_TAG = "SIGN UP";
    private static final String APP_ID = "327056365677-stsv6tntebv1f2jj8agkcr84vrbs3llk.apps.googleusercontent.com";
    private static final Uri REDIRECT_URI = Uri.parse("https://127.0.0.1");

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
    public void onBackPressed() {
        // Do nothing. Prevents going back after logging out.
    }

    /**********************************************************
    * SIGN UP HELPERS
    ***********************************************************/

    public void onSignUpPressed(View view) {
        EditText usernameEditText = findViewById(R.id.usernameInputBox);
        EditText passwordEditText = findViewById(R.id.passwordInputBox);
        String usernameValue = usernameEditText.getText().toString().trim();
        String passwordValue = passwordEditText.getText().toString().trim();
        trySignUp(usernameValue, passwordValue);
    }

    private void trySignUp(String usernameValue, String passwordValue) {
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
        validateGooglelAPIAuthorization();
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

    private static void validateGooglelAPIAuthorization() {
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
