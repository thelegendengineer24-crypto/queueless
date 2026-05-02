package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class activity_tokengenerate extends AppCompatActivity {

    // Views
    private AutoCompleteTextView businessSpinner;
    private TextInputEditText etSearchBusiness;
    private MaterialButton btnGenerateToken, btnViewStatus, btnGoHome;
    private TextView tvSelectedBusiness, tvNowServing, tvPeopleAhead,
            tvEstWait, txtTokenNumber, tvTokenBusiness, tvPosition, tvWaitTime;
    private CardView cardQueuePreview, cardSuccess;
    private android.widget.LinearLayout layoutSelectedBusiness;

    // Firebase
    private DatabaseReference db;
    private FirebaseAuth mAuth;

    // State
    private String selectedBusinessId = null;
    private String selectedBusinessName = null;
    private List<String> businessNames = new ArrayList<>();
    private List<String> businessIds = new ArrayList<>();
    private ValueEventListener queueListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tokengenerate);

        db = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        loadBusinesses();
        setupListeners();
    }

    private void initViews() {
        businessSpinner       = findViewById(R.id.businessSpinner);
        etSearchBusiness      = findViewById(R.id.etSearchBusiness);
        btnGenerateToken      = findViewById(R.id.btnGenerateToken);
        btnViewStatus         = findViewById(R.id.btnViewStatus);
        btnGoHome             = findViewById(R.id.btnGoHome);
        tvSelectedBusiness    = findViewById(R.id.tvSelectedBusiness);
        tvNowServing          = findViewById(R.id.tvNowServing);
        tvPeopleAhead         = findViewById(R.id.tvPeopleAhead);
        tvEstWait             = findViewById(R.id.tvEstWait);
        txtTokenNumber        = findViewById(R.id.txtTokenNumber);
        tvTokenBusiness       = findViewById(R.id.tvTokenBusiness);
        tvPosition            = findViewById(R.id.tvPosition);
        tvWaitTime            = findViewById(R.id.tvWaitTime);
        cardQueuePreview      = findViewById(R.id.cardQueuePreview);
        cardSuccess           = findViewById(R.id.cardSuccess);
        layoutSelectedBusiness = findViewById(R.id.layoutSelectedBusiness);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // ── Load all businesses from Firebase ──
    private void loadBusinesses() {
        db.child("businesses").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                businessNames.clear();
                businessIds.clear();
                for (DataSnapshot biz : snapshot.getChildren()) {
                    String name = biz.child("name").getValue(String.class);
                    if (name != null) {
                        businessNames.add(name);
                        businessIds.add(biz.getKey());
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        activity_tokengenerate.this,
                        android.R.layout.simple_dropdown_item_1line,
                        businessNames
                );
                businessSpinner.setAdapter(adapter);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(activity_tokengenerate.this,
                        "Failed to load businesses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {

        // Spinner selection
        businessSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedBusinessId   = businessIds.get(position);
            selectedBusinessName = businessNames.get(position);
            tvSelectedBusiness.setText(selectedBusinessName);
            layoutSelectedBusiness.setVisibility(View.VISIBLE);
            cardSuccess.setVisibility(View.GONE);
            loadLiveQueuePreview(selectedBusinessId);
        });

        // Search filter
        etSearchBusiness.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // AutoCompleteTextView filter is handled automatically
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Generate Token
        btnGenerateToken.setOnClickListener(v -> generateToken());

        // Track Token → go to TokenStatus
        btnViewStatus.setOnClickListener(v -> {
            Intent intent = new Intent(this, Activity_Tokenstatus);
            intent.putExtra("businessId",   selectedBusinessId);
            intent.putExtra("businessName", selectedBusinessName);
            intent.putExtra("tokenNumber",  txtTokenNumber.getText().toString()
                    .replace("#", "").trim());
            startActivity(intent);
        });

        // Go Home
        btnGoHome.setOnClickListener(v -> {
            Intent intent = new Intent(this,Activity_Userdashboard);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    // ── Real-time queue preview before generating ──
    private void loadLiveQueuePreview(String bizId) {
        if (queueListener != null) {
            db.child("queue").child(bizId).removeEventListener(queueListener);
        }
        cardQueuePreview.setVisibility(View.VISIBLE);
        queueListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvNowServing.setText("#0");
                    tvPeopleAhead.setText("0");
                    tvEstWait.setText("~0 min");
                    return;
                }
                long current = getOrDefault(snapshot, "currentToken", 0L);
                long total   = getOrDefault(snapshot, "totalTokens",  0L);
                long avgTime = getOrDefault(snapshot, "avgTime",       5L);
                long ahead   = Math.max(0, total - current);
                long wait    = ahead * avgTime;

                tvNowServing.setText(String.format("#%03d", current));
                tvPeopleAhead.setText(String.valueOf(ahead));
                tvEstWait.setText("~" + wait + " min");
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        };
        db.child("queue").child(bizId).addValueEventListener(queueListener);
    }

    // ── Core: generate token atomically ──
    private void generateToken() {
        if (selectedBusinessId == null) {
            Toast.makeText(this, "Please select a business first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerateToken.setEnabled(false);
        btnGenerateToken.setText("Generating...");

        DatabaseReference queueRef = db.child("queue").child(selectedBusinessId);

        queueRef.child("totalTokens").get().addOnSuccessListener(snapshot -> {
            long newToken = 1;
            if (snapshot.exists() && snapshot.getValue(Long.class) != null) {
                newToken = snapshot.getValue(Long.class) + 1;
            }
            final long tokenNumber = newToken;
            final String uid = mAuth.getCurrentUser().getUid();

            // Update totalTokens in queue
            queueRef.child("totalTokens").setValue(tokenNumber);

            // Save token under user's record
            db.child("userTokens").child(uid).child(selectedBusinessId)
                    .setValue(tokenNumber);

            // Get avgTime for wait estimate
            queueRef.child("avgTime").get().addOnSuccessListener(avgSnap -> {
                long avgTime;
                if (avgSnap.exists() && avgSnap.getValue(Long.class) != null) {
                    avgTime = avgSnap.getValue(Long.class);
                } else {
                    avgTime = 5L;
                }
                queueRef.child("currentToken").get().addOnSuccessListener(curSnap -> {
                    long current = 0L;
                    if (curSnap.exists() && curSnap.getValue(Long.class) != null) {
                        current = curSnap.getValue(Long.class);
                    }
                    long ahead = Math.max(0, tokenNumber - current);
                    long wait  = ahead * avgTime;

                    // Show success card
                    txtTokenNumber.setText(String.format("#%03d", tokenNumber));
                    tvTokenBusiness.setText(selectedBusinessName);
                    tvPosition.setText(String.valueOf(ahead));
                    tvWaitTime.setText("~" + wait + " min");

                    cardSuccess.setVisibility(View.VISIBLE);
                    btnGenerateToken.setText("Generate Another");
                    btnGenerateToken.setEnabled(true);

                    Toast.makeText(this, "Token Generated Successfully!", Toast.LENGTH_SHORT).show();
                });
            });

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnGenerateToken.setText("Generate Token");
            btnGenerateToken.setEnabled(true);
        });
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
        if (queueListener != null && selectedBusinessId != null) {
            db.child("queue").child(selectedBusinessId).removeEventListener(queueListener);
        }
    }
}