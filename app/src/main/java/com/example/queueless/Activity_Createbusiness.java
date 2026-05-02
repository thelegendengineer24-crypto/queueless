package com.example.queueless;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Activity_Createbusiness extends AppCompatActivity {

    // ── Views ──
    private TextInputEditText etBusinessName, etUniqueId, etOpeningTime, etClosingTime, etMaxTokens;
    private AutoCompleteTextView spinnerBusinessType;
    private MaterialButton btnCreateBusiness, btnCancel;
    private ImageView btnBack;
    private LinearLayout layoutPreview;
    private TextView tvPreviewName, tvPreviewId;

    // ── Firebase ──
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // ── Business Types ──
    private final String[] BUSINESS_TYPES = {
            "Hospital / Clinic",
            "Bank",
            "Government Office",
            "Restaurant",
            "Salon / Spa",
            "Retail Shop",
            "Pharmacy",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createbusiness);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupBusinessTypeDropdown();
        setupTimePickers();
        setupLivePreview();      // ✅ NEW: Real-time preview
        setupButtons();
    }

    // ─────────────────────────────────────────
    //  1. Views initialize
    // ─────────────────────────────────────────
    private void initViews() {
        etBusinessName     = findViewById(R.id.etBusinessName);
        etUniqueId         = findViewById(R.id.etUniqueId);
        etOpeningTime      = findViewById(R.id.etOpeningTime);
        etClosingTime      = findViewById(R.id.etClosingTime);
        etMaxTokens        = findViewById(R.id.etMaxTokens);
        spinnerBusinessType = findViewById(R.id.spinnerBusinessType);
        btnCreateBusiness  = findViewById(R.id.btnCreateBusiness);
        btnCancel          = findViewById(R.id.btnCancel);
        btnBack            = findViewById(R.id.btnBack);
        layoutPreview      = findViewById(R.id.layoutPreview);
        tvPreviewName      = findViewById(R.id.tvPreviewName);
        tvPreviewId        = findViewById(R.id.tvPreviewId);
    }

    // ─────────────────────────────────────────
    //  2. ✅ NEW: Business Type Dropdown
    // ─────────────────────────────────────────
    private void setupBusinessTypeDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                BUSINESS_TYPES
        );
        spinnerBusinessType.setAdapter(adapter);
        spinnerBusinessType.setThreshold(0);
    }

    // ─────────────────────────────────────────
    //  3. ✅ NEW: Time Picker for Open/Close
    //     Click karo toh clock dialog aayega
    // ─────────────────────────────────────────
    private void setupTimePickers() {
        etOpeningTime.setOnClickListener(v -> showTimePicker(etOpeningTime));
        etClosingTime.setOnClickListener(v -> showTimePicker(etClosingTime));
    }

    private void showTimePicker(TextInputEditText targetField) {
        Calendar calendar = Calendar.getInstance();
        int hour   = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            String period = hourOfDay >= 12 ? "PM" : "AM";
            int displayHour = hourOfDay > 12 ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay);
            String time = String.format("%d:%02d %s", displayHour, minuteOfHour, period);
            targetField.setText(time);
        }, hour, minute, false).show();
    }

    // ─────────────────────────────────────────
    //  4. ✅ NEW: Live Preview Card
    //     Jaise type karo, preview update hoga
    // ─────────────────────────────────────────
    private void setupLivePreview() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview();
            }
        };

        etBusinessName.addTextChangedListener(watcher);
        etUniqueId.addTextChangedListener(watcher);
    }

    private void updatePreview() {
        String name = etBusinessName.getText() != null
                ? etBusinessName.getText().toString().trim() : "";
        String id   = etUniqueId.getText() != null
                ? etUniqueId.getText().toString().trim() : "";

        if (!name.isEmpty() || !id.isEmpty()) {
            layoutPreview.setVisibility(View.VISIBLE);
            tvPreviewName.setText(name.isEmpty() ? "Business Name..." : name);
            tvPreviewId.setText("ID: " + (id.isEmpty() ? "---" : id));
        } else {
            layoutPreview.setVisibility(View.GONE);
        }
    }

    // ─────────────────────────────────────────
    //  5. Buttons
    // ─────────────────────────────────────────
    private void setupButtons() {

        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        btnCreateBusiness.setOnClickListener(v -> registerBusiness());
    }

    // ─────────────────────────────────────────
    //  6. Register Business — Firestore
    //     ✅ NEW fields: type, timing, maxTokens
    // ─────────────────────────────────────────
    private void registerBusiness() {
        String name        = etBusinessName.getText() != null ? etBusinessName.getText().toString().trim() : "";
        String uniqueId    = etUniqueId.getText()     != null ? etUniqueId.getText().toString().trim()     : "";
        String type        = spinnerBusinessType.getText().toString().trim();
        String openTime    = etOpeningTime.getText()  != null ? etOpeningTime.getText().toString().trim()  : "";
        String closeTime   = etClosingTime.getText()  != null ? etClosingTime.getText().toString().trim()  : "";
        String maxTokenStr = etMaxTokens.getText()    != null ? etMaxTokens.getText().toString().trim()    : "";

        // ── Validation ──
        if (TextUtils.isEmpty(name)) {
            etBusinessName.setError("Business name required");
            etBusinessName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(uniqueId)) {
            etUniqueId.setError("Unique ID required");
            etUniqueId.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(type)) {
            spinnerBusinessType.setError("Business type select karo");
            spinnerBusinessType.requestFocus();
            return;
        }

        // Max tokens — default 100 agar empty
        int maxTokens = 100;
        if (!maxTokenStr.isEmpty()) {
            maxTokens = Integer.parseInt(maxTokenStr);
            if (maxTokens <= 0) {
                etMaxTokens.setError("Valid number daalo");
                etMaxTokens.requestFocus();
                return;
            }
        }

        // ── Button disable karo double click se bachne ke liye ──
        btnCreateBusiness.setEnabled(false);
        btnCreateBusiness.setText("Registering...");

        // ── Firestore data prepare ──
        String userId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : "";

        Map<String, Object> business = new HashMap<>();
        business.put("name", name);                    // "name" field — UserDashboard search ke liye
        business.put("businessName", name);
        business.put("uniqueId", uniqueId);
        business.put("type", type);                    // ✅ NEW
        business.put("openingTime", openTime);          // ✅ NEW
        business.put("closingTime", closeTime);         // ✅ NEW
        business.put("maxTokensPerDay", maxTokens);     // ✅ NEW
        business.put("adminUid", userId);
        business.put("isActive", true);
        business.put("currentToken", 0);
        business.put("totalTokens", 0);
        business.put("createdAt", FieldValue.serverTimestamp());

        // ── Firestore mein save karo ──
        // Document ID = uniqueId (easy to search karne ke liye)
        db.collection("businesses")
                .document(uniqueId)
                .set(business)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "✅ " + name + " registered successfully!",
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, Activity_Admindashboard.class);
                    intent.putExtra("BUSINESS_NAME", name);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreateBusiness.setEnabled(true);
                    btnCreateBusiness.setText("Register Business");
                    Toast.makeText(this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}