package com.example.qrhealthsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UpdateMedicalActivity extends AppCompatActivity {

    EditText etDiseases, etAllergies, etMedications, etPastSurgeries, etEmergencyHistory;
    EditText etHistoryTitle, etHistoryType, etHistoryHospital, etHistoryDoctor, etHistoryNotes;
    LinearLayout layoutDoctorUpdate, layoutNurseHistory;
    Button btnSave;
    FirebaseFirestore db;
    String userId, role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_medical);

        etDiseases = findViewById(R.id.etDiseases);
        etAllergies = findViewById(R.id.etAllergies);
        etMedications = findViewById(R.id.etMedications);
        etPastSurgeries = findViewById(R.id.etPastSurgeries);
        etEmergencyHistory = findViewById(R.id.etEmergencyHistory);
        
        etHistoryTitle = findViewById(R.id.etHistoryTitle);
        etHistoryType = findViewById(R.id.etHistoryType);
        etHistoryHospital = findViewById(R.id.etHistoryHospital);
        etHistoryDoctor = findViewById(R.id.etHistoryDoctor);
        etHistoryNotes = findViewById(R.id.etHistoryNotes);
        
        layoutDoctorUpdate = findViewById(R.id.layoutDoctorUpdate);
        layoutNurseHistory = findViewById(R.id.layoutNurseHistory);
        btnSave = findViewById(R.id.btnSave);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("USER_ID");
        role = getIntent().getStringExtra("ROLE");

        if (role != null) {
            if (role.equals("doctor")) {
                layoutDoctorUpdate.setVisibility(View.VISIBLE);
                layoutNurseHistory.setVisibility(View.GONE);
                btnSave.setText("Update Medical Record");
            } else if (role.equals("nurse")) {
                layoutDoctorUpdate.setVisibility(View.GONE);
                layoutNurseHistory.setVisibility(View.VISIBLE);
                btnSave.setText("Add to Past History");
            }
        }

        if (userId != null) {
            fetchExistingData();
        }

        btnSave.setOnClickListener(v -> {
            if (role.equals("doctor")) {
                updateMedical();
            } else if (role.equals("nurse")) {
                addPastHistory();
            }
        });
    }

    private void fetchExistingData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> medicalInfo = (Map<String, Object>) documentSnapshot.get("medicalInfo");
                        if (medicalInfo != null) {
                            etDiseases.setText((String) medicalInfo.get("diseases"));
                            etAllergies.setText((String) medicalInfo.get("allergies"));
                            etMedications.setText((String) medicalInfo.get("medications"));
                            etPastSurgeries.setText((String) medicalInfo.get("pastSurgeries"));
                        }
                        etEmergencyHistory.setText(documentSnapshot.getString("emergencyHistory"));
                    }
                });
    }

    private void updateMedical() {
        Map<String, Object> medicalInfo = new HashMap<>();
        medicalInfo.put("diseases", etDiseases.getText().toString().trim());
        medicalInfo.put("allergies", etAllergies.getText().toString().trim());
        medicalInfo.put("medications", etMedications.getText().toString().trim());
        medicalInfo.put("pastSurgeries", etPastSurgeries.getText().toString().trim());

        Map<String, Object> updates = new HashMap<>();
        updates.put("medicalInfo", medicalInfo);
        updates.put("emergencyHistory", etEmergencyHistory.getText().toString().trim());

        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Medical record updated", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void addPastHistory() {
        String title = etHistoryTitle.getText().toString().trim();
        String type = etHistoryType.getText().toString().trim();
        String hospital = etHistoryHospital.getText().toString().trim();
        String doctor = etHistoryDoctor.getText().toString().trim();
        String notes = etHistoryNotes.getText().toString().trim();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (title.isEmpty() || type.isEmpty()) {
            Toast.makeText(this, "Title and Type are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> historyItem = new HashMap<>();
        historyItem.put("date", date);
        historyItem.put("type", type);
        historyItem.put("title", title);
        historyItem.put("hospital", hospital);
        historyItem.put("doctor", doctor);
        historyItem.put("notes", notes);

        db.collection("users").document(userId)
                .update("pastHistory", FieldValue.arrayUnion(historyItem))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "History updated", Toast.LENGTH_LONG).show();
                    finish();
                });
    }
}