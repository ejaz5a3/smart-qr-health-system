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

public class NurseRegisterActivity extends AppCompatActivity {

    EditText etName, etPhone, etEmail, etAddress, etHospital, etDepartment, etRegistrationNumber, etPassword;
    Button btnRegister;
    TextInputLayout passwordLayout;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nurse_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        etPhone = findViewById(R.id.etPhone);
        etHospital = findViewById(R.id.etHospital);
        etDepartment = findViewById(R.id.etDepartment);
        etRegistrationNumber = findViewById(R.id.etRegistrationNumber);
        etPassword = findViewById(R.id.etPassword);
        passwordLayout = findViewById(R.id.passwordLayout);
        btnRegister = findViewById(R.id.btnRegisterNurse);

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
            String department = etDepartment.getText().toString().trim();
            String registrationNumber = etRegistrationNumber.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || hospital.isEmpty() || 
                department.isEmpty() || registrationNumber.isEmpty() || address.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidNurseReg(registrationNumber)) {
                etRegistrationNumber.setError("Invalid Registration Number (Ex: RN12345 or APN/12345)");
                etRegistrationNumber.requestFocus();
                return;
            }

            String phonePattern = "^\\+91[0-9]{10}$";
            if (!phone.matches(phonePattern)) {
                Toast.makeText(this, "Enter valid 10-digit phone number with +91", Toast.LENGTH_LONG).show();
                return;
            }

            if (passwordLayout.getError() != null) {
                Toast.makeText(this, "Please fix password errors", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if registration number exists
            db.collection("professionals")
                    .whereEqualTo("registrationNumber", registrationNumber)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(this, "Nurse registration number already exists", Toast.LENGTH_LONG).show();
                        } else {
                            registerNurse(email, password, name, phone, hospital, department, registrationNumber, address);
                        }
                    });
        });
    }

    private boolean isValidNurseReg(String nurse) {
        // Hardcoded for demo bypass
        if (nurse.equalsIgnoreCase("RN12345")) return true;
        
        // Flexible pattern for Indian Nursing Councils
        // Allows: APN/12345, TS/67890, RN123456, INC/2020/9876, etc.
        String nursePattern = "^[A-Z]{2,4}[A-Z0-9/\\-]{3,15}$";
        return nurse.matches(nursePattern);
    }

    private void registerNurse(String email, String password, String name, String phone, String hospital, String department, String registrationNumber, String address) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    Map<String, Object> nurse = new HashMap<>();
                    nurse.put("name", name);
                    nurse.put("email", email);
                    nurse.put("phone", phone);
                    nurse.put("hospital", hospital);
                    nurse.put("department", department);
                    nurse.put("registrationNumber", registrationNumber);
                    nurse.put("address", address);
                    nurse.put("role", "nurse");

                    db.collection("professionals").document(uid).set(nurse)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Nurse Registered Successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, StaffDashboardActivity.class);
                                intent.putExtra("ROLE", "nurse");
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}