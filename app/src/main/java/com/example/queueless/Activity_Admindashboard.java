package com.example.queueless;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Activity_Admindashboard extends AppCompatActivity {

    private TextView txtCurrentToken, txtTotalTokens, txtRemaining, txtAvgWait, tvBusinessName;
    private TextInputEditText edtAvgTime;
    private MaterialButton btnNext, btnReset, btnUpdateTime;
    private ImageView btnShowQr, btnLogout;
    private ProgressBar progressQueue;
    private DatabaseReference queueRef;

    private long currentToken = 0;
    private long totalTokens = 0;
    private int avgTime = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admindashboard);

        // Firebase Reference
        queueRef = FirebaseDatabase.getInstance().getReference("queue");

        initViews();
        listenToQueue();
        setupButtons();
    }

    private void initViews() {
        txtCurrentToken = findViewById(R.id.txtCurrentToken);
        txtTotalTokens = findViewById(R.id.txtTotalTokens);
        txtRemaining = findViewById(R.id.txtRemaining);
        txtAvgWait = findViewById(R.id.txtAvgWait);
        tvBusinessName = findViewById(R.id.tvBusinessName);
        edtAvgTime = findViewById(R.id.edtAvgTime);
        progressQueue = findViewById(R.id.progressQueue);
        btnNext = findViewById(R.id.btnNext);
        btnReset = findViewById(R.id.btnReset);
        btnUpdateTime = findViewById(R.id.btnUpdateTime);
        btnShowQr = findViewById(R.id.btnShowQr);
        btnLogout = findViewById(R.id.btnLogout);

        String bName = getIntent().getStringExtra("BUSINESS_NAME");
        if (bName != null) tvBusinessName.setText(bName);
    }

    private void listenToQueue() {
        queueRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                currentToken = snapshot.child("currentToken").getValue(Long.class) != null ? snapshot.child("currentToken").getValue(Long.class) : 0;
                totalTokens = snapshot.child("totalTokens").getValue(Long.class) != null ? snapshot.child("totalTokens").getValue(Long.class) : 0;
                avgTime = snapshot.child("avgTime").getValue(Integer.class) != null ? snapshot.child("avgTime").getValue(Integer.class) : 5;
                updateUI();
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void updateUI() {
        txtCurrentToken.setText(String.valueOf(currentToken));
        txtTotalTokens.setText(String.valueOf(totalTokens));
        long remaining = Math.max(0, totalTokens - currentToken);
        txtRemaining.setText(String.valueOf(remaining));
        txtAvgWait.setText((remaining * avgTime) + " min");

        int progress = (totalTokens > 0) ? (int) ((currentToken * 100) / totalTokens) : 0;
        progressQueue.setProgress(progress);
    }

    private void setupButtons() {
        // Next Token Logic
        btnNext.setOnClickListener(v -> {
            if (currentToken < totalTokens) {
                queueRef.child("currentToken").setValue(currentToken + 1);
            } else {
                Toast.makeText(this, "No more tokens in queue!", Toast.LENGTH_SHORT).show();
            }
        });

        // QR Code Popup (Uses existing scan icon)
        btnShowQr.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("Business QR Code");
            builder.setMessage("Ask customers to scan this icon to join.");
            ImageView qrPreview = new ImageView(this);
            qrPreview.setImageResource(R.drawable.ic_qrcode); // Placeholder
            qrPreview.setPadding(0, 40, 0, 40);
            builder.setView(qrPreview);
            builder.setPositiveButton("Close", null);
            builder.show();
        });

        // Reset with Confirmation
        btnReset.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Reset Queue?")
                    .setMessage("Are you sure? This will clear all data.")
                    .setPositiveButton("Reset", (d, w) -> {
                        queueRef.child("currentToken").setValue(0);
                        queueRef.child("totalTokens").setValue(0);
                    })
                    .setNegativeButton("Cancel", null).show();
        });

        // Update Average Time
        btnUpdateTime.setOnClickListener(v -> {
            String val = edtAvgTime.getText().toString();
            if (!val.isEmpty()) {
                queueRef.child("avgTime").setValue(Integer.parseInt(val));
                Toast.makeText(this, "Wait time updated!", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> finish());
    }
}