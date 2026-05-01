package com.example.queueless;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class activity_tokengenerate extends AppCompatActivity {

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
        String[] businesses = {"Select Business", "Bank", "Hospital", "Salon", "Government Office"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                businesses
        );

        businessSpinner.setAdapter(adapter);

        btnGenerateToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (businessSpinner.getSelectedItemPosition() == 0) {
                    Toast.makeText(activity_tokengenerate.this,
                            "Please select a business",
                            Toast.LENGTH_SHORT).show();
                } else {

                    tokenCounter++;
                    String token = "Your Token: #" + tokenCounter;

                    txtTokenNumber.setText(token);

                    Toast.makeText(activity_tokengenerate.this,
                            "Token Generated Successfully!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
