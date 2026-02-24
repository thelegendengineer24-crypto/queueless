package com.example.queueless;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class activity_createbusiness extends AppCompatActivity {

    EditText etBusinessName, etUniqueId;
    Button btnCreateBusiness;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createbusiness);

        etBusinessName = findViewById(R.id.etBusinessName);
        etUniqueId = findViewById(R.id.etUniqueId);
        btnCreateBusiness = findViewById(R.id.btnCreateBusiness);

        db = FirebaseFirestore.getInstance();

        btnCreateBusiness.setOnClickListener(v -> registerBusiness());
    }

    private void registerBusiness() {

        String name = etBusinessName.getText().toString().trim();
        String uniqueId = etUniqueId.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(uniqueId)) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> business = new HashMap<>();
        business.put("businessName", name);
        business.put("uniqueId", uniqueId);

        db.collection("businesses")
                .add(business)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(this, "Business Registered!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
