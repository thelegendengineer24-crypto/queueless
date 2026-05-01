package com.example.queueless;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

public class activity_admindashboard  extends AppCompatActivity {

    TextView txtCurrentToken, txtTotalTokens;
    EditText edtAvgTime;
    Button btnNext, btnReset, btnUpdateTime;

    DatabaseReference queueRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admindashboard);

        queueRef = FirebaseDatabase.getInstance().getReference("queue");

        txtCurrentToken = findViewById(R.id.txtCurrentToken);
        txtTotalTokens = findViewById(R.id.txtTotalTokens);
        edtAvgTime = findViewById(R.id.edtAvgTime);
        btnNext = findViewById(R.id.btnNext);
        btnReset = findViewById(R.id.btnReset);
        btnUpdateTime = findViewById(R.id.btnUpdateTime);

        queueRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot.exists()){
                    long current = snapshot.child("currentToken").getValue(Long.class);
                    long total = snapshot.child("totalTokens").getValue(Long.class);

                    txtCurrentToken.setText("Current: " + current);
                    txtTotalTokens.setText("Total Tokens: " + total);
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });

        btnNext.setOnClickListener(v -> {
            queueRef.child("currentToken").get().addOnSuccessListener(snapshot -> {
                long current = snapshot.getValue(Long.class);
                queueRef.child("currentToken").setValue(current + 1);
            });
        });

        btnReset.setOnClickListener(v -> {
            queueRef.child("currentToken").setValue(0);
            queueRef.child("totalTokens").setValue(0);
        });

        btnUpdateTime.setOnClickListener(v -> {
            int avg = Integer.parseInt(edtAvgTime.getText().toString());
            queueRef.child("avgTime").setValue(avg);
            Toast.makeText(this,"Updated!",Toast.LENGTH_SHORT).show();
        });
    }
}
