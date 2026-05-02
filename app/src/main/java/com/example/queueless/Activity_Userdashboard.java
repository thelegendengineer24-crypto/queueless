package com.example.queueless;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Activity_Userdashboard extends AppCompatActivity {

    // ── Views ──
    private TextView tvWelcome, tvGreeting, tvSelectedBusiness, tvBusinessName;
    private TextView tvTokenNumber, tvTokenStatus, tvNowServing, tvWaitTime;
    private TextView tvActiveBadge, tvQueuePosition;
    private TextInputEditText etSearchBusiness;
    private AutoCompleteTextView spinnerBusiness;
    private MaterialButton btnGenerateToken, btnViewStatus, btnScanQr;
    private LinearLayout cardStatus, cardHistory, cardLogout, layoutSelectedBusiness;
    private ProgressBar progressQueue;
    private ImageView btnNotification, ivAvatar;

    // ── Data ──
    private final List<String> businessList   = new ArrayList<>();
    private final List<String> filteredList   = new ArrayList<>();
    private final List<String> businessIdList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private String selectedBusiness   = "";
    private String selectedBusinessId = "";
    private String userName = "";
    private String userId   = "";

    // ── Firebase ──
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration tokenListener;
    private ListenerRegistration queueListener;

    // ── Camera permission launcher ──
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    // ─────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userdashboard);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) userId = auth.getCurrentUser().getUid();

        registerCameraPermission();

        initViews();
        setGreeting();
        fetchUserName();
        setupSpinnerAdapter();
        loadBusinesses();
        setupSearch();
        setupSpinnerSelection();
        setupButtons();
        loadActiveToken();
    }

    // ─────────────────────────────────────────
    //  1. Views
    // ─────────────────────────────────────────
    @SuppressLint("WrongViewCast")
    private void initViews() {
        tvGreeting             = findViewById(R.id.tvGreeting);
        tvWelcome              = findViewById(R.id.tvWelcome);
        tvActiveBadge          = findViewById(R.id.tvActiveBadge);
        btnNotification        = findViewById(R.id.btnNotification);
        ivAvatar               = findViewById(R.id.ivAvatar);
        tvTokenNumber          = findViewById(R.id.tvTokenNumber);
        tvTokenStatus          = findViewById(R.id.tvTokenStatus);
        tvBusinessName         = findViewById(R.id.tvBusinessName);
        tvNowServing           = findViewById(R.id.tvNowServing);
        tvWaitTime             = findViewById(R.id.tvWaitTime);
        tvQueuePosition        = findViewById(R.id.tvQueuePosition);
        progressQueue          = findViewById(R.id.progressQueue);
        btnViewStatus          = findViewById(R.id.btnViewStatus);
        etSearchBusiness       = findViewById(R.id.etSearchBusiness);
        spinnerBusiness        = findViewById(R.id.spinnerBusiness);
        layoutSelectedBusiness = findViewById(R.id.layoutSelectedBusiness);
        tvSelectedBusiness     = findViewById(R.id.tvSelectedBusiness);
        btnGenerateToken       = findViewById(R.id.btnGenerateToken);
        btnScanQr              = findViewById(R.id.btnScanQr);
        cardStatus             = findViewById(R.id.cardStatus);
        cardHistory            = findViewById(R.id.cardHistory);
        cardLogout             = findViewById(R.id.cardLogout);

        layoutSelectedBusiness.setVisibility(View.GONE);
        setEmptyTokenCard();
    }

    // ─────────────────────────────────────────
    //  2. Time-based Greeting
    // ─────────────────────────────────────────
    private void setGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String g;
        if      (hour >= 5  && hour < 12) g = "Good Morning,";
        else if (hour >= 12 && hour < 17) g = "Good Afternoon,";
        else if (hour >= 17 && hour < 21) g = "Good Evening,";
        else                               g = "Welcome back,";
        tvGreeting.setText(g);
    }

    // ─────────────────────────────────────────
    //  3. Username — Intent + Firestore fallback
    // ─────────────────────────────────────────
    private void fetchUserName() {
        userName = getIntent().getStringExtra("USERNAME");
        if (userName == null || userName.isEmpty()) userName = "User";
        tvWelcome.setText(userName + " 👋");

        if (!userId.isEmpty()) {
            db.collection("Users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            if (name != null && !name.isEmpty()) {
                                userName = name;
                                tvWelcome.setText(userName + " 👋");
                            }
                        }
                    });
        }
    }

    // ─────────────────────────────────────────
    //  4. Spinner Adapter
    // ─────────────────────────────────────────
    private void setupSpinnerAdapter() {
        spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, filteredList);
        spinnerBusiness.setAdapter(spinnerAdapter);
        spinnerBusiness.setThreshold(0);
    }

    // ─────────────────────────────────────────
    //  5. Load Businesses from Firestore
    //     ID parallel list bhi rakho
    // ─────────────────────────────────────────
    private void loadBusinesses() {
        db.collection("businesses").get()
                .addOnSuccessListener(snapshots -> {
                    businessList.clear();
                    filteredList.clear();
                    businessIdList.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            businessList.add(name);
                            businessIdList.add(doc.getId());
                        }
                    }
                    filteredList.addAll(businessList);
                    spinnerAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Business load error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ─────────────────────────────────────────
    //  6. Search filter
    // ─────────────────────────────────────────
    private void setupSearch() {
        etSearchBusiness.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBusinesses(s.toString());
            }
        });
    }

    private void filterBusinesses(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(businessList);
        } else {
            String lq = query.toLowerCase().trim();
            for (String b : businessList) {
                if (b.toLowerCase().contains(lq)) filteredList.add(b);
            }
        }
        spinnerAdapter.notifyDataSetChanged();
        if (!filteredList.isEmpty()) spinnerBusiness.showDropDown();
    }

    // ─────────────────────────────────────────
    //  7. Spinner selection + ID store
    // ─────────────────────────────────────────
    private void setupSpinnerSelection() {
        spinnerBusiness.setOnItemClickListener((parent, view, position, id) -> {
            selectedBusiness = filteredList.get(position);
            int idx = businessList.indexOf(selectedBusiness);
            if (idx >= 0 && idx < businessIdList.size()) {
                selectedBusinessId = businessIdList.get(idx);
            }
            tvSelectedBusiness.setText(selectedBusiness);
            layoutSelectedBusiness.setVisibility(View.VISIBLE);
            etSearchBusiness.setText("");
        });
    }

    // ─────────────────────────────────────────
    //  8. ✅ QR Scanner — Camera permission
    // ─────────────────────────────────────────
    private void registerCameraPermission() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) launchQrScanner();
                    else Toast.makeText(this,
                            "Camera permission chahiye QR scan ke liye!",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void checkAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchQrScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchQrScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Vendor ka QR Code scan karo");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    // ─────────────────────────────────────────
    //  9. ✅ QR Result Handle
    //     Format: "queueless://business/BUSINESS_ID"
    //     Ya sirf plain BUSINESS_ID bhi kaam karega
    // ─────────────────────────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            handleQrResult(result.getContents());
        }
    }

    private void handleQrResult(String scannedData) {
        String businessId;
        if (scannedData.startsWith("queueless://business/")) {
            businessId = scannedData.replace("queueless://business/", "").trim();
        } else {
            businessId = scannedData.trim();
        }

        final String finalId = businessId;

        // Firestore se verify karo
        db.collection("businesses").document(finalId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name == null || name.isEmpty()) name = "Unknown Business";

                        selectedBusiness   = name;
                        selectedBusinessId = finalId;
                        tvSelectedBusiness.setText(selectedBusiness);
                        layoutSelectedBusiness.setVisibility(View.VISIBLE);

                        String finalName = name;
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("✅ Business Found!")
                                .setMessage(finalName + "\n\nToken generate karna chahte hain?")
                                .setPositiveButton("Generate Token", (dialog, which) -> {
                                    Intent intent = new Intent(this, activity_tokengenerate.class);
                                    intent.putExtra("BUSINESS_NAME", finalName);
                                    intent.putExtra("BUSINESS_ID",   finalId);
                                    intent.putExtra("USERNAME",       userName);
                                    startActivity(intent);
                                })
                                .setNegativeButton("Sirf Select Karo", null)
                                .show();
                    } else {
                        Toast.makeText(this, "❌ Invalid QR — Business nahi mila!",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "QR Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ─────────────────────────────────────────
    //  10. Active Token — Real-time listener
    // ─────────────────────────────────────────
    private void loadActiveToken() {
        if (userId.isEmpty()) return;

        if (tokenListener != null) tokenListener.remove();

        tokenListener = db.collection("tokens")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .limit(1)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || snapshots.isEmpty()) {
                        setEmptyTokenCard();
                        return;
                    }

                    QueryDocumentSnapshot tokenDoc =
                            (QueryDocumentSnapshot) snapshots.getDocuments().get(0);

                    String tokenNum   = tokenDoc.getString("tokenNumber");
                    String businessNm = tokenDoc.getString("businessName");
                    String busId      = tokenDoc.getString("businessId");

                    tvTokenNumber.setText("#" + (tokenNum != null ? tokenNum : "---"));
                    tvBusinessName.setText(businessNm != null ? businessNm : "");
                    tvTokenStatus.setText("Active");
                    tvActiveBadge.setVisibility(View.VISIBLE);

                    if (busId != null) listenToQueueStatus(busId, tokenNum);
                });
    }

    // ─────────────────────────────────────────
    //  11. Real-time Queue Status
    // ─────────────────────────────────────────
    private void listenToQueueStatus(String businessId, String myTokenNum) {
        if (queueListener != null) queueListener.remove();

        queueListener = db.collection("businesses").document(businessId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null) return;

                    long current = doc.getLong("currentToken") != null
                            ? doc.getLong("currentToken") : 0;
                    long total   = doc.getLong("totalTokens") != null
                            ? doc.getLong("totalTokens") : 0;
                    long avgTime = doc.getLong("avgTime") != null
                            ? doc.getLong("avgTime") : 5;

                    long myNum = 0;
                    try { myNum = Long.parseLong(myTokenNum != null ? myTokenNum : "0"); }
                    catch (NumberFormatException ignored) {}

                    long ahead = Math.max(0, myNum - current);
                    long wait  = ahead * avgTime;

                    tvNowServing.setText("#" + String.format("%03d", current));
                    tvQueuePosition.setText(ahead > 0 ? ahead + " ahead of you" : "Almost your turn!");
                    tvWaitTime.setText("~" + wait + " min");

                    int progress = total > 0 ? (int)((current * 100) / total) : 0;
                    progressQueue.setProgress(progress);

                    if (myNum > 0 && myNum <= current) {
                        tvTokenStatus.setText("Your Turn! 🎉");
                        tvQueuePosition.setText("It's your turn now!");
                    }
                });
    }

    // ─────────────────────────────────────────
    //  12. Buttons
    // ─────────────────────────────────────────
    private void setupButtons() {

        btnViewStatus.setOnClickListener(v -> {
            Intent intent = new Intent(this, activity_tokenstatus.class);
            intent.putExtra("BUSINESS_ID", selectedBusinessId);
            startActivity(intent);
        });

        btnGenerateToken.setOnClickListener(v -> {
            if (selectedBusiness.isEmpty()) {
                Toast.makeText(this, "Pehle ek business select karo!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, activity_tokengenerate.class);
            intent.putExtra("BUSINESS_NAME", selectedBusiness);
            intent.putExtra("BUSINESS_ID",   selectedBusinessId);
            intent.putExtra("USERNAME",       userName);
            startActivity(intent);
        });

        // ✅ QR Scan button
        btnScanQr.setOnClickListener(v -> checkAndScan());

        // Avatar pe bhi QR scan
        ivAvatar.setOnClickListener(v -> checkAndScan());

        cardStatus.setOnClickListener(v -> {
            Intent intent = new Intent(this, activity_tokenstatus.class);
            intent.putExtra("BUSINESS_ID", selectedBusinessId);
            startActivity(intent);
        });

        cardHistory.setOnClickListener(v ->
                Toast.makeText(this, "History coming soon!", Toast.LENGTH_SHORT).show());

        cardLogout.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Logout")
                        .setMessage("Kya aap logout karna chahte hain?")
                        .setPositiveButton("Logout", (dialog, which) -> {
                            if (tokenListener != null) tokenListener.remove();
                            if (queueListener != null) queueListener.remove();
                            auth.signOut();
                            Intent intent = new Intent(this, Activity_Login.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show());

        btnNotification.setOnClickListener(v ->
                Toast.makeText(this, "Notifications coming soon!", Toast.LENGTH_SHORT).show());
    }

    // ─────────────────────────────────────────
    //  13. Empty Token Card
    // ─────────────────────────────────────────
    private void setEmptyTokenCard() {
        if (tvTokenNumber == null) return;
        tvTokenNumber.setText("---");
        tvBusinessName.setText("No active token");
        tvTokenStatus.setText("Inactive");
        progressQueue.setProgress(0);
        tvWaitTime.setText("-- min");
        tvNowServing.setText("--");
        tvQueuePosition.setText("No active queue");
        tvActiveBadge.setVisibility(View.GONE);
    }

    // ─────────────────────────────────────────
    //  14. Lifecycle
    // ─────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        setGreeting();
        loadBusinesses();
        loadActiveToken();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tokenListener != null) tokenListener.remove();
        if (queueListener != null) queueListener.remove();
    }
}