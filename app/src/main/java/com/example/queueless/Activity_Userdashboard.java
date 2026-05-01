package com.example.queueless;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import androidx.appcompat.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class Activity_Userdashboard extends AppCompatActivity {

    private TextView tvWelcome, tvSelectedBusiness;
    private Button btnGenerateToken, btnViewStatus, btnHistory, btnLogout;
    private Spinner spinnerBusiness;
    private SearchView searchBusiness;

    private List<String> businessList = new ArrayList<>();
    private List<String> filteredList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userdashboard);

        initViews();
        setupUserName();
        setupBusinessList();
        setupSpinner();
        setupSearch();
        setupButtons();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvSelectedBusiness = findViewById(R.id.tvSelectedBusiness);
        btnGenerateToken = findViewById(R.id.btnGenerateToken);
        btnViewStatus = findViewById(R.id.btnViewStatus);
        btnHistory = findViewById(R.id.btnHistory);
        btnLogout = findViewById(R.id.btnLogout);
        spinnerBusiness = findViewById(R.id.spinnerBusiness);
        searchBusiness = findViewById(R.id.searchBusiness);
    }

    private void setupUserName() {
        String userName = getIntent().getStringExtra("USERNAME");
        if (userName == null || userName.isEmpty()) {
            userName = "User";
        }
        tvWelcome.setText("Welcome, " + userName + " 👋");
    }

    private void setupBusinessList() {
        businessList.add("Unique Clinic");
        businessList.add("Matoshree Hospital");
        businessList.add("Sam Dental");
        businessList.add("Nayan Eye Specialist");
        businessList.add("Shiv Restaurant");

        filteredList.addAll(businessList);
    }

    private void setupSpinner() {
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                filteredList);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBusiness.setAdapter(adapter);

        spinnerBusiness.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedBusiness = filteredList.get(position);
                tvSelectedBusiness.setText("Selected Business: " + selectedBusiness);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tvSelectedBusiness.setText("No business selected");
            }
        });
    }

    private void setupSearch() {
        searchBusiness.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterBusinesses(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterBusinesses(newText);
                return true;
            }
        });
    }

    private void setupButtons() {

        btnGenerateToken.setOnClickListener(v ->
                startActivity(new Intent(this, Activity_Tokengenerate.class)));

        btnViewStatus.setOnClickListener(v ->
                startActivity(new Intent(this, Activity_Tokenstatus.class)));

        btnHistory.setOnClickListener(v ->
                Toast.makeText(this, "History coming soon", Toast.LENGTH_SHORT).show());

        btnLogout.setOnClickListener(v -> finish());
    }

    private void filterBusinesses(String query) {
        filteredList.clear();

        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(businessList);
        } else {
            query = query.toLowerCase();
            for (String business : businessList) {
                if (business.toLowerCase().contains(query)) {
                    filteredList.add(business);
                }
            }
        }

        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No business found", Toast.LENGTH_SHORT).show();
        }

        adapter.notifyDataSetChanged();
    }
}