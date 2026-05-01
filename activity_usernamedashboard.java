package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class activity_userdashboard extends AppCompatActivity {

    TextView tvWelcome, tvSelectedBusiness;
    Button btnGenerateToken, btnViewStatus, btnHistory, btnLogout;

    String userName = "Aman";
    String selectedBusiness = "XYZ Clinic";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userdashboard);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvSelectedBusiness = findViewById(R.id.tvSelectedBusiness);
        btnGenerateToken = findViewById(R.id.btnGenerateToken);
        btnViewStatus = findViewById(R.id.btnViewStatus);
        btnHistory = findViewById(R.id.btnHistory);
        btnLogout = findViewById(R.id.btnLogout);

        tvWelcome.setText("Welcome, " + userName + " 👋");
        tvSelectedBusiness.setText("Selected Business: " + selectedBusiness);

        btnGenerateToken.setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.queueless.activity_tokengenerate.class));
        });

        btnViewStatus.setOnClickListener(v -> {
            startActivity(new Intent(this, activity_tokenstatus.class));
        });

        btnHistory.setOnClickListener(v -> {
            // Future: Open history activity
        });

        btnLogout.setOnClickListener(v -> {
            finish();
        });
    }
}
