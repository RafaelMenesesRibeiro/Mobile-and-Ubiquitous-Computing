package MobileAndUbiquitousComputing.P2Photos;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedLoginException;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedSignupException;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.UsernameExistsException;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.WrongCredentialsException;
import MobileAndUbiquitousComputing.P2Photos.Helpers.ConnectionManager;
import MobileAndUbiquitousComputing.P2Photos.Helpers.Login;
import MobileAndUbiquitousComputing.P2Photos.Helpers.QueryManager;

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

        if (!password.equals(confirmPassword)) {
            passwordInput.setText("");
            confirmPasswordInput.setText("");
            return;
        }

        try {
            signup(username, password);
            Intent intent = new Intent(this, MainMenuActivity.class);
            startActivity(intent);
        }
        catch (UsernameExistsException ueex) {
            Toast toast = Toast.makeText(this, "The username '" + username + "' already exists", Toast.LENGTH_LONG);
            toast.show();
        }
        catch (FailedLoginException flex) {
            Toast toast = Toast.makeText(this, "The Login operation failed. Try again later", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void signup(String username, String password)
            throws FailedSignupException, FailedLoginException, UsernameExistsException {
        Log.i("MSG", "Signup: " + username);
        String url = ConnectionManager.P2PHOTO_HOST + ConnectionManager.SIGNUP_OPERATION;
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("username", username);
            requestBody.put("password", password);
            RequestData requestData = new PostRequestData(this, RequestData.RequestType.SIGNUP, url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            // TODO - Response codes are now in a ResponseEntity. //
            if (code == 200) {
                Log.i("STATUS", "The sign up operation was successful");
            }
            else if (code == 422) {
                Log.i("STATUS", "The sign up operation was unsuccessful. The username chosen already exists.");
                throw new UsernameExistsException();
            }
            else {
                Log.i("STATUS", "The sign up operation was unsuccessful. Unknown error.");
                throw new FailedSignupException();
            }
        }
        catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedSignupException(ex.getMessage());
        }

        try {
            Login.login(this, username, password);
        }
        catch (WrongCredentialsException wcex) {
            // Do nothing.
            // SHOULD NEVER BE HERE. AS THE CREDENTIALS WERE USED WITHOUT CHANGE FOR SIGNING UP.
        }
    }
}
