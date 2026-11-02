package com.example.qrhealthsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DoctorRegisterActivity extends AppCompatActivity {

    EditText etName, etPhone, etEmail, etHospital, etLicense, etExperience, etPassword, etAddress;
    Button btnRegister;
    TextInputLayout passwordLayout;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etHospital = findViewById(R.id.etHospital);
        etLicense = findViewById(R.id.etLicense);
        etExperience = findViewById(R.id.etExperience);
        etAddress = findViewById(R.id.etAddress);
        etPassword = findViewById(R.id.etPassword);
        passwordLayout = findViewById(R.id.passwordLayout);
        btnRegister = findViewById(R.id.btnRegister);

        etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etPhone.getText().toString().isEmpty()) {
                etPhone.setText("+91");
                etPhone.setSelection(etPhone.getText().length());
            }
        });

        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().startsWith("+91")) {
                    etPhone.setText("+91");
                    etPhone.setSelection(etPhone.getText().length());
                }
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                String passwordPattern = "^(?=(?:.*[^a-zA-Z0-9]){2,})(?=.*[A-Za-z])(?=.*\\d).{8,}$";
                if (!password.matches(passwordPattern)) {
                    passwordLayout.setError("Password must contain:\n• At least 8 characters\n• Letters\n• Numbers\n• Minimum 2 special characters");
                } else {
                    passwordLayout.setError(null);
                }
            }
        });

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String hospital = etHospital.getText().toString().trim();
            String license = etLicense.getText().toString().trim();
            String experience = etExperience.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || hospital.isEmpty() || 
                license.isEmpty() || experience.isEmpty() || address.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidDoctorLicense(license)) {
                etLicense.setError("Invalid License (Ex: AP/12345)");
                etLicense.requestFocus();
                return;
            }

            if (!phone.matches("^\\+91[0-9]{10}$")) {
                Toast.makeText(this, "Enter valid 10-digit phone number with +91", Toast.LENGTH_LONG).show();
                return;
            }

            if (passwordLayout.getError() != null) {
                Toast.makeText(this, "Please fix password errors", Toast.LENGTH_SHORT).show();
                return;
            }

            // Direct Registration after license format verification
            db.collection("professionals").whereEqualTo("license", license).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "License number already registered", Toast.LENGTH_LONG).show();
                    } else {
                        registerDoctor();
                    }
                });
        });
    }

    private boolean isValidDoctorLicense(String license) {
        if (license.equalsIgnoreCase("AP12345") || license.equalsIgnoreCase("AP/12345")) return true;
        String doctorPattern = "^[A-Z]{2,3}[A-Z0-9/\\-]{3,15}$";
        return license.matches(doctorPattern);
    }

    private void registerDoctor() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Map<String, Object> doctor = new HashMap<>();
                    doctor.put("name", etName.getText().toString().trim());
                    doctor.put("email", email);
                    doctor.put("phone", etPhone.getText().toString().trim());
                    doctor.put("hospital", etHospital.getText().toString().trim());
                    doctor.put("license", etLicense.getText().toString().trim());
                    doctor.put("experience", etExperience.getText().toString().trim());
                    doctor.put("address", etAddress.getText().toString().trim());
                    doctor.put("role", "doctor");
                    doctor.put("password", password);

                    db.collection("professionals").document(authResult.getUser().getUid()).set(doctor)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Doctor Registered Successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, StaffDashboardActivity.class);
                                intent.putExtra("ROLE", "doctor");
                                startActivity(intent);
                                finish();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
