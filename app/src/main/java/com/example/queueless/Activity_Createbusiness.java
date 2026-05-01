package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;.FirebaseAuth;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Activity_Createbusiness extends AppCompatActivity {

    TextInputEditText etBusinessName, etUniqueId;
    MaterialButton btnCreateBusiness;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createbusiness);

        // Initialize UI components
        etBusinessName = findViewById(R.id.etBusinessName);
        etUniqueId = findViewById(R.id.etUniqueId);
        btnCreateBusiness = findViewById(R.id.btnCreateBusiness);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Button click listener
        btnCreateBusiness.setOnClickListener(v -> registerBusiness());
    }

    private void registerBusiness() {
        // Get input values
        String name = etBusinessName.getText().toString().trim();
        String uniqueId = etUniqueId.getText().toString().trim();

        // Check empty fields
        if (TextUtils.isEmpty(name)) {
            etBusinessName.setError("Business Name required");
            etBusinessName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(uniqueId)) {
            etUniqueId.setError("Unique ID required");
            etUniqueId.requestFocus();
            return;
        }

        // Prepare Firestore data
        Map<String, Object> business = new HashMap<>();
        business.put("businessName", name);
        business.put("uniqueId", uniqueId);
        business.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        // Current admin UID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Save in Firestore: Business collection, document ID = admin UID
        db.collection("Business").document(userId)
                .set(business)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Business Registered!", Toast.LENGTH_SHORT).show();
                    // Open Admin Dashboard
                    startActivity(new Intent(Activity_Createbusiness.this, Activity_Admindashboard.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}