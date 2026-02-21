package com.yourpackagename;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.queueless.R;
import com.example.queueless.activity_roleselection;

public class RoleSelectionActivity extends AppCompatActivity {

    Button btnUser, btnAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roleselection);

        btnUser = findViewById(R.id.btnUser);
        btnAdmin = findViewById(R.id.btnAdmin);

        btnUser.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, UserLoginActivity.class);
            startActivity(intent);
        });

        btnAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });
    }

    private class AdminLoginActivity {
    }

    private class UserLoginActivity {
    }
}