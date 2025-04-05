package com.app.happytails.utils;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.app.happytails.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class ForgetPassword extends AppCompatActivity {

    private Button recoverBtn;
    private TextInputEditText emailEt;
    private ImageButton backBtn;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ProgressBar progBar;

    public static final String EMAIL_REGEX = "^(.+)@(.+)$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        backBtn = findViewById(R.id.btn_back_forget);
        recoverBtn = findViewById(R.id.btn_verify_forget);
        emailEt = findViewById(R.id.edit_email_forget);
        progBar = findViewById(R.id.progress_loading_forget);
        progBar.setVisibility(View.GONE);

        backBtn.setOnClickListener(v -> onBackPressed());

        recoverBtn.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();

            if (email.isEmpty() || !email.matches(EMAIL_REGEX)) {
                emailEt.setError("Invalid email format");
                return;
            }

            setInProgress(true);

            checkIfEmailExists(email);
        });
    }

    private void checkIfEmailExists(String email) {
        firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            sendPasswordResetEmail(email);
                        } else {
                            setInProgress(false);
                            Toast.makeText(ForgetPassword.this, "Email not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        setInProgress(false);
                        String err = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(ForgetPassword.this, err, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendPasswordResetEmail(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        setInProgress(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgetPassword.this, "Password reset email sent successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(ForgetPassword.this, SignInActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(ForgetPassword.this, err, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setInProgress(boolean inProgress) {
        progBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        recoverBtn.setEnabled(!inProgress);
    }
}