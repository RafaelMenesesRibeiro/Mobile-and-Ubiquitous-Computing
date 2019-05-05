package cmov1819.p2photo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedLoginException;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.managers.ArchitectureManager;
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.helpers.mediators.GoogleDriveMediator;
import cmov1819.p2photo.msgtypes.ErrorResponse;

import static android.widget.Toast.LENGTH_LONG;
import static cmov1819.p2photo.helpers.managers.SessionManager.updateUsername;

public class LoginActivity extends AppCompatActivity {
    private static final String LOGIN_TAG = "LOGIN";
    private static final String SIGN_UP_TAG = "SIGN UP";

    private AuthStateManager authStateManager;

    BroadcastReceiver restrictionsReceiver;

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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do nothing. */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ActivateButtons(usernameInput, passwordInput, loginButton, signUpButton);
            }

            @Override
            public void afterTextChanged(Editable s) { /* Do nothing */ }
        });

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do nothing. */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ActivateButtons(usernameInput, passwordInput, loginButton, signUpButton);
            }

            @Override
            public void afterTextChanged(Editable s) { /* Do nothing. */ }
        });

        CheckBox checkBox = findViewById(R.id.tickBox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ArchitectureManager.isCloudStorageArchitecture = isChecked;
            }
        });
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
            String msg = "Starting Sign Up operation for user: " + usernameValue + "...";
            LogManager.logInfo(LogManager.SIGN_UP_TAG, msg);

            JSONObject requestBody = new JSONObject();
            requestBody.put("username", usernameValue);
            requestBody.put("password", passwordValue);

            String url = getString(R.string.p2photo_host) + getString(R.string.signup);
            RequestData requestData = new PostRequestData(this, RequestData.RequestType.SIGNUP, url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                msg = "Sign Up successful";
                LogManager.logInfo(LogManager.SIGN_UP_TAG, msg);
                Toast.makeText(getApplicationContext(), "Created account successfully", LENGTH_LONG).show();
                return true;
            } else if (code == HttpURLConnection.HTTP_BAD_REQUEST) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String reason = errorResponse.getReason();
                if (reason.equals(getString(R.string.bad_user))) {
                    msg = "Sign Up unsuccessful. Username does not abide the rules... ";
                    LogManager.logError(LogManager.SIGN_UP_TAG, msg);
                    Toast.makeText(getApplicationContext(), getString(R.string.bad_user), LENGTH_LONG).show();
                } else if (reason.equals(getString(R.string.bad_pass))) {
                    msg = "Sign Up unsuccessful the password does not abide the rules... ";
                    LogManager.logError(LogManager.SIGN_UP_TAG, msg);
                    Toast.makeText(getApplicationContext(), getString(R.string.bad_pass), LENGTH_LONG).show();
                }
            } else if (code == 422) {
                msg = "Sign Up unsuccessful. The chosen username already exists.";
                LogManager.logError(LogManager.SIGN_UP_TAG, msg);
                Toast.makeText(getApplicationContext(), "Chosen username already exists. Choose another...", LENGTH_LONG).show();
            } else {
                msg = "Sign Up unsuccessful. Server response code: " + code;
                LogManager.logError(LogManager.SIGN_UP_TAG, msg);
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

        try {
            tryLogin(usernameValue, passwordValue);
        }
        catch (FailedLoginException | FailedOperationException ex) {
            usernameEditText.setText("");
            passwordEditText.setText("");
            return;
        }

        enableUserTextInputs(usernameEditText, passwordEditText);
        tryEnablingPostAuthorizationFlows(view);
    }

    public void tryLogin(String username, String password) throws FailedLoginException {
        try {
            String msg = "Starting Login operation for user: " + username + "...";
            LogManager.logInfo(LogManager.LOGIN_TAG, msg);

            JSONObject requestBody = new JSONObject();
            requestBody.put("username", username);
            requestBody.put("password", password);

            String url = getString(R.string.p2photo_host) + getString(R.string.login);
            RequestData requestData = new PostRequestData(this, RequestData.RequestType.LOGIN, url, requestBody);
            ResponseData result = new QueryManager().execute(requestData).get();

            int code = result.getServerCode();

            if (code == HttpURLConnection.HTTP_OK) {
                (findViewById(R.id.usernameInputBox)).setVisibility(View.INVISIBLE);
                (findViewById(R.id.passwordInputBox)).setVisibility(View.INVISIBLE);
                msg = "Login operation succeded";
                LogManager.logInfo(LogManager.LOGIN_TAG, msg);
                Toast.makeText(getApplicationContext(), "Welcome " + username, LENGTH_LONG).show();
                updateUsername(this, username);
            }
            else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                msg = "Login operation failed. The username or password are incorrect.";
                LogManager.logError(LogManager.LOGIN_TAG, msg);
                msg = "Incorrect credential combination";
                Toast.makeText(getApplicationContext(), msg, LENGTH_LONG).show();
                throw new FailedLoginException(msg);
            }
            else {
                msg = "Login operation failed. Server error with response code: " + code;
                LogManager.logError(LogManager.LOGIN_TAG, msg);
                msg = "Unexpected error... Try again later";
                Toast.makeText(getApplicationContext(), msg, LENGTH_LONG).show();
                throw new FailedLoginException(msg);
            }
        }
        catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedLoginException(ex.getMessage());
        }
    }

    /**********************************************************
     * GOOGLE API OAUTH HELPERS
     ***********************************************************/

    private void tryEnablingPostAuthorizationFlows(View view) {
        String msg = "Trying to enable post authorization flows...";
        LogManager.logInfo(LogManager.LOGIN_TAG, msg);
        if (authStateManager.hasValidAuthState()) {
            msg = "Valid authentication state >>> starting new MainMenuActivity...";
            LogManager.logInfo(LogManager.LOGIN_TAG, msg);
            Intent mainMenuActivityIntent = new Intent(LoginActivity.this, MainMenuActivity.class);
            mainMenuActivityIntent.putExtra("initialScreen", SearchUserFragment.class.getName());
            startActivity(mainMenuActivityIntent);
        }
        else {
            msg = "Invalid authentication state >>> starting AuthenticationActivity...";
            LogManager.logInfo(LogManager.LOGIN_TAG, msg);
            AuthorizationRequest authorizationRequest = authStateManager.getAuthorizationRequest();
            Intent authenticationIntent = new Intent(this, AuthenticationActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    view.getContext(), authorizationRequest.hashCode(), authenticationIntent, 0
            );
            AuthorizationService authorizationService = new AuthorizationService(view.getContext());
            authorizationService.performAuthorizationRequest(authorizationRequest, pendingIntent);
            authorizationService.dispose();
        }
        finish();
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
