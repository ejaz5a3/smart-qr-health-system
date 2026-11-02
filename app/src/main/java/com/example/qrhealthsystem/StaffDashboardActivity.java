package com.example.qrhealthsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class StaffDashboardActivity extends AppCompatActivity {

    Button btnScan;
    String role;
    ImageView imgRole;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Profile icon click
        toolbar.setNavigationOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        btnScan = findViewById(R.id.btnScan);
        imgRole = findViewById(R.id.imgRole);

        role = getIntent().getStringExtra("ROLE");
        if (role == null) role = "staff";

        // Set Dashboard Title based on role
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(role.substring(0, 1).toUpperCase() + role.substring(1) + " Dashboard");
        }

        btnScan.setOnClickListener(v -> {
            Intent intent = new Intent(StaffDashboardActivity.this, ScannerActivity.class);
            intent.putExtra("ROLE", role);
            startActivity(intent);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(StaffDashboardActivity.this)
                        .setTitle("Exit App")
                        .setMessage("Are you sure you want to exit?")
                        .setPositiveButton("Yes", (dialog, which) -> finishAffinity())
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }
}