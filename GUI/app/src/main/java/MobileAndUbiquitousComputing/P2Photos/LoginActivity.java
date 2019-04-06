package MobileAndUbiquitousComputing.P2Photos;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import MobileAndUbiquitousComputing.P2Photos.exceptions.FailedLoginException;
import MobileAndUbiquitousComputing.P2Photos.exceptions.WrongCredentialsException;
import MobileAndUbiquitousComputing.P2Photos.helpers.Login;

public class LoginActivity extends AppCompatActivity {

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
        // Do nothing.
        // Prevents going back after logging out.
    }

    private void ActivateButtons(EditText usernameInput, EditText passwordInput,
                                 Button loginButton, Button signupButton) {
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

    public void LoginClicked(View view) {
        String username = ((EditText) findViewById(R.id.usernameInputBox)).getText().toString();
        EditText passwordInput = findViewById(R.id.passwordInputBox);
        String password = passwordInput.getText().toString();
        username = username.trim();
        password = password.trim();
        try {
            Login.login(this, username, password);
            Intent intent = new Intent(this, MainMenuActivity.class);
            startActivity(intent);
        } catch (WrongCredentialsException wcex) {
            passwordInput.setText("");
        } catch (FailedLoginException flex) {
            Toast toast = Toast.makeText(this, "The Login operation failed. Try again later",
                    Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void SignUpClicked(View view) {
        String username = ((EditText) findViewById(R.id.usernameInputBox)).getText().toString();
        String password = ((EditText) findViewById(R.id.passwordInputBox)).getText().toString();
        Intent intent = new Intent(this, SignUpActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("password", password);
        startActivity(intent);
    }
}
