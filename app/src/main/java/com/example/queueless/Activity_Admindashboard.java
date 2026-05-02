package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    // ── Views ──
    private TextView txtCurrentToken, txtTotalTokens, txtRemaining;
    private TextView txtAvgWait, txtQueuePercent, tvBusinessName;
    private TextInputEditText edtAvgTime;
    private MaterialButton btnNext, btnReset, btnUpdateTime;
    private LinearLayout btnBusinessRegister, btnViewHistory;
    private ImageView btnNotification, btnLogout;
    private ProgressBar progressQueue;

    // ── Firebase ──
    private DatabaseReference queueRef;

    // ── State ──
    private long currentToken = 0;
    private long totalTokens  = 0;
    private int  avgTime      = 5; // default 5 mins per token

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admindashboard);

        // Firebase — apna business ID yahan daalo
        // e.g. "queue/businessId" agar multiple businesses hain
        queueRef = FirebaseDatabase.getInstance().getReference("queue");

        initViews();
        setupBusinessName();
        listenToQueue();       // Real-time Firebase listener
        setupButtons();
    }

    // ─────────────────────────────────────────
    //  1. Views initialize
    // ─────────────────────────────────────────
    private void initViews() {
        txtCurrentToken    = findViewById(R.id.txtCurrentToken);
        txtTotalTokens     = findViewById(R.id.txtTotalTokens);
        txtRemaining       = findViewById(R.id.txtRemaining);
        txtAvgWait         = findViewById(R.id.txtAvgWait);
        txtQueuePercent    = findViewById(R.id.txtQueuePercent);
        tvBusinessName     = findViewById(R.id.tvBusinessName);
        edtAvgTime         = findViewById(R.id.edtAvgTime);
        progressQueue      = findViewById(R.id.progressQueue);
        btnNext            = findViewById(R.id.btnNext);
        btnReset           = findViewById(R.id.btnReset);
        btnUpdateTime      = findViewById(R.id.btnUpdateTime);
        btnBusinessRegister = findViewById(R.id.btnBusinessRegister);
        btnViewHistory     = findViewById(R.id.btnViewHistory);
        btnNotification    = findViewById(R.id.btnNotification);
        btnLogout          = findViewById(R.id.btnLogout);
    }

    // ─────────────────────────────────────────
    //  2. Business name set karo
    //     (Intent se aata hai login ke baad)
    // ─────────────────────────────────────────
    private void setupBusinessName() {
        String businessName = getIntent().getStringExtra("BUSINESS_NAME");
        if (businessName != null && !businessName.isEmpty()) {
            tvBusinessName.setText(businessName);
        }
    }

    // ─────────────────────────────────────────
    //  3. Firebase Real-time Listener
    //     Sab kuch automatically update hoga
    // ─────────────────────────────────────────
    private void listenToQueue() {
        queueRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Data fetch karo
                Long currentObj = snapshot.child("currentToken").getValue(Long.class);
                Long totalObj   = snapshot.child("totalTokens").getValue(Long.class);
                Long avgObj     = snapshot.child("avgTime").getValue(Long.class);

                currentToken = (currentObj != null) ? currentObj : 0;
                totalTokens  = (totalObj   != null) ? totalObj   : 0;
                if (avgObj   != null) avgTime = avgObj.intValue();

                // UI update karo
                updateUI();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Activity_Admindashboard.this,
                        "Firebase error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────
    //  4. UI Update — sab stats ek jagah
    // ─────────────────────────────────────────
    private void updateUI() {
        // Current token
        txtCurrentToken.setText(String.valueOf(currentToken));

        // Total tokens
        txtTotalTokens.setText(String.valueOf(totalTokens));

        // Remaining in queue
        long remaining = Math.max(0, totalTokens - currentToken);
        txtRemaining.setText(String.valueOf(remaining));

        // Avg wait time
        long waitMins = remaining * avgTime;
        txtAvgWait.setText(waitMins + " min");

        // Progress bar
        int progress = (totalTokens > 0)
                ? (int) ((currentToken * 100) / totalTokens)
                : 0;
        progressQueue.setProgress(progress);
        txtQueuePercent.setText(progress + "%");
    }

    // ─────────────────────────────────────────
    //  5. Buttons setup
    // ─────────────────────────────────────────
    private void setupButtons() {

        // ── Next Token ──
        btnNext.setOnClickListener(v -> {
            if (currentToken >= totalTokens && totalTokens > 0) {
                Toast.makeText(this, "Queue khatam ho gayi!", Toast.LENGTH_SHORT).show();
                return;
            }
            queueRef.child("currentToken").setValue(currentToken + 1)
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        });

        // ── Reset Queue — Confirmation Dialog ──
        btnReset.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Reset Queue?")
                    .setMessage("Kya aap sure hain? Saare tokens reset ho jaayenge.")
                    .setPositiveButton("Haan, Reset Karo", (dialog, which) -> {
                        queueRef.child("currentToken").setValue(0);
                        queueRef.child("totalTokens").setValue(0);
                        Toast.makeText(this, "Queue reset ho gayi!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // ── Update Avg Time ──
        btnUpdateTime.setOnClickListener(v -> {
            String input = edtAvgTime.getText() != null
                    ? edtAvgTime.getText().toString().trim() : "";

            if (input.isEmpty()) {
                Toast.makeText(this, "Pehle value enter karo!", Toast.LENGTH_SHORT).show();
                return;
            }

            int newAvg = Integer.parseInt(input);
            if (newAvg <= 0) {
                Toast.makeText(this, "Valid time enter karo!", Toast.LENGTH_SHORT).show();
                return;
            }

            queueRef.child("avgTime").setValue(newAvg)
                    .addOnSuccessListener(unused -> {
                        avgTime = newAvg;
                        edtAvgTime.setText("");
                        edtAvgTime.clearFocus();
                        Toast.makeText(this,
                                "Avg time updated: " + newAvg + " min",
                                Toast.LENGTH_SHORT).show();
                        updateUI();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        });

        // ── Register Business ──
        btnBusinessRegister.setOnClickListener(v ->
                startActivity(new Intent(this, Activity_Createbusiness.class)));

        // ── View History ──
        btnViewHistory.setOnClickListener(v ->
                Toast.makeText(this, "History coming soon!", Toast.LENGTH_SHORT).show());

        // ── Notification ──
        btnNotification.setOnClickListener(v ->
                Toast.makeText(this, "Notifications coming soon!", Toast.LENGTH_SHORT).show());

        // ── Logout ──
        btnLogout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Logout")
                    .setMessage("Kya aap logout karna chahte hain?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        // FirebaseAuth.getInstance().signOut(); // uncomment if using Auth
                        Intent intent = new Intent(this, Activity_Login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}