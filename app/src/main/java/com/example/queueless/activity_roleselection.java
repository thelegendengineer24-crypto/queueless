package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class activity_roleselection extends AppCompatActivity {

    Button btnUser, btnAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roleselection);

        btnUser = findViewById(R.id.btnUser);
        btnAdmin = findViewById(R.id.btnAdmin);

        btnUser.setOnClickListener(v -> {
            Intent intent = new Intent(activity_roleselection.this, UserLoginActivity.class);
            startActivity(intent);
        });

        btnAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(activity_roleselection.this, AdminLoginActivity.class);
            startActivity(intent);
        });
    }
}