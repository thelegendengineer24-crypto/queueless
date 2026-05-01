package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class Activity_Roleselection extends AppCompatActivity {

    Button btnUser, btnAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roleselection);

        btnUser = findViewById(R.id.btnUser);
        btnAdmin = findViewById(R.id.btnAdmin);

        // User Role
        btnUser.setOnClickListener(v -> {
            Intent intent = new Intent(this, Activity_Login.class);
            intent.putExtra("role", "user");  // pass role info
            startActivity(intent);
        });

        // Admin Role
        btnAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(this, Activity_Login.class);
            intent.putExtra("role", "admin"); // pass role info
            startActivity(intent);
        });
    }
}