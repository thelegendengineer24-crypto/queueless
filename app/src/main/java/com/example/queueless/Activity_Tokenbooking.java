package com.example.queueless;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

public class Activity_Tokenbooking extends AppCompatActivity {

    TextView tvServiceName, tvTokenNumber;
    MaterialButton btnBookToken;

    FirebaseFirestore db;

    String businessId;
    String serviceId;
    String serviceName;

    boolean alreadyBooked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tokenbooking);

        tvServiceName = findViewById(R.id.tvServiceName);
        tvTokenNumber = findViewById(R.id.tvTokenNumber);
        btnBookToken = findViewById(R.id.btnBookToken);

        db = FirebaseFirestore.getInstance();

        // Receive data from previous screen
        businessId = getIntent().getStringExtra("businessId");
        serviceId = getIntent().getStringExtra("serviceId");
        serviceName = getIntent().getStringExtra("serviceName");

        tvServiceName.setText(serviceName);

        btnBookToken.setOnClickListener(v -> bookToken());
    }

    private void bookToken() {

        if (alreadyBooked) {
            Toast.makeText(this, "Token Already Booked!", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference serviceRef = db.collection("businesses")
                .document(businessId)
                .collection("services")
                .document(serviceId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {

            Long currentToken = transaction.get(serviceRef).getLong("currentToken");

            if (currentToken == null) {
                currentToken = 0L;
            }

            long newToken = currentToken + 1;

            transaction.update(serviceRef, "currentToken", newToken);

            runOnUiThread(() -> {
                tvTokenNumber.setText("Your Token: " + newToken);
                Toast.makeText(this, "Token Booked Successfully!", Toast.LENGTH_SHORT).show();
                alreadyBooked = true;
            });

            return null;

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Booking Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }
}