package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class Activity_Admindashboard extends AppCompatActivity {

    TextView txtCurrentToken, txtTotalTokens;
    EditText edtAvgTime;
    Button btnNext, btnReset, btnUpdateTime, btnBusinessRegister;  // added button

    DatabaseReference queueRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admindashboard);

        // Firebase queue reference
        queueRef = FirebaseDatabase.getInstance().getReference("queue");

        // Views
        txtCurrentToken = findViewById(R.id.txtCurrentToken);
        txtTotalTokens = findViewById(R.id.txtTotalTokens);
        edtAvgTime = findViewById(R.id.edtAvgTime);
        btnNext = findViewById(R.id.btnNext);
        btnReset = findViewById(R.id.btnReset);
        btnUpdateTime = findViewById(R.id.btnUpdateTime);
        btnBusinessRegister = findViewById(R.id.btnBusinessRegister);  // new button

        // Firebase listener
        queueRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Long currentObj = snapshot.child("currentToken").getValue(Long.class);
                Long totalObj = snapshot.child("totalTokens").getValue(Long.class);

                long current = (currentObj != null) ? currentObj : 0;
                long total = (totalObj != null) ? totalObj : 0;

                txtCurrentToken.setText("Current: " + current);
                txtTotalTokens.setText("Total Tokens: " + total);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });

        // Buttons functionality
        btnNext.setOnClickListener(v -> {
            queueRef.child("currentToken").get().addOnSuccessListener(snapshot -> {
                Long currentObj = snapshot.getValue(Long.class);
                long current = (currentObj != null) ? currentObj : 0;
                queueRef.child("currentToken").setValue(current + 1);
            });
        });

        btnReset.setOnClickListener(v -> {
            queueRef.child("currentToken").setValue(0);
            queueRef.child("totalTokens").setValue(0);
        });

        btnUpdateTime.setOnClickListener(v -> {
            String input = edtAvgTime.getText().toString();
            if(!input.isEmpty()){
                int avg = Integer.parseInt(input);
                queueRef.child("avgTime").setValue(avg);
                Toast.makeText(this,"Updated!",Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,"Enter value first",Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ New: Business Register button
        btnBusinessRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, Activity_Createbusiness.class));
        });
    }
}