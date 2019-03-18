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

import MobileAndUbiquitousComputing.P2Photos.Helpers.Signup;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        Intent intent = getIntent();
        String username = intent.getStringExtra("username");
        String password = intent.getStringExtra("password");

        final EditText usernameInput = findViewById(R.id.usernameInputBox);
        usernameInput.setText(username);
        final EditText passwordInput = findViewById(R.id.passwordInputBox);
        passwordInput.setText(password);

        final EditText confirmPasdwordInput = findViewById(R.id.confirmPasswordInputBox);
        final Button confirmButton = findViewById(R.id.ConfirmButton);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do nothing. */ }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ActivateButtons(usernameInput, passwordInput, confirmPasdwordInput, confirmButton);
            }
            @Override
            public void afterTextChanged(Editable s) { /* Do nothing */ }
        };

        usernameInput.addTextChangedListener(textWatcher);
        passwordInput.addTextChangedListener(textWatcher);
        confirmPasdwordInput.addTextChangedListener(textWatcher);
    }

    private void ActivateButtons(EditText usernameInput, EditText passwordInput, EditText confirmPasswordInput,
                                 Button confirmButton) {
        if (!usernameInput.getText().toString().isEmpty() && !passwordInput.getText().toString().isEmpty() && !confirmPasswordInput.getText().toString().isEmpty()) {
            confirmButton.setEnabled(true);
            confirmButton.setBackgroundColor(getResources().getColor(R.color.colorButtonActive));
            confirmButton.setTextColor(getResources().getColor(R.color.white));
        }
        else {
            confirmButton.setEnabled(false);
            confirmButton.setBackgroundColor(getResources().getColor(R.color.colorButtonInactive));
            confirmButton.setTextColor(getResources().getColor(R.color.almostBlack));
        }
    }

    public void SignUpClicked(View view) {
        EditText usernameInput = (EditText) findViewById(R.id.usernameInputBox);
        EditText passwordInput = (EditText) findViewById(R.id.passwordInputBox);
        EditText confirmPasswordInput = (EditText) findViewById(R.id.confirmPasswordInputBox);
        String username = usernameInput.getText().toString();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        // TODO - Request server if username already exists. //

        if (!password.equals(confirmPassword)) {
            passwordInput.setText("");
            confirmPasswordInput.setText("");
            return;
        }

        boolean isSuccess = Signup.SignupUser(username, password);
        if (!isSuccess) {
            // TODO - Implement what happens when sign up fails. //
        }

        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }
}
