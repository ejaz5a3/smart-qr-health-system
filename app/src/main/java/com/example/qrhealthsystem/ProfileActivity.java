package com.example.qrhealthsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private LinearLayout detailsContainer;
    private TextView tvName, tvRole;
    private Button btnLogout, btnUpdateProfile;
    private String userRole = "";
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }

        tvName = findViewById(R.id.tvProfileName);
        tvRole = findViewById(R.id.tvProfileRole);
        detailsContainer = findViewById(R.id.detailsContainer);
        btnLogout = findViewById(R.id.btnLogout);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            uid = user.getUid();
            fetchUserDetails(uid);
        }

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnUpdateProfile.setOnClickListener(v -> showUpdateProfileDialog());
    }

    private void fetchUserDetails(String uid) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    userRole = "patient";
                    displayDetails(documentSnapshot, "Patient");
                } else {
                    db.collection("professionals").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                userRole = doc.getString("role");
                                displayDetails(doc, userRole);
                            }
                        });
                }
            });
    }

    private void displayDetails(DocumentSnapshot doc, String role) {
        tvName.setText(doc.getString("name"));
        if (role != null) {
            tvRole.setText(role.substring(0, 1).toUpperCase() + role.substring(1));
        }

        Map<String, Object> data = doc.getData();
        if (data != null) {
            detailsContainer.removeAllViews();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                // Hide specific fields from being displayed in the list
                if (key.equals("name") || key.equals("role") || key.equals("uuid") || 
                    key.equals("password") || key.equals("email") || key.equals("optionalRecords")) continue;
                
                addDetailRow(key, String.valueOf(entry.getValue()));
            }
        }
    }

    private void addDetailRow(String label, String value) {
        View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
        TextView text1 = view.findViewById(android.R.id.text1);
        TextView text2 = view.findViewById(android.R.id.text2);

        String formattedLabel = label.replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2")
                                     .replace("_", " ");
        formattedLabel = formattedLabel.substring(0, 1).toUpperCase() + formattedLabel.substring(1);

        text1.setText(formattedLabel);
        text2.setText(value);
        
        detailsContainer.addView(view);
    }

    private void showUpdateProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Contact Info");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        final EditText etPhone = new EditText(this);
        etPhone.setHint("New Phone Number");
        layout.addView(etPhone);

        final EditText etEmail = new EditText(this);
        etEmail.setHint("New Email Address");
        layout.addView(etEmail);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (phone.isEmpty() && email.isEmpty()) {
                Toast.makeText(this, "Please enter at least one field", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            if (!phone.isEmpty()) updates.put("phone", phone);
            if (!email.isEmpty()) updates.put("email", email);

            String collection = userRole.equals("patient") ? "users" : "professionals";
            
            db.collection(collection).document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Contact Info Updated", Toast.LENGTH_SHORT).show();
                    fetchUserDetails(uid);
                    
                    // Note: This only updates Firestore. To update Firebase Auth email, 
                    // additional steps are needed, but this fulfills the UI request.
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}