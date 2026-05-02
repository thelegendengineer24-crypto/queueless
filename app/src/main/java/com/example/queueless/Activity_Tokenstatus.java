package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class activity_tokenstatus extends AppCompatActivity {

    // Views
    private TextView tvBusinessName, tvYourToken, tvNowServing, tvAheadCount,
            tvWaitTime, tvTokenStatus, tvAlertMessage, tvProgressStart, tvProgressEnd;
    private ProgressBar progressQueue;
    private CardView cardAlert, cardDone;
    private LinearLayout btnRefresh, btnHistory, btnCancelToken;
    private MaterialButton btnNewToken;

    // Firebase
    private DatabaseReference db;
    private FirebaseAuth mAuth;
    private ValueEventListener queueListener;

    // Token data (passed via Intent)
    private String businessId;
    private String businessName;
    private long myTokenNumber;

    // Alert threshold: show alert when ≤ this many tokens ahead
    private static final int ALERT_THRESHOLD = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tokenstatus);

        db    = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Read intent data
        businessId    = getIntent().getStringExtra("businessId");
        businessName  = getIntent().getStringExtra("businessName");
        String tokenStr = getIntent().getStringExtra("tokenNumber");
        try {
            myTokenNumber = Long.parseLong(tokenStr);
        } catch (Exception e) {
            myTokenNumber = 0;
        }

        initViews();
        setupClickListeners();
        startRealTimeListener();
    }

    private void initViews() {
        tvBusinessName  = findViewById(R.id.tvBusinessName);
        tvYourToken     = findViewById(R.id.tvYourToken);
        tvNowServing    = findViewById(R.id.tvNowServing);
        tvAheadCount    = findViewById(R.id.tvAheadCount);
        tvWaitTime      = findViewById(R.id.tvWaitTime);
        tvTokenStatus   = findViewById(R.id.tvTokenStatus);
        tvAlertMessage  = findViewById(R.id.tvAlertMessage);
        tvProgressStart = findViewById(R.id.tvProgressStart);
        tvProgressEnd   = findViewById(R.id.tvProgressEnd);
        progressQueue   = findViewById(R.id.progressQueue);
        cardAlert       = findViewById(R.id.cardAlert);
        cardDone        = findViewById(R.id.cardDone);
        btnRefresh      = findViewById(R.id.btnRefresh);
        btnHistory      = findViewById(R.id.btnHistory);
        btnCancelToken  = findViewById(R.id.btnCancelToken);
        btnNewToken     = findViewById(R.id.btnNewToken);

        // Set static data
        tvBusinessName.setText(businessName != null ? businessName : "Unknown Business");
        tvYourToken.setText(String.format("#%03d", myTokenNumber));
        tvProgressEnd.setText(String.format("#%03d", myTokenNumber));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        // Manual refresh
        btnRefresh.setOnClickListener(v -> {
            if (businessId != null) refreshQueue();
        });

        // History → go to dashboard (or dedicated history screen when built)
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, Activity_Userdashboard.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // Cancel token with confirmation dialog
        btnCancelToken.setOnClickListener(v -> showCancelDialog());

        // New Token
        btnNewToken.setOnClickListener(v -> {
            Intent intent = new Intent(this, activity_tokengenerate.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    // ── Real-time listener on queue node ──
    private void startRealTimeListener() {
        if (businessId == null) return;

        queueListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                long current = getOrDefault(snapshot, "currentToken", 0L);
                long total   = getOrDefault(snapshot, "totalTokens",  myTokenNumber);
                long avgTime = getOrDefault(snapshot, "avgTime",       5L);

                updateUI(current, total, avgTime);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(activity_tokenstatus.this,
                        "Connection lost. Pull to refresh.", Toast.LENGTH_SHORT).show();
            }
        };

        db.child("queue").child(businessId).addValueEventListener(queueListener);
    }

    private void refreshQueue() {
        db.child("queue").child(businessId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                long current = getOrDefault(snapshot, "currentToken", 0L);
                long total   = getOrDefault(snapshot, "totalTokens",  myTokenNumber);
                long avgTime = getOrDefault(snapshot, "avgTime",       5L);
                updateUI(current, total, avgTime);
                Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(long current, long total, long avgTime) {
        long ahead = Math.max(0, myTokenNumber - current - 1);
        long wait  = ahead * avgTime;

        // Served check
        if (current >= myTokenNumber) {
            showDoneState();
            return;
        }

        // Progress (0–100)
        int progress = (total > 0) ? (int) ((current * 100) / total) : 0;

        tvNowServing.setText(String.format("#%03d", current));
        tvAheadCount.setText(String.valueOf(ahead));
        tvWaitTime.setText("~" + wait + "m");
        progressQueue.setProgress(progress);
        tvProgressStart.setText(String.format("#%03d", current));

        // Status badge
        if (current + 1 == myTokenNumber) {
            tvTokenStatus.setText("● Your Turn!");
            tvTokenStatus.setTextColor(0xFF00E676);
        } else {
            tvTokenStatus.setText("● Waiting");
            tvTokenStatus.setTextColor(0xFFFFD54F);
        }

        // Proximity alert
        if (ahead <= ALERT_THRESHOLD && ahead > 0) {
            tvAlertMessage.setText("Only " + ahead + " token" + (ahead == 1 ? "" : "s") + " ahead of you");
            cardAlert.setVisibility(View.VISIBLE);
        } else {
            cardAlert.setVisibility(View.GONE);
        }
    }

    private void showDoneState() {
        tvTokenStatus.setText("● Served");
        tvTokenStatus.setTextColor(0xFF00E676);
        cardAlert.setVisibility(View.GONE);
        cardDone.setVisibility(View.VISIBLE);
        progressQueue.setProgress(100);
        tvAheadCount.setText("0");
        tvWaitTime.setText("Done");
        tvNowServing.setText(String.format("#%03d", myTokenNumber));
    }

    // ── Cancel Token Dialog ──
    private void showCancelDialog() {
        new AlertDialog.Builder(this, R.style.AlertDialogDark)
                .setTitle("Cancel Token?")
                .setMessage("Are you sure you want to cancel token #" +
                        String.format("%03d", myTokenNumber) + "?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelToken())
                .setNegativeButton("Keep Token", null)
                .show();
    }

    private void cancelToken() {
        if (mAuth.getCurrentUser() == null || businessId == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        // Remove user's token record
        db.child("userTokens").child(uid).child(businessId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Token cancelled", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, Activity_Userdashboard.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to cancel: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ── Helper ──
    private long getOrDefault(DataSnapshot snap, String key, long def) {
        DataSnapshot child = snap.child(key);
        if (child.exists() && child.getValue(Long.class) != null) {
            return child.getValue(Long.class);
        }
        return def;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queueListener != null && businessId != null) {
            db.child("queue").child(businessId).removeEventListener(queueListener);
        }
    }
}