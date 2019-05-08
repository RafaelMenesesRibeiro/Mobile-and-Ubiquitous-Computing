package cmov1819.p2photo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.security.SignatureException;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedLoginException;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.CryptoUtils;
import cmov1819.p2photo.helpers.architectures.cloudBackedArchitecture.CloudBackedArchitecture;
import cmov1819.p2photo.helpers.managers.ArchitectureManager;
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.msgtypes.ErrorResponse;

import static cmov1819.p2photo.helpers.managers.SessionManager.updateUsername;

public class LoginActivity extends AppCompatActivity {
    @Override
    public void onBackPressed() {
        // Do nothing. Prevents going back after logging out.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        ArchitectureManager.setWirelessP2PArch(); // Default architecture is Wireless P2P.
        CheckBox checkBox = findViewById(R.id.tickBox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ArchitectureManager.setCloudBackedArch();
                    return;
                }
                ArchitectureManager.setWirelessP2PArch();
            }
        });

        populate();
    }

    private void populate() {
        final Button loginButton = findViewById(R.id.LoginButton);
        final EditText usernameInput = findViewById(R.id.usernameInputBox);
        final EditText passwordInput = findViewById(R.id.passwordInputBox);

        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do nothing. */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                activateButtons(usernameInput, passwordInput, loginButton);
            }

            @Override
            public void afterTextChanged(Editable s) { /* Do nothing */ }
        });

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do nothing. */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                activateButtons(usernameInput, passwordInput, loginButton);
            }

            @Override
            public void afterTextChanged(Editable s) { /* Do nothing. */ }
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

        if (usernameValue.equals("") || passwordValue.equals("")) {
            LogManager.toast(this, "Fill in username and password");
            return;
        }

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
                LogManager.toast(this, "Created account successfully");
                return true;
            } else if (code == HttpURLConnection.HTTP_BAD_REQUEST) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String reason = errorResponse.getReason();
                if (reason.equals(getString(R.string.bad_user))) {
                    msg = "Sign Up unsuccessful. Username does not abide the rules... ";
                    LogManager.logError(LogManager.SIGN_UP_TAG, msg);
                    LogManager.toast(this, getString(R.string.bad_user));
                }
                else if (reason.equals(getString(R.string.bad_pass))) {
                    msg = "Sign Up unsuccessful the password does not abide the rules... ";
                    LogManager.logError(LogManager.SIGN_UP_TAG, msg);
                    LogManager.toast(this, getString(R.string.bad_pass));
                }
            }
            else if (code == 422) {
                msg = "Sign Up unsuccessful. The chosen username already exists.";
                LogManager.logError(LogManager.SIGN_UP_TAG, msg);
                LogManager.toast(this, "Chosen username already exists. Choose another...");
            }
            else {
                msg = "Sign Up unsuccessful. Server response code: " + code;
                LogManager.logError(LogManager.SIGN_UP_TAG, msg);
                LogManager.toast(this, "Unexpected error, try later...");
            }

            return false;

        }
        catch (JSONException ex) {
            String msg = "JSONException: " + ex.getMessage();
            LogManager.logError(LogManager.SIGN_UP_TAG, msg);
            throw new FailedOperationException(ex.getMessage());
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            String msg = "Sign Up unsuccessful. " + ex.getMessage();
            LogManager.logError(LogManager.SIGN_UP_TAG, msg);
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
        try {
            ArchitectureManager.systemArchitecture.setup(view, this);
        }
        catch (FailedOperationException ex) {
            // TODO - Is this the best way? //
            LogManager.logError(LogManager.LOGIN_TAG, ex.getMessage());
            System.exit(-1);
        }
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
                LogManager.toast(this, "Welcome " + username);
                updateUsername(this, username);
            }
            else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                msg = "Login operation failed. The username or password are incorrect.";
                LogManager.logError(LogManager.LOGIN_TAG, msg);
                LogManager.toast(this, "Incorrect credential combination");
                throw new FailedLoginException(msg);
            }
            else {
                msg = "Login operation failed. Server error with response code: " + code;
                LogManager.logError(LogManager.LOGIN_TAG, msg);
                LogManager.toast(this, "Unexpected error... Try again later");
                throw new FailedLoginException(msg);
            }
        }
        catch (JSONException ex) {
            String msg = "JSONException: " + ex.getMessage();
            LogManager.logError(LogManager.LOGIN_TAG, msg);
            throw new FailedOperationException(ex.getMessage());
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            String msg = "Login unsuccessful. " + ex.getMessage();
            LogManager.logError(LogManager.LOGIN_TAG, msg);
            throw new FailedOperationException(ex.getMessage());
        }
    }

    public static void goHome(Activity activity) {
        Intent mainMenuActivityIntent = new Intent(activity, MainMenuActivity.class);
        mainMenuActivityIntent.putExtra("initialScreen", SearchUserFragment.class.getName());
        activity.startActivity(mainMenuActivityIntent);
    }

    /**********************************************************
     * WIRELESS P2P ARCHITECTURE SETUP HELPERS
     ***********************************************************/

    public static void initializeSymmetricKey(Activity activity) throws SignatureException {
        CryptoUtils.initializeSymmetricKey();
        goHome(activity);
    }

    /**********************************************************
     * GOOGLE API OAUTH HELPERS
     ***********************************************************/

    public static void tryEnablingPostAuthorizationFlows(View view, Activity activity) {
        String msg = "Trying to enable post authorization flows...";
        LogManager.logInfo(LogManager.LOGIN_TAG, msg);
        AuthStateManager authStateManager  = ((CloudBackedArchitecture) ArchitectureManager.systemArchitecture).getAuthStateManager(activity);
        if (authStateManager.hasValidAuthState()) {
            msg = "Valid authentication state >>> starting new MainMenuActivity...";
            LogManager.logInfo(LogManager.LOGIN_TAG, msg);
            goHome(activity);
        }
        else {
            msg = "Invalid authentication state >>> starting AuthenticationActivity...";
            LogManager.logInfo(LogManager.LOGIN_TAG, msg);
            AuthorizationRequest authorizationRequest = authStateManager.getAuthorizationRequest();
            Intent authenticationIntent = new Intent(activity, AuthenticationActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    view.getContext(), authorizationRequest.hashCode(), authenticationIntent, 0
            );
            AuthorizationService authorizationService = new AuthorizationService(view.getContext());
            authorizationService.performAuthorizationRequest(authorizationRequest, pendingIntent);
            authorizationService.dispose();
        }
        activity.finish();
    }

    /**********************************************************
     * UI HELPERS
     ***********************************************************/

    private void activateButtons(EditText usernameInput, EditText passwordInput, Button loginButton) {
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
