package MobileAndUbiquitousComputing.P2Photos.EntryScreens;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import MobileAndUbiquitousComputing.P2Photos.R;

public class SignupLoginEntryScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_login_entry_screen);

        // Adds click event to Login Button
        Button btn = findViewById(R.id.LoginButton);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginButtonClicked(v);
            }
        });

        // Adds click event to Sign Up Button
        btn = findViewById(R.id.SignUpButton);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignUpButtonClicked(v);
            }
        });
    }

    private void LoginButtonClicked(View view) {
        startActivity(new Intent(this, LoginScreen.class));
    }

    private void SignUpButtonClicked(View view) {
        startActivity(new Intent(this, SignUpScreen.class));
    }
}
