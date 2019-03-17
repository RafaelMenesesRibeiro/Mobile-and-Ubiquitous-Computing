package MobileAndUbiquitousComputing.P2Photos;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import MobileAndUbiquitousComputing.P2Photos.Helpers.Login;
import MobileAndUbiquitousComputing.P2Photos.R;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        final Button loginButton = findViewById(R.id.LoginButton);
        final Button signupButton = findViewById(R.id.SignUpBottom);
        final EditText usernameInput = findViewById(R.id.usernameInputBox);
        final EditText passwordInput = findViewById(R.id.passwordInputBox);

        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do nothing. */ }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ActivateButtons(usernameInput, passwordInput, loginButton, signupButton);
            }
            @Override
            public void afterTextChanged(Editable s) { /* Do nothing */ }
        });

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do nothing. */ }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ActivateButtons(usernameInput, passwordInput, loginButton, signupButton);
            }
            @Override
            public void afterTextChanged(Editable s) { /* Do nothing. */ }
        });
    }

    @Override
    public void onBackPressed() {
        // Do nothing.
        // Prevents going back after logging out.
    }

    private void ActivateButtons(EditText usernameInput, EditText passwordInput, Button loginButton, Button signupButton) {
        if (!usernameInput.getText().toString().isEmpty() && !passwordInput.getText().toString().isEmpty()) {
            loginButton.setClickable(true);
            loginButton.setBackgroundColor(getResources().getColor(R.color.colorButtonActive));
            loginButton.setTextColor(getResources().getColor(R.color.white));
            signupButton.setClickable(true);
            signupButton.setBackgroundColor(getResources().getColor(R.color.colorButtonActive));
            signupButton.setTextColor(getResources().getColor(R.color.white));
        }
        else {
            loginButton.setClickable(false);
            loginButton.setBackgroundColor(getResources().getColor(R.color.colorButtonInactive));
            loginButton.setTextColor(getResources().getColor(R.color.almostBlack));
            signupButton.setClickable(false);
            signupButton.setBackgroundColor(getResources().getColor(R.color.colorButtonInactive));
            signupButton.setTextColor(getResources().getColor(R.color.almostBlack));
        }
    }

    public void LoginClicked(View view) {
        String username = ((EditText) findViewById(R.id.usernameInputBox)).getText().toString();
        String password = ((EditText) findViewById(R.id.passwordInputBox)).getText().toString();
        Login login = new Login(username, password);
        login.LoginUser();
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }

    public void SignUpClicked(View view) {
        // TODO - Implement this. //
    }
}
