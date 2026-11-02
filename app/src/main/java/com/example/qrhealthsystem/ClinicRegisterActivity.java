package com.example.qrhealthsystem;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputLayout;

import android.text.TextWatcher;
import android.text.Editable;

import java.util.HashMap;
import java.util.Map;

public class ClinicRegisterActivity extends AppCompatActivity {

    EditText etClinicName, etEmail, etOwnerName, etRegistrationNumber, etAddress, etPhone, etPassword;
    Button btnRegister;

    FirebaseAuth mAuth;
    TextInputLayout passwordLayout;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clinic_register);

        mAuth = FirebaseAuth.getInstance();

        etClinicName = findViewById(R.id.etClinicName);
        etEmail = findViewById(R.id.etEmail);
        etOwnerName = findViewById(R.id.etOwnerName);
        etRegistrationNumber = findViewById(R.id.etRegistrationNumber);
        etAddress = findViewById(R.id.etAddress);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        passwordLayout = findViewById(R.id.passwordLayout);
        btnRegister = findViewById(R.id.btnRegisterClinic);

        etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etPhone.getText().toString().isEmpty()) {
                etPhone.setText("+91");
                etPhone.setSelection(etPhone.getText().length());
            }
        });

        etPhone.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){}
            public void afterTextChanged(Editable s){
                if(!s.toString().startsWith("+91")){
                    etPhone.setText("+91");
                    etPhone.setSelection(etPhone.getText().length());
                }
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){}
            public void afterTextChanged(Editable s){
                String password = s.toString();
                String passwordPattern = "^(?=(?:.*[^a-zA-Z0-9]){2,})(?=.*[A-Za-z])(?=.*\\d).{8,}$";
                if(!password.matches(passwordPattern)){
                    passwordLayout.setError("Password must contain:\n• At least 8 characters\n• Letters\n• Numbers\n• Minimum 2 special characters");
                } else {
                    passwordLayout.setError(null);
                }
            }
        });

        btnRegister.setOnClickListener(v -> {
            String clinicName = etClinicName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String ownerName = etOwnerName.getText().toString().trim();
            String registrationNumber = etRegistrationNumber.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (clinicName.isEmpty() || email.isEmpty() || phone.isEmpty() || ownerName.isEmpty() || 
                registrationNumber.isEmpty() || address.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidClinicReg(registrationNumber)) {
                etRegistrationNumber.setError("Invalid Clinic Reg (Ex: CE/TS/2022/1234)");
                etRegistrationNumber.requestFocus();
                return;
            }

            String phonePattern = "^\\+91[0-9]{10}$";
            if (!phone.matches(phonePattern)) {
                Toast.makeText(this, "Enter valid 10-digit phone number with +91", Toast.LENGTH_LONG).show();
                return;
            }

            // Check if registration number exists
            db.collection("professionals")
                    .whereEqualTo("registrationNumber", registrationNumber)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(this, "Clinic registration number already exists", Toast.LENGTH_LONG).show();
                        } else {
                            registerClinic(email, password, clinicName, phone, ownerName, registrationNumber, address);
                        }
                    });
        });
    }

    private boolean isValidClinicReg(String clinic) {
        // Example: CE/TS/2022/1234
        return clinic.matches("^CE/[A-Z]{2,5}/[0-9]{4}/[0-9]{4,6}$");
    }

    private void registerClinic(String email, String password, String clinicName, String phone, String ownerName, String registrationNumber, String address) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    Map<String, Object> clinic = new HashMap<>();
                    clinic.put("name", clinicName); // Using 'name' for consistency
                    clinic.put("email", email);
                    clinic.put("phone", phone);
                    clinic.put("ownerName", ownerName);
                    clinic.put("registrationNumber", registrationNumber);
                    clinic.put("address", address);
                    clinic.put("role", "clinic");

                    db.collection("professionals").document(uid).set(clinic)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Clinic Registered Successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, StaffDashboardActivity.class);
                                intent.putExtra("ROLE", "clinic");
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