package com.app.happytails.utils;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.happytails.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginOtpActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private ProgressBar progressBar;
    private Button verifyButton;
    private ImageView backButton;

    private String email;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_otp);

        firebaseAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progress_loading_otp);
        verifyButton = findViewById(R.id.btn_verify);
        backButton = findViewById(R.id.btn_back_otp);

        email = getIntent().getStringExtra("email");
        username = getIntent().getStringExtra("username");

        verifyButton.setOnClickListener(v -> checkEmailVerification());
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void checkEmailVerification() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {
            setInProgress(true);

            user.reload().addOnCompleteListener(task -> {
                setInProgress(false);

                if (task.isSuccessful()) {
                    if (user.isEmailVerified()) {
                        Toast.makeText(this, "Email verified.", Toast.LENGTH_SHORT).show();
                        navigateToSignIn();
                    } else {
                        Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Network issue. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User not found. Please log in again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(LoginOtpActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    private void setInProgress(boolean inProgress) {
        progressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        verifyButton.setEnabled(!inProgress);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(LoginOtpActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}