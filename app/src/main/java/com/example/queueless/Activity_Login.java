package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Activity_Login extends AppCompatActivity {

    TextInputEditText emailEt, passwordEt;
    MaterialButton loginBtn;
    TextView registerTv;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        loginBtn = findViewById(R.id.loginBtn);
        registerTv = findViewById(R.id.registerTv);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loginBtn.setOnClickListener(v -> loginUser());

        registerTv.setOnClickListener(v -> {
            Intent intent = new Intent(Activity_Login.this, Activity_Register.class);
            intent.putExtra("role", "user"); // default role for registration
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();

        if (email.isEmpty()) {
            emailEt.setError("Email required");
            emailEt.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEt.setError("Password required");
            passwordEt.requestFocus();
            return;
        }

        loginBtn.setEnabled(false); // prevent double click

        // Sign in with Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = mAuth.getCurrentUser().getUid();

                    // Fetch user document to check role
                    db.collection("Users").document(userId)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String role = documentSnapshot.getString("role");

                                    if (role != null && role.trim().equalsIgnoreCase("admin")) {
                                        // Admin login
                                        db.collection("Business").document(userId)
                                                .get()
                                                .addOnSuccessListener(businessDoc -> {
                                                    Intent intent;
                                                    if (businessDoc.exists()) {
                                                        // Admin already has business → open dashboard
                                                        intent = new Intent(Activity_Login.this, Activity_Admindashboard.class);
                                                    } else {
                                                        // First time admin → create business
                                                        intent = new Intent(Activity_Login.this, Activity_Createbusiness.class);
                                                    }
                                                    startActivity(intent);
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(Activity_Login.this, "Error fetching business: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    loginBtn.setEnabled(true);
                                                });
                                    } else {
                                        // Normal user login
                                        startActivity(new Intent(Activity_Login.this, Activity_Userdashboard.class));
                                        finish();
                                    }

                                } else {
                                    Toast.makeText(Activity_Login.this, "User data not found", Toast.LENGTH_SHORT).show();
                                    loginBtn.setEnabled(true);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(Activity_Login.this, "Error fetching user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                loginBtn.setEnabled(true);
                            });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Activity_Login.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    loginBtn.setEnabled(true);
                });
    }
}