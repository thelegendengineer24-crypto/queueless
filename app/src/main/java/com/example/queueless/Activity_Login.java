package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Activity_Login extends AppCompatActivity {

    // ── Views ──
    private TextInputEditText emailEt, passwordEt;
    private MaterialButton loginBtn;
    private TextView registerTv, tvForgotPassword;

    // ── Firebase ──
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        initViews();
        checkAutoLogin();   // ✅ NEW: Already logged in toh direct dashboard
        setupButtons();
    }

    // ─────────────────────────────────────────
    //  1. Views
    // ─────────────────────────────────────────
    private void initViews() {
        emailEt          = findViewById(R.id.emailEt);
        passwordEt       = findViewById(R.id.passwordEt);
        loginBtn         = findViewById(R.id.loginBtn);
        registerTv       = findViewById(R.id.registerTv);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    // ─────────────────────────────────────────
    //  2. ✅ NEW: Auto Login Check
    //     Agar user pehle se logged in hai toh
    //     login screen skip kar do
    // ─────────────────────────────────────────
    private void checkAutoLogin() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            navigateByRole(userId);
        }
    }

    // ─────────────────────────────────────────
    //  3. Buttons
    // ─────────────────────────────────────────
    private void setupButtons() {

        loginBtn.setOnClickListener(v -> loginUser());

        registerTv.setOnClickListener(v -> {
            Intent intent = new Intent(Activity_Login.this, Activity_Register.class);
            intent.putExtra("role", "user");
            startActivity(intent);
        });

        // ✅ NEW: Forgot Password
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    // ─────────────────────────────────────────
    //  4. Login Logic
    // ─────────────────────────────────────────
    private void loginUser() {
        String email    = emailEt.getText() != null ? emailEt.getText().toString().trim() : "";
        String password = passwordEt.getText() != null ? passwordEt.getText().toString().trim() : "";

        // Validation
        if (email.isEmpty()) {
            emailEt.setError("Email required");
            emailEt.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEt.setError("Valid email enter karo");
            emailEt.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordEt.setError("Password required");
            passwordEt.requestFocus();
            return;
        }
        if (password.length() < 6) {
            passwordEt.setError("Password kam se kam 6 characters ka hona chahiye");
            passwordEt.requestFocus();
            return;
        }

        // Button disable — double click se bachao
        loginBtn.setEnabled(false);
        loginBtn.setText("Logging in...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = mAuth.getCurrentUser().getUid();
                    navigateByRole(userId);
                })
                .addOnFailureListener(e -> {
                    loginBtn.setEnabled(true);
                    loginBtn.setText("Login");

                    // ✅ Specific error messages
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("password")) {
                        Toast.makeText(this, "Galat password!", Toast.LENGTH_SHORT).show();
                    } else if (msg != null && msg.contains("no user")) {
                        Toast.makeText(this, "Email registered nahi hai!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Login failed: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ─────────────────────────────────────────
    //  5. Role check + Navigate
    //     Users   → UserDashboard
    //     Admin   → AdminDashboard / CreateBusiness
    // ─────────────────────────────────────────
    private void navigateByRole(String userId) {
        db.collection("Users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                        loginBtn.setEnabled(true);
                        loginBtn.setText("Login");
                        mAuth.signOut();
                        return;
                    }

                    String role = documentSnapshot.getString("role");

                    if (role != null && role.trim().equalsIgnoreCase("admin")) {
                        // Admin — check if business exists
                        // ✅ FIX: collection "businesses" (lowercase) use karo
                        db.collection("businesses")
                                .whereEqualTo("adminUid", userId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    Intent intent;
                                    if (!querySnapshot.isEmpty()) {
                                        // Business already registered
                                        String businessName = querySnapshot.getDocuments()
                                                .get(0).getString("businessName");
                                        intent = new Intent(this, Activity_Admindashboard.class);
                                        intent.putExtra("BUSINESS_NAME",
                                                businessName != null ? businessName : "");
                                    } else {
                                        // First time — create business
                                        intent = new Intent(this, Activity_Createbusiness.class);
                                    }
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    loginBtn.setEnabled(true);
                                    loginBtn.setText("Login");
                                    Toast.makeText(this,
                                            "Error: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        // Normal user
                        String userName = documentSnapshot.getString("name");
                        Intent intent = new Intent(this, Activity_Userdashboard.class);
                        intent.putExtra("USERNAME", userName != null ? userName : "User");
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    loginBtn.setEnabled(true);
                    loginBtn.setText("Login");
                    Toast.makeText(this,
                            "Error fetching user: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ─────────────────────────────────────────
    //  6. ✅ NEW: Forgot Password Dialog
    //     Email daalo → Firebase reset link bhejega
    // ─────────────────────────────────────────
    private void showForgotPasswordDialog() {
        // Email field already filled hai toh use karo
        String currentEmail = emailEt.getText() != null
                ? emailEt.getText().toString().trim() : "";

        final TextInputEditText resetEmailInput = new TextInputEditText(this);
        resetEmailInput.setHint("Email address");
        resetEmailInput.setText(currentEmail);
        resetEmailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        resetEmailInput.setPadding(40, 20, 40, 20);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset Password")
                .setMessage("Apna email daalo — hum reset link bhejenge")
                .setView(resetEmailInput)
                .setPositiveButton("Send Link", (dialog, which) -> {
                    String resetEmail = resetEmailInput.getText() != null
                            ? resetEmailInput.getText().toString().trim() : "";

                    if (resetEmail.isEmpty()) {
                        Toast.makeText(this, "Email daalo!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mAuth.sendPasswordResetEmail(resetEmail)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this,
                                            "Reset link " + resetEmail + " pe bhej diya!",
                                            Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Error: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}