package MobileAndUbiquitousComputing.P2Photos;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

import MobileAndUbiquitousComputing.P2Photos.exceptions.FailedLoginException;
import MobileAndUbiquitousComputing.P2Photos.exceptions.WrongCredentialsException;
import MobileAndUbiquitousComputing.P2Photos.helpers.AuthorizationCodes;
import MobileAndUbiquitousComputing.P2Photos.helpers.LoginManager;

import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.AUTH_ENDPOINT;
import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.TOKEN_ENDPOINT;
import static android.widget.Toast.LENGTH_LONG;

public class LoginActivity extends AppCompatActivity {
    private static final String LOGIN_TAG = "LOGIN";
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

    public void SignUpClicked(View view) {
        EditText usernameEditText = findViewById(R.id.usernameInputBox);
        EditText passwordEditText = findViewById(R.id.passwordInputBox);

        String usernameValue = usernameEditText.getText().toString().trim();
        String passwordValue = passwordEditText.getText().toString().trim();

        Intent intent = new Intent(this, SignUpActivity.class);
        intent.putExtra("username", usernameValue);
        intent.putExtra("password", passwordValue);

        startActivity(intent);
    }

    public void LoginClicked(View view) {
        EditText usernameEditText = findViewById(R.id.usernameInputBox);
        EditText passwordEditText = findViewById(R.id.passwordInputBox);

        String usernameValue = usernameEditText.getText().toString().trim();
        String passwordValue = passwordEditText.getText().toString().trim();

        try {
            LoginManager.login(this, usernameValue, passwordValue);
            Intent intent = new Intent(this, MainMenuActivity.class);
            startActivity(intent);
        } catch (WrongCredentialsException wc) {
            passwordEditText.setText("");
        } catch (FailedLoginException fl) {
            Toast.makeText(this, "Login operation failed...", LENGTH_LONG).show();
        }

        tryObtainOAuthTokens();
    }

    private void tryObtainOAuthTokens() {
        // Declare the authorization and token endpoints of the OAuth server we wish to authorize with
        AuthorizationServiceConfiguration serviceConfiguration =
                new AuthorizationServiceConfiguration(Uri.parse(AUTH_ENDPOINT), Uri.parse(TOKEN_ENDPOINT), null);

        // Describes actual authorization request, including our OAuth APP clientId and the scopes we are requesting
        String clientId = "327056365677-stsv6tntebv1f2jj8agkcr84vrbs3llk.apps.googleusercontent.com";
        Uri redirectUri = Uri.parse("https://127.0.0.1");                   // loopback address
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                serviceConfiguration,
                clientId,
                AuthorizationCodes.RESPONSE_TYPE_CODE,
                redirectUri
        );
        builder.setScopes("profile");                                       // Declare scope here. drive.files, etc.
        AuthorizationRequest request = builder.build();

        // Ideally, there is one instance of AuthorizationService per Activity
        AuthorizationService authorizationService = new AuthorizationService(view.getContext());

        // Create the PendingIntent to handle the authorization response
        String action = "MobileAndUbiquitousComputing.P2Photos.HANDLE_AUTHORIZATION_RESPONSE";
        Intent postAuthorizationIntent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                view.getContext(), request.hashCode(), postAuthorizationIntent, 0
        );
        // Perform authorization request
        authorizationService.performAuthorizationRequest(request, pendingIntent);

            /*
                To add handling for the authorization response. We have to add hooks into the app in order to receive
            the authorization response from the browser.

                First we register RedirectUriReceiverActivity intent-filters in AndroidManifest.xml, inside the
            <application> block. This registers the app to receive the OAuth2 authorization response intent from the
            system browser on our behalf.

                Then, add a new intent-filter to your <activity android:name=".MainActivity"> so AppAuth can pass the
            authorization response to your main activity.

                After updating the Android Manifest file we need to create methods to handle the received intents:
                #onNewIntent
                #checkIntent
                #onStart
            */
    }

    /**********************************************************
    * HELPER METHODS
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
