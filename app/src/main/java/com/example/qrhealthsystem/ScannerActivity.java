package com.example.qrhealthsystem;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.List;
import java.util.Map;

public class ScannerActivity extends AppCompatActivity {

    Button btnScan, btnUpdate, btnCopy;
    TextView tvResult, tvTitle;
    View resultLayout;
    String role, scannedUUID;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        role = getIntent().getStringExtra("ROLE");
        if (role == null) role = "reception";

        btnScan = findViewById(R.id.btnScan);
        tvResult = findViewById(R.id.tvResult);
        tvTitle = findViewById(R.id.tvTitle);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnCopy = findViewById(R.id.btnCopy);
        resultLayout = findViewById(R.id.resultLayout);

        resultLayout.setVisibility(View.GONE);

        btnScan.setOnClickListener(v -> startScanner());
        
        btnUpdate.setOnClickListener(v -> {
            Intent intent = new Intent(ScannerActivity.this, UpdateMedicalActivity.class);
            intent.putExtra("USER_ID", scannedUUID);
            intent.putExtra("ROLE", role);
            startActivity(intent);
        });

        btnCopy.setOnClickListener(v -> {
            String textToCopy = tvResult.getText().toString();
            if (!textToCopy.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Patient Details", textToCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Details copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan Patient QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        barcodeLauncher.launch(options);
    }

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    scannedUUID = result.getContents().trim();
                    if (role.equals("reception")) {
                        fetchAndShowDetails();
                    } else {
                        promptForPatientPassword();
                    }
                }
            });

    private void promptForPatientPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Patient Verification");
        builder.setMessage("Ask the patient to enter their account password to view details.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Enter Patient Password");
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = 50;
        container.setPadding(padding, padding, padding, padding);
        container.addView(input);
        
        builder.setView(container);

        builder.setPositiveButton("Unlock", (dialog, which) -> {
            String enteredPassword = input.getText().toString().trim();
            if (enteredPassword.isEmpty()) {
                Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
            } else {
                verifyWithFirebase(enteredPassword);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void verifyWithFirebase(String password) {
        db.collection("users").document(scannedUUID).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String storedPassword = document.getString("password");
                        if (password.equals(storedPassword)) {
                            fetchAndShowDetails();
                        } else {
                            Toast.makeText(this, "Incorrect Password! Access Denied.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Patient not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchAndShowDetails() {
        db.collection("users").document(scannedUUID).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        tvResult.setText("User not found");
                        resultLayout.setVisibility(View.VISIBLE);
                        return;
                    }

                    Map<String, Object> basicInfo = (Map<String, Object>) document.get("basicInfo");
                    Map<String, Object> medicalInfo = (Map<String, Object>) document.get("medicalInfo");
                    List<Map<String, Object>> pastHistory = (List<Map<String, Object>>) document.get("pastHistory");

                    String name = basicInfo != null ? (String) basicInfo.get("name") : "N/A";
                    String age = basicInfo != null ? String.valueOf(basicInfo.get("age")) : "N/A";
                    String gender = basicInfo != null ? (String) basicInfo.get("gender") : "N/A";
                    String bloodGroup = basicInfo != null ? (String) basicInfo.get("bloodGroup") : "N/A";
                    
                    String allergies = medicalInfo != null ? (String) medicalInfo.get("allergies") : "N/A";
                    String diseases = medicalInfo != null ? (String) medicalInfo.get("diseases") : "N/A";
                    String medications = medicalInfo != null ? (String) medicalInfo.get("medications") : "N/A";
                    String pastSurgeries = medicalInfo != null ? (String) medicalInfo.get("pastSurgeries") : "N/A";

                    StringBuilder historyBuilder = new StringBuilder();
                    if (pastHistory != null) {
                        for (Map<String, Object> history : pastHistory) {
                            historyBuilder.append("\n\nDate: ").append(history.get("date"))
                                    .append("\nTitle: ").append(history.get("title"))
                                    .append("\nType: ").append(history.get("type"))
                                    .append("\nHospital: ").append(history.get("hospital"))
                                    .append("\nDoctor: ").append(history.get("doctor"))
                                    .append("\nNotes: ").append(history.get("notes"))
                                    .append("\n-------------------------");
                        }
                    }

                    String resultText;
                    btnUpdate.setVisibility(View.GONE);
                    btnCopy.setVisibility(View.GONE);

                    if (role.equals("doctor")) {
                        btnUpdate.setVisibility(View.VISIBLE);
                        tvTitle.setText("Doctor View");
                        resultText = "Name: " + name +
                                     "\n\nAge: " + age + 
                                     "\n\nBlood Group: " + bloodGroup + 
                                     "\n\nDiseases: " + diseases + 
                                     "\n\nAllergies: " + allergies + 
                                     "\n\nMedications: " + medications + 
                                     "\n\nPast Surgeries: " + pastSurgeries +
                                     "\n\n--- PAST HISTORY ---" + (historyBuilder.length() > 0 ? historyBuilder.toString() : "\nNo history available");
                    } else if (role.equals("nurse")) {
                        btnUpdate.setVisibility(View.VISIBLE);
                        tvTitle.setText("Nurse View");
                        resultText = "Name: " + name +
                                     "\n\nAge: " + age + 
                                     "\n\nBlood Group: " + bloodGroup + 
                                     "\n\nDiseases: " + diseases + 
                                     "\n\nMedications: " + medications +
                                     "\n\n--- PAST HISTORY ---" + (historyBuilder.length() > 0 ? historyBuilder.toString() : "\nNo history available");
                    } else {
                        btnCopy.setVisibility(View.VISIBLE);
                        tvTitle.setText("Reception View");
                        resultText = "Name: " + name + 
                                     "\n\nAge: " + age + 
                                     "\n\nGender: " + gender + 
                                     "\n\nPhone: " + document.getString("phone") + 
                                     "\n\nAddress: " + document.getString("address");
                    }

                    tvResult.setText(resultText);
                    resultLayout.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Data Fetched Successfully", Toast.LENGTH_SHORT).show();
                });
    }
}
