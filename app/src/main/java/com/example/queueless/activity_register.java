package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class activity_register extends AppCompatActivity {

    TextInputEditText nameEt, emailEt, passwordEt;
    MaterialButton registerBtn;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameEt = findViewById(R.id.nameEt);
        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        registerBtn = findViewById(R.id.registerBtn);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        registerBtn.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String name = nameEt.getText().toString().trim();
        String email = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();

        if(name.isEmpty() || email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    String userId = mAuth.getCurrentUser().getUid();

                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("name", name);
                    userMap.put("email", email);
                    userMap.put("role", "user"); // default role

                    db.collection("Users")
                            .document(userId)
                            .set(userMap);

                    Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}