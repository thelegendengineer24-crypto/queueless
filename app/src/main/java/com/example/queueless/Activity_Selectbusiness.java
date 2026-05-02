package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Activity_Selectbusiness extends AppCompatActivity {

    // Views
    private TextInputLayout   tilSearchId;
    private TextInputEditText etSearchId;
    private MaterialButton    btnSearch;
    private ProgressBar       progressBar;
    private TextView          tvSearchError;

    private View     cardBusinessResult;
    private TextView tvBusinessName;
    private TextView tvServiceCount;
    private TextView tvNoServices;
    private RecyclerView recyclerServices;

    // Data
    private FirebaseFirestore db;
    private final List<ServiceItem> serviceList = new ArrayList<>();
    private ServiceAdapter adapter;
    private String businessDocId       = "";
    private String currentBusinessName = "";

    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectbusiness);

        db = FirebaseFirestore.getInstance();
        bindViews();
        setupRecyclerView();
        setupListeners();
    }

    // -------------------------------------------------------------------------
    private void bindViews() {
        tilSearchId        = findViewById(R.id.tilSearchId);
        etSearchId         = findViewById(R.id.etSearchId);
        btnSearch          = findViewById(R.id.btnSearch);
        progressBar        = findViewById(R.id.progressBar);
        tvSearchError      = findViewById(R.id.tvSearchError);
        cardBusinessResult = findViewById(R.id.cardBusinessResult);
        tvBusinessName     = findViewById(R.id.tvBusinessName);
        tvServiceCount     = findViewById(R.id.tvServiceCount);
        tvNoServices       = findViewById(R.id.tvNoServices);
        recyclerServices   = findViewById(R.id.recyclerServices);
    }

    private void setupRecyclerView() {
        adapter = new ServiceAdapter(serviceList, this::onServiceSelected);
        recyclerServices.setLayoutManager(new LinearLayoutManager(this));
        recyclerServices.setAdapter(adapter);
        recyclerServices.setHasFixedSize(false);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> searchBusiness());

        etSearchId.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                searchBusiness();
                return true;
            }
            return false;
        });
    }

    // -------------------------------------------------------------------------
    // SEARCH BUSINESS
    // -------------------------------------------------------------------------
    private void searchBusiness() {
        hideKeyboard();
        clearError();

        String enteredId = etSearchId.getText() != null
                ? etSearchId.getText().toString().trim() : "";

        if (TextUtils.isEmpty(enteredId)) {
            showError("Please enter a Business ID");
            return;
        }

        setLoading(true);
        resetResultCard();

        db.collection("businesses")
                .whereEqualTo("uniqueId", enteredId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    setLoading(false);

                    if (snapshots == null || snapshots.isEmpty()) {
                        showError("No business found with this ID");
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshots) {
                        businessDocId       = doc.getId();
                        currentBusinessName = doc.getString("businessName");

                        tvBusinessName.setText(
                                !TextUtils.isEmpty(currentBusinessName)
                                        ? currentBusinessName : "Unnamed Business");

                        cardBusinessResult.setVisibility(View.VISIBLE);
                        loadServices();
                        break;
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Search failed. Check your connection.");
                });
    }

    // -------------------------------------------------------------------------
    // LOAD SERVICES
    // -------------------------------------------------------------------------
    private void loadServices() {
        if (TextUtils.isEmpty(businessDocId)) return;

        db.collection("businesses")
                .document(businessDocId)
                .collection("services")
                .get()
                .addOnSuccessListener(snapshots -> {
                    serviceList.clear();

                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String name = doc.getString("serviceName");
                            if (!TextUtils.isEmpty(name)) {
                                serviceList.add(new ServiceItem(doc.getId(), name));
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();

                    int count = serviceList.size();
                    tvServiceCount.setText(count + " available");

                    if (count == 0) {
                        tvNoServices.setVisibility(View.VISIBLE);
                        recyclerServices.setVisibility(View.GONE);
                    } else {
                        tvNoServices.setVisibility(View.GONE);
                        recyclerServices.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load services. Try again.",
                                Toast.LENGTH_SHORT).show());
    }

    // -------------------------------------------------------------------------
    // ON SERVICE CLICK → go to Token Booking
    // -------------------------------------------------------------------------
    private void onServiceSelected(ServiceItem service) {
        Intent intent = new Intent(this, activity_tokenbooking.class);
        intent.putExtra("businessDocId",   businessDocId);
        intent.putExtra("businessName",    currentBusinessName);
        intent.putExtra("serviceId",       service.id);
        intent.putExtra("serviceName",     service.name);
        startActivity(intent);
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSearch.setEnabled(!loading);
        etSearchId.setEnabled(!loading);
    }

    private void showError(String msg) {
        tvSearchError.setText(msg);
        tvSearchError.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        tvSearchError.setVisibility(View.GONE);
        tvSearchError.setText("");
    }

    private void resetResultCard() {
        cardBusinessResult.setVisibility(View.GONE);
        serviceList.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        tvNoServices.setVisibility(View.GONE);
        businessDocId       = "";
        currentBusinessName = "";
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    // =========================================================================
    // MODEL
    // =========================================================================
    public static class ServiceItem {
        public final String id;
        public final String name;
        public ServiceItem(String id, String name) {
            this.id   = id;
            this.name = name;
        }
    }

    // =========================================================================
    // ADAPTER
    // =========================================================================
    public static class ServiceAdapter
            extends RecyclerView.Adapter<ServiceAdapter.VH> {

        public interface OnClick { void onClick(ServiceItem s); }

        private final List<ServiceItem> items;
        private final OnClick           listener;

        public ServiceAdapter(List<ServiceItem> items, OnClick listener) {
            this.items    = items;
            this.listener = listener;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_services, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            ServiceItem item = items.get(pos);
            h.tvName.setText(item.name);
            h.tvNumber.setText(String.valueOf(pos + 1));
            h.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvNumber;
            VH(View v) {
                super(v);
                tvName   = v.findViewById(R.id.tvServiceName);
                tvNumber = v.findViewById(R.id.tvServiceNumber);
            }
        }
    }
}