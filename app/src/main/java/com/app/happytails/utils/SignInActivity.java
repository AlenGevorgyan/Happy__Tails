package com.app.happytails.utils;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.happytails.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private EditText emailEditText, passwordEditText;
    private ProgressBar progressBar;
    private Button signInButton, testBtn;
    private TextView signupText, forgotPass;
    private static final String TAG = "SignInActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        firebaseAuth = FirebaseAuth.getInstance();
        emailEditText = findViewById(R.id.emailSignInEditText);
        passwordEditText = findViewById(R.id.passwordSignInEditText);
        progressBar = findViewById(R.id.SignIpProc);
        signInButton = findViewById(R.id.SignInButton);
        signupText = findViewById(R.id.SignUpTextView);
        testBtn = findViewById(R.id.testButton);
        forgotPass = findViewById(R.id.forgotTv);

        signupText.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        forgotPass.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, ForgetPassword.class);
            startActivity(intent);
        });

        signInButton.setOnClickListener(v -> signInUser());
        testBtn.setOnClickListener(v -> testSignIn());

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            navigateToHome();
        }
    }


    private void testSignIn() {
        String email = "alengevorgyan2009@gmail.com";
        String password = "Alen2005.2009";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        signInButton.setEnabled(false);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    signInButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            navigateToHome();
                        } else {
                            showError("Please verify your email before signing in.");
                        }
                    } else {
                        handleSignInError(task.getException());
                    }
                });
    }

    private void signInUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        signInButton.setEnabled(false);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    signInButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            navigateToHome();
                        } else {
                            showError("Please verify your email before signing in.");
                        }
                    } else {
                        handleSignInError(task.getException());
                    }
                });
    }

    private void handleSignInError(Exception exception) {
        try {
            throw exception;
        } catch (FirebaseAuthInvalidUserException e) {
            showError("No account found with this email.");
        } catch (FirebaseAuthInvalidCredentialsException e) {
            showError("Invalid password. Please try again.");
        } catch (Exception e) {
            showError("Sign-in failed. Please try again later.");
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }
}
