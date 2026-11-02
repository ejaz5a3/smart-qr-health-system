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

public class ReceptionRegisterActivity extends AppCompatActivity {

    EditText etName, etPhone, etEmail, etHospital, etPassword, etAddress;
    Button btnRegisterReception;
    TextInputLayout passwordLayout;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reception_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etHospital = findViewById(R.id.etHospital);
        etAddress = findViewById(R.id.etAddress);
        etPassword = findViewById(R.id.etPassword);
        passwordLayout = findViewById(R.id.passwordLayout);
        btnRegisterReception = findViewById(R.id.btnRegisterReception);

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

        btnRegisterReception.setOnClickListener(v -> {
            if (etName.getText().toString().trim().isEmpty() ||
                    etEmail.getText().toString().trim().isEmpty() ||
                    etPhone.getText().toString().trim().isEmpty() ||
                    etHospital.getText().toString().trim().isEmpty() ||
                    etAddress.getText().toString().trim().isEmpty() ||
                    etPassword.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String phone = etPhone.getText().toString().trim();
            String phonePattern = "^\\+91[0-9]{10}$";
            if (!phone.matches(phonePattern)) {
                Toast.makeText(this, "Enter valid 10-digit phone number with +91", Toast.LENGTH_LONG).show();
                return;
            }

            if (passwordLayout.getError() != null) {
                Toast.makeText(this, "Please fix password errors", Toast.LENGTH_SHORT).show();
                return;
            }

            registerReception();
        });
    }

    private void registerReception() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    Map<String, Object> reception = new HashMap<>();
                    reception.put("name", etName.getText().toString().trim());
                    reception.put("email", email);
                    reception.put("phone", etPhone.getText().toString().trim());
                    reception.put("hospital", etHospital.getText().toString().trim());
                    reception.put("address", etAddress.getText().toString().trim());
                    reception.put("role", "receptionist");

                    db.collection("professionals").document(uid).set(reception)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Receptionist Registered Successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, StaffDashboardActivity.class);
                                intent.putExtra("ROLE", "receptionist");
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