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
            confirmButton.setClickable(true);
            confirmButton.setBackgroundColor(getResources().getColor(R.color.colorButtonActive));
            confirmButton.setTextColor(getResources().getColor(R.color.white));
        }
        else {
            confirmButton.setClickable(false);
            confirmButton.setBackgroundColor(getResources().getColor(R.color.colorButtonInactive));
            confirmButton.setTextColor(getResources().getColor(R.color.almostBlack));
        }
    }

    public void SignUpClicked(View view) {
        String username = ((EditText) findViewById(R.id.usernameInputBox)).getText().toString();
        String password = ((EditText) findViewById(R.id.passwordInputBox)).getText().toString();
        String confirmPassword = ((EditText) findViewById(R.id.confirmPasswordInputBox)).getText().toString();

        if (!password.equals(confirmPassword)) {
            // TODO //
        }
    }
}
