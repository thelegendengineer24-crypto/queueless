package com.example.queueless;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.gridlayout.widget.GridLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class activity_tokenbooking extends AppCompatActivity {

    // Views
    private LinearLayout layoutDayChips;
    private GridLayout gridSlots;
    private TextView tvSelectedDate, tvSummaryBusiness, tvSummaryDate,
            tvSummarySlot, tvSummaryToken, tvBookingBusinessName, tvBookingBusinessType;
    private CardView cardBookingSummary;
    private MaterialButton btnConfirmBooking, btnGoHome;

    // Firebase
    private DatabaseReference db;
    private FirebaseAuth mAuth;

    // State
    private String businessId;
    private String businessName;
    private String selectedDateKey;   // e.g. "2026-05-02"
    private String selectedDateLabel; // e.g. "Friday, 02 May 2026"
    private String selectedSlot;      // e.g. "10:30 AM"
    private View  selectedDayView;
    private View  selectedSlotView;

    // Predefined slots — in a real app fetch these from Firebase per business
    private static final String[] TIME_SLOTS = {
            "09:00 AM", "09:30 AM", "10:00 AM",
            "10:30 AM", "11:00 AM", "11:30 AM",
            "12:00 PM", "02:00 PM", "02:30 PM",
            "03:00 PM", "03:30 PM", "04:00 PM"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tokenbooking);

        db    = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        businessId   = getIntent().getStringExtra("businessId");
        businessName = getIntent().getStringExtra("businessName");

        initViews();
        populateDayChips();
        populateTimeSlots();
        setupListeners();
    }

    private void initViews() {
        layoutDayChips       = findViewById(R.id.layoutDayChips);
        gridSlots            = findViewById(R.id.gridSlots);
        tvSelectedDate       = findViewById(R.id.tvSelectedDate);
        tvSummaryBusiness    = findViewById(R.id.tvSummaryBusiness);
        tvSummaryDate        = findViewById(R.id.tvSummaryDate);
        tvSummarySlot        = findViewById(R.id.tvSummarySlot);
        tvSummaryToken       = findViewById(R.id.tvSummaryToken);
        tvBookingBusinessName = findViewById(R.id.tvBookingBusinessName);
        tvBookingBusinessType = findViewById(R.id.tvBookingBusinessType);
        cardBookingSummary   = findViewById(R.id.cardBookingSummary);
        btnConfirmBooking    = findViewById(R.id.btnConfirmBooking);
        btnGoHome            = findViewById(R.id.btnGoHome);

        tvBookingBusinessName.setText(businessName != null ? businessName : "Business");
        tvSummaryBusiness.setText(businessName != null ? businessName : "Business");

        // Remove the placeholder chip from XML
        layoutDayChips.removeAllViews();

        // Remove placeholder gridSlots children
        gridSlots.removeAllViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // ── Build next 7 days as chips ──
    private void populateDayChips() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dayName = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dayNum  = new SimpleDateFormat("dd",  Locale.getDefault());
        SimpleDateFormat keyFmt  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelFmt = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            final String key   = keyFmt.format(cal.getTime());
            final String label = labelFmt.format(cal.getTime());
            final String day   = dayName.format(cal.getTime()).toUpperCase();
            final String num   = dayNum.format(cal.getTime());

            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(64), dpToPx(72));
            params.setMarginEnd(dpToPx(10));
            chip.setLayoutParams(params);
            chip.setBackgroundResource(R.drawable.bg_quick_action_dark);
            chip.setPadding(0, dpToPx(8), 0, dpToPx(8));

            TextView tvDay = new TextView(this);
            tvDay.setText(day);
            tvDay.setTextSize(10);
            tvDay.setTextColor(Color.parseColor("#A8C4FF"));
            tvDay.setGravity(Gravity.CENTER);

            TextView tvNum = new TextView(this);
            tvNum.setText(num);
            tvNum.setTextSize(20);
            tvNum.setTextStyle(android.graphics.Typeface.BOLD);
            tvNum.setTextColor(Color.WHITE);
            tvNum.setGravity(Gravity.CENTER);

            chip.addView(tvDay);
            chip.addView(tvNum);

            chip.setOnClickListener(v -> {
                if (selectedDayView != null) {
                    selectedDayView.setBackgroundResource(R.drawable.bg_quick_action_dark);
                }
                chip.setBackgroundResource(R.drawable.bg_quick_action_blue);
                selectedDayView   = chip;
                selectedDateKey   = key;
                selectedDateLabel = label;
                tvSelectedDate.setText(label);
                updateSummaryIfReady();
                // Optionally fetch booked slots for this date from Firebase here
            });

            // Auto-select today
            if (i == 0) {
                chip.setBackgroundResource(R.drawable.bg_quick_action_blue);
                selectedDayView   = chip;
                selectedDateKey   = key;
                selectedDateLabel = label;
                tvSelectedDate.setText(label);
            }

            layoutDayChips.addView(chip);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    // ── Build time slot grid ──
    private void populateTimeSlots() {
        for (int i = 0; i < TIME_SLOTS.length; i++) {
            final String slot = TIME_SLOTS[i];

            TextView tvSlot = new TextView(this);
            tvSlot.setText(slot);
            tvSlot.setTextSize(12);
            tvSlot.setTextColor(Color.WHITE);
            tvSlot.setGravity(Gravity.CENTER);
            tvSlot.setBackgroundResource(R.drawable.bg_quick_action_dark);

            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width  = 0;
            gp.height = dpToPx(44);
            gp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            gp.rowSpec    = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            gp.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            tvSlot.setLayoutParams(gp);

            tvSlot.setOnClickListener(v -> {
                if (selectedSlotView != null) {
                    selectedSlotView.setBackgroundResource(R.drawable.bg_quick_action_dark);
                }
                tvSlot.setBackgroundResource(R.drawable.bg_quick_action_blue);
                selectedSlotView = tvSlot;
                selectedSlot     = slot;
                updateSummaryIfReady();
            });

            gridSlots.addView(tvSlot);
        }
    }

    private void setupListeners() {
        btnConfirmBooking.setOnClickListener(v -> confirmBooking());

        btnGoHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, Activity_Userdashboard);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void updateSummaryIfReady() {
        if (selectedDateKey != null && selectedSlot != null) {
            tvSummaryDate.setText(selectedDateLabel);
            tvSummarySlot.setText(selectedSlot);
            cardBookingSummary.setVisibility(View.VISIBLE);
            // Fetch next token number for preview
            fetchTokenPreview();
        }
    }

    private void fetchTokenPreview() {
        if (businessId == null) return;
        db.child("queue").child(businessId).child("totalTokens")
                .get().addOnSuccessListener(snap -> {
                    long nextToken = 1;
                    if (snap.exists() && snap.getValue(Long.class) != null) {
                        nextToken = snap.getValue(Long.class) + 1;
                    }
                    tvSummaryToken.setText(String.format("#%03d", nextToken));
                });
    }

    private void confirmBooking() {
        if (selectedDateKey == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedSlot == null) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }
        if (businessId == null) {
            Toast.makeText(this, "Business not found", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmBooking.setEnabled(false);
        btnConfirmBooking.setText("Booking...");

        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference queueRef = db.child("queue").child(businessId);

        // Atomic: increment token and save booking
        queueRef.child("totalTokens").get().addOnSuccessListener(snap -> {
            long newToken = 1;
            if (snap.exists() && snap.getValue(Long.class) != null) {
                newToken = snap.getValue(Long.class) + 1;
            }
            final long tokenNumber = newToken;

            // Save booking record
            String bookingId = db.child("bookings").push().getKey();
            if (bookingId == null) {
                Toast.makeText(this, "Booking failed. Try again.", Toast.LENGTH_SHORT).show();
                btnConfirmBooking.setEnabled(true);
                btnConfirmBooking.setText("Confirm Booking");
                return;
            }

            java.util.Map<String, Object> booking = new java.util.HashMap<>();
            booking.put("uid",          uid);
            booking.put("businessId",   businessId);
            booking.put("businessName", businessName);
            booking.put("date",         selectedDateKey);
            booking.put("slot",         selectedSlot);
            booking.put("token",        tokenNumber);
            booking.put("status",       "confirmed");
            booking.put("timestamp",    System.currentTimeMillis());

            db.child("bookings").child(bookingId).setValue(booking);
            db.child("userBookings").child(uid).child(bookingId).setValue(booking);
            queueRef.child("totalTokens").setValue(tokenNumber);

            // Navigate to status screen
            Intent intent = new Intent(this, activity_tokenstatus.class);
            intent.putExtra("businessId",   businessId);
            intent.putExtra("businessName", businessName);
            intent.putExtra("tokenNumber",  String.valueOf(tokenNumber));
            startActivity(intent);
            finish();

            Toast.makeText(this, "Booking Confirmed! Token #" +
                    String.format("%03d", tokenNumber), Toast.LENGTH_LONG).show();

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnConfirmBooking.setEnabled(true);
            btnConfirmBooking.setText("Confirm Booking");
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}