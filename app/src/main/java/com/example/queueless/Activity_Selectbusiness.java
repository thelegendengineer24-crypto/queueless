package com.example.queueless;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class Activity_Selectbusiness extends AppCompatActivity {

    TextInputEditText etSearchId;
    MaterialButton btnSearch;
    TextView tvBusinessName;
    RecyclerView recyclerServices;

    FirebaseFirestore db;
    ArrayList<String> serviceList;
    ServiceAdapter adapter;

    String businessDocId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectbusiness);

        etSearchId = findViewById(R.id.etSearchId);
        btnSearch = findViewById(R.id.btnSearch);
        tvBusinessName = findViewById(R.id.tvBusinessName);
        recyclerServices = findViewById(R.id.recyclerServices);

        db = FirebaseFirestore.getInstance();

        serviceList = new ArrayList<>();
        adapter = new ServiceAdapter(serviceList);

        recyclerServices.setLayoutManager(new LinearLayoutManager(this));
        recyclerServices.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> searchBusiness());
    }

    private void searchBusiness() {

        String enteredId = etSearchId.getText().toString().trim();

        db.collection("businesses")
                .whereEqualTo("uniqueId", enteredId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Business Not Found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        businessDocId = doc.getId();
                        tvBusinessName.setText(doc.getString("businessName"));
                        loadServices();
                    }
                });
    }

    private void loadServices() {

        db.collection("businesses")
                .document(businessDocId)
                .collection("services")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    serviceList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        serviceList.add(doc.getString("serviceName"));
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}