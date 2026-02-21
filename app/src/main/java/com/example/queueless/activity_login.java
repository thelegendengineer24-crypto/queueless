package com.example.queueless;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class activity_login extends AppCompatActivity {

    TextInputEditText emailEt, passwordEt;
    MaterialButton loginBtn;
    TextView registerTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        loginBtn = findViewById(R.id.loginBtn);
        registerTv = findViewById(R.id.registerTv);


        loginBtn.setOnClickListener(v -> {

            String email = emailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();

            if(email.isEmpty()){
                emailEt.setError("Email required");
                return;
            }

            if(password.isEmpty()){
                passwordEt.setError("Password required");
                return;
            }


            Toast.makeText(activity_login.this, "Login Successful", Toast.LENGTH_SHORT).show();


        });


        registerTv.setOnClickListener(v -> {
            // startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            Toast.makeText(activity_login.this, "Go to Register Screen", Toast.LENGTH_SHORT).show();
        });

    }
}