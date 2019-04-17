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
import MobileAndUbiquitousComputing.P2Photos.exceptions.WrongCredentialsException;
import MobileAndUbiquitousComputing.P2Photos.helpers.AppContext;
import MobileAndUbiquitousComputing.P2Photos.helpers.AuthorizationCodes;
import MobileAndUbiquitousComputing.P2Photos.helpers.QueryManager;
import MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager;

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

    public void onSignUpPressed(View view) {
        EditText usernameEditText = findViewById(R.id.usernameInputBox);
        EditText passwordEditText = findViewById(R.id.passwordInputBox);

        String usernameValue = usernameEditText.getText().toString().trim();
        String passwordValue = passwordEditText.getText().toString().trim();

        Intent intent = new Intent(this, SignUpActivity.class);
        intent.putExtra("username", usernameValue);
        intent.putExtra("password", passwordValue);

        startActivity(intent);
    }

    public void onLoginPressed(View view) {
        EditText usernameEditText = findViewById(R.id.usernameInputBox);
        EditText passwordEditText = findViewById(R.id.passwordInputBox);

        String usernameValue = usernameEditText.getText().toString().trim();
        String passwordValue = passwordEditText.getText().toString().trim();

        try {
            tryLogin(this, usernameValue, passwordValue);
            Intent intent = new Intent(this, MainMenuActivity.class);
            startActivity(intent);
        } catch (WrongCredentialsException wc) {
            passwordEditText.setText("");
        } catch (FailedLoginException fl) {
            Toast.makeText(this, "Login operation failed...", LENGTH_LONG).show();
        }

        validateGooglelAPIAuthorization();
    }

    public static void tryLogin(Activity activity, String username, String password) throws FailedLoginException {
        Log.i(LOGIN_TAG, "Starting tryLogin operation for username: " + username + "...");

        try {
            String url = activity.getString(R.string.p2photo_host) + activity.getString(R.string.login_operation);

            JSONObject requestBody = new JSONObject();
            requestBody.put("username", username);
            requestBody.put("password", password);

            RequestData requestData = new PostRequestData(activity, RequestData.RequestType.LOGIN, url, requestBody);
            ResponseData result = new QueryManager().execute(requestData).get();

            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                Log.i(LOGIN_TAG, "Login operation succeded");
                SessionManager.updateUserName(activity, username);
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Log.i(LOGIN_TAG, "Login operation failed. The username or password are incorrect.");
                throw new WrongCredentialsException();
            } else {
                Log.i(LOGIN_TAG,"Login operation failed. Server error with response code: " + code);
                throw new FailedLoginException();
            }

        } catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedLoginException(ex.getMessage());
        }
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

    /**********************************************************
    * HELPERS
    ***********************************************************/

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
     * UI Related code
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
