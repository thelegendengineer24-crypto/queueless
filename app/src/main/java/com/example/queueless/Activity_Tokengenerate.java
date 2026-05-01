package com.example.queueless;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Activity_Tokengenerate extends AppCompatActivity {

    Spinner businessSpinner;
    Button btnGenerateToken;
    TextView txtTokenNumber;

    int tokenCounter = 100; // starting token number

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tokengenerate); 

        businessSpinner = findViewById(R.id.businessSpinner);
        btnGenerateToken = findViewById(R.id.btnGenerateToken);
        txtTokenNumber = findViewById(R.id.txtTokenNumber);

        // Sample Business List
        String[] businesses = {"Select Business", "Bank", "Hospital", "Salon", "Government Office", "Restaurant" , "other"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                businesses
        );

        businessSpinner.setAdapter(adapter);

        btnGenerateToken.setOnClickListener(v -> {

            if (businessSpinner.getSelectedItemPosition() == 0) {

                Toast.makeText(Activity_Tokengenerate.this,
                        "Please select a business",
                        Toast.LENGTH_SHORT).show();

            } else {

                tokenCounter++;
                String token = "Your Token: #" + tokenCounter;

                txtTokenNumber.setText(token);

                Toast.makeText(Activity_Tokengenerate.this,
                        "Token Generated Successfully!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}