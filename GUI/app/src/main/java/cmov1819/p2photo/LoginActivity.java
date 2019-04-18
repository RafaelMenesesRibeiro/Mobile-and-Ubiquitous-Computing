package cmov1819.p2photo;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedLoginException;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.AuthStateManager;
import cmov1819.p2photo.helpers.QueryManager;
import cmov1819.p2photo.msgtypes.ErrorResponse;

import static android.widget.Toast.LENGTH_LONG;
import static cmov1819.p2photo.helpers.SessionManager.updateUsername;

public class LoginActivity extends AppCompatActivity {
    private static final String USED_INTENT = "used";
    private static final String LOGIN_TAG = "LOGIN";
    private static final String SIGN_UP_TAG = "SIGN UP";
    private static final String AUTH_REQUEST_TAG = "OAUTH";
    private static final String AUTH_RESPONSE_ACTION = "cmov1819.p2photo.HANDLE_AUTHORIZATION_RESPONSE";

    private AuthStateManager authStateManager;

    @Override
    public void onBackPressed() {
        // Do nothing. Prevents going back after logging out.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);
        this.authStateManager = AuthStateManager.getInstance(this);
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
                case AUTH_RESPONSE_ACTION:
                    // If this intent hasn't been processed yet, process it.
                    if (!intent.hasExtra(USED_INTENT)) {
                        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
                        AuthorizationException error = AuthorizationException.fromIntent(intent);
                        authStateManager.handleAuthorizationResponse(response, error);
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
            disableUserTextInputs(usernameEditText, passwordEditText);
            findViewById(R.id.LoginButton).performClick();
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
        tryLogin(usernameValue, passwordValue);
        enableUserTextInputs(usernameEditText, passwordEditText);
        tryEnablingPostAuthorizationFlows();
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
                (findViewById(R.id.usernameInputBox)).setVisibility(View.INVISIBLE);
                (findViewById(R.id.passwordInputBox)).setVisibility(View.INVISIBLE);
                Log.i(LOGIN_TAG, "Login operation succeded");
                Toast.makeText(getApplicationContext(), "Welcome " + username, LENGTH_LONG).show();
                updateUsername(this, username);
            }
            else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Log.i(LOGIN_TAG, "Login operation failed. The username or password are incorrect.");
                Toast.makeText(getApplicationContext(), "Incorrect credential combination", LENGTH_LONG).show();
            }
            else {
                Log.i(LOGIN_TAG,"Login operation failed. Server error with response code: " + code);
                Toast.makeText(getApplicationContext(), "Unexpected error... Try again later", LENGTH_LONG).show();
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
    private void tryEnablingPostAuthorizationFlows() {
            if (authStateManager.hasValidAuthState()) {
                startActivity(new Intent(LoginActivity.this, MainMenuActivity.class));
            }
            else {
                Intent postAuthIntent = new Intent(AUTH_RESPONSE_ACTION);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        this, authStateManager.getAuthorizationRequestCode(), postAuthIntent, 0
                );
                authStateManager.getAuthorizationService().performAuthorizationRequest(
                        authStateManager.getAuthorizationRequest(), pendingIntent
                );
            }
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

    private void disableUserTextInputs(EditText usernameEditText, EditText passwordEditText) {
        usernameEditText.setFocusable(false);
        usernameEditText.setClickable(false);
        passwordEditText.setFocusable(false);
        passwordEditText.setClickable(false);
    }

    private void enableUserTextInputs(EditText usernameEditText, EditText passwordEditText) {
        usernameEditText.setFocusable(true);
        usernameEditText.setClickable(true);
        passwordEditText.setFocusable(true);
        passwordEditText.setClickable(true);
    }

}
