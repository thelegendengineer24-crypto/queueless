package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Activity_Userdashboard extends AppCompatActivity {

    // ── Views ──
    private TextView tvWelcome, tvSelectedBusiness, tvBusinessName;
    private TextView tvTokenNumber, tvTokenStatus, tvNowServing, tvWaitTime, tvActiveBadge;
    private TextInputEditText etSearchBusiness;
    private AutoCompleteTextView spinnerBusiness;
    private MaterialButton btnGenerateToken, btnViewStatus;
    private LinearLayout cardStatus, cardHistory, cardLogout, layoutSelectedBusiness;
    private ProgressBar progressQueue;
    private ImageView btnNotification, ivAvatar;

    // ── Data ──
    private final List<String> businessList = new ArrayList<>();
    private final List<String> filteredList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    private String selectedBusiness = "";
    private String userName = "";

    // ── Firebase ──
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userdashboard);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupUserName();
        setupSpinnerAdapter();
        loadBusinessesFromFirebase();   // <-- Firebase se real data
        setupSearch();
        setupSpinnerSelection();
        setupButtons();
    }

    // ─────────────────────────────────────────
    //  1. Views initialize — naye XML IDs se
    // ─────────────────────────────────────────
    private void initViews() {
        // Header
        tvWelcome         = findViewById(R.id.tvWelcome);
        tvActiveBadge     = findViewById(R.id.tvActiveBadge);
        btnNotification   = findViewById(R.id.btnNotification);
        ivAvatar          = findViewById(R.id.ivAvatar);

        // Hero Token Card
        tvTokenNumber     = findViewById(R.id.tvTokenNumber);
        tvTokenStatus     = findViewById(R.id.tvTokenStatus);
        tvBusinessName    = findViewById(R.id.tvBusinessName);
        tvNowServing      = findViewById(R.id.tvNowServing);
        tvWaitTime        = findViewById(R.id.tvWaitTime);
        progressQueue     = findViewById(R.id.progressQueue);
        btnViewStatus     = findViewById(R.id.btnViewStatus);

        // Search + Generate Card
        etSearchBusiness       = findViewById(R.id.etSearchBusiness);
        spinnerBusiness        = findViewById(R.id.spinnerBusiness);
        layoutSelectedBusiness = findViewById(R.id.layoutSelectedBusiness);
        tvSelectedBusiness     = findViewById(R.id.tvSelectedBusiness);
        btnGenerateToken       = findViewById(R.id.btnGenerateToken);

        // Quick Actions
        cardStatus  = findViewById(R.id.cardStatus);
        cardHistory = findViewById(R.id.cardHistory);
        cardLogout  = findViewById(R.id.cardLogout);

        // Hide selected chip aur token card by default
        layoutSelectedBusiness.setVisibility(View.GONE);
        hideTokenCard();
    }

    // ─────────────────────────────────────────
    //  2. Username set karo
    // ─────────────────────────────────────────
    private void setupUserName() {
        userName = getIntent().getStringExtra("USERNAME");
        if (userName == null || userName.isEmpty()) userName = "User";
        tvWelcome.setText(userName + " 👋");
    }

    // ─────────────────────────────────────────
    //  3. Spinner Adapter setup
    // ─────────────────────────────────────────
    private void setupSpinnerAdapter() {
        spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                filteredList
        );
        spinnerBusiness.setAdapter(spinnerAdapter);
        spinnerBusiness.setThreshold(0); // Pehle click pe hi dropdown dikhao
    }

    // ─────────────────────────────────────────
    //  4. Firebase se businesses load karo ✅
    //     FIX: Yahan businesses actually registered
    //     hain woh aayenge, hardcoded nahi
    // ─────────────────────────────────────────
    private void loadBusinessesFromFirebase() {
        db.collection("businesses")   // <-- apna collection name yahan daalo
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    businessList.clear();
                    filteredList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // "name" field Firestore document mein hona chahiye
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            businessList.add(name);
                        }
                    }

                    if (businessList.isEmpty()) {
                        Toast.makeText(this,
                                "Koi business registered nahi hai abhi",
                                Toast.LENGTH_SHORT).show();
                    }

                    filteredList.addAll(businessList);
                    spinnerAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Business load karne mein error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ─────────────────────────────────────────
    //  5. Search — TextInputEditText pe TextWatcher
    //     FIX: SearchView ki jagah EditText use kiya
    //     notifyDataSetChanged() sahi kaam karta hai
    // ─────────────────────────────────────────
    private void setupSearch() {
        etSearchBusiness.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBusinesses(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // ─────────────────────────────────────────
    //  6. Spinner item select hone pe
    // ─────────────────────────────────────────
    private void setupSpinnerSelection() {
        spinnerBusiness.setOnItemClickListener((parent, view, position, id) -> {
            selectedBusiness = filteredList.get(position);
            tvSelectedBusiness.setText(selectedBusiness);
            layoutSelectedBusiness.setVisibility(View.VISIBLE);

            // Search field clear karo
            etSearchBusiness.setText("");

            Toast.makeText(this,
                    selectedBusiness + " selected!",
                    Toast.LENGTH_SHORT).show();
        });
    }

    // ─────────────────────────────────────────
    //  7. Buttons
    // ─────────────────────────────────────────
    private void setupButtons() {

        // Hero Card — Refresh Status
        btnViewStatus.setOnClickListener(v ->
                startActivity(new Intent(this, Activity_Tokenstatus.class)));

        // Generate Token
        btnGenerateToken.setOnClickListener(v -> {
            if (selectedBusiness.isEmpty()) {
                Toast.makeText(this,
                        "Pehle ek business select karo!",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, Activity_Tokengenerate.class);
            intent.putExtra("BUSINESS_NAME", selectedBusiness);
            intent.putExtra("USERNAME", userName);
            startActivity(intent);
        });

        // Quick Actions
        cardStatus.setOnClickListener(v ->
                startActivity(new Intent(this, Activity_Tokenstatus.class)));

        cardHistory.setOnClickListener(v ->
                Toast.makeText(this, "History coming soon!", Toast.LENGTH_SHORT).show());

        cardLogout.setOnClickListener(v -> {
            // Firebase sign out (agar use kar raha hai)
            // FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, Activity_Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Notification Bell
        btnNotification.setOnClickListener(v ->
                Toast.makeText(this, "Notifications coming soon!", Toast.LENGTH_SHORT).show());
    }

    // ─────────────────────────────────────────
    //  8. Search filter logic
    //     FIX: Spinner ke liye sahi tarika —
    //     list clear + refill + notifyDataSetChanged
    // ─────────────────────────────────────────
    private void filterBusinesses(String query) {
        filteredList.clear();

        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(businessList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (String business : businessList) {
                if (business.toLowerCase().contains(lowerQuery)) {
                    filteredList.add(business);
                }
            }
        }

        // ✅ Yeh FIX hai — AutoCompleteTextView ke liye
        // notifyDataSetChanged() sahi kaam karta hai
        spinnerAdapter.notifyDataSetChanged();

        // Dropdown show karo automatically
        if (!filteredList.isEmpty()) {
            spinnerBusiness.showDropDown();
        }
    }

    // ─────────────────────────────────────────
    //  9. Active Token Card show/hide helpers
    //     (Call karo jab token generate ho)
    // ─────────────────────────────────────────
    public void showActiveToken(String tokenNum, String business, int queuePos, int total) {
        tvTokenNumber.setText("#" + tokenNum);
        tvBusinessName.setText(business);
        tvActiveBadge.setVisibility(View.VISIBLE);
        tvTokenStatus.setText("Active");

        // Progress calculate karo
        int progress = (int) (((float)(total - queuePos) / total) * 100);
        progressQueue.setProgress(progress);

        tvWaitTime.setText("~" + (queuePos * 4) + " min wait");
    }

    private void hideTokenCard() {
        // Token card default mein empty state dikhao
        tvTokenNumber.setText("---");
        tvBusinessName.setText("No active token");
        tvTokenStatus.setText("Inactive");
        progressQueue.setProgress(0);
        tvWaitTime.setText("-- min");
        tvNowServing.setText("--");
    }

    // ─────────────────────────────────────────
    //  10. onResume — wapas aane pe refresh karo
    // ─────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        loadBusinessesFromFirebase(); // Fresh data fetch karo
    }
}