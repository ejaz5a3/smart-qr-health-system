package com.example.qrhealthsystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;

public class RegisterActivity extends AppCompatActivity {

    EditText etName, etEmail, etPassword, etPhone, etAge, etAddress, etDiseases, etAllergies, etMedications, etPastSurgeries, etEmergencyContact;
    RadioGroup rgGender, rgBloodGroup;
    Button btnRegister;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    TextInputLayout passwordLayout;

    ArrayList<String> selectedDiseases = new ArrayList<>();
    ArrayList<String> selectedAllergies = new ArrayList<>();
    ArrayList<String> selectedMedications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        passwordLayout = findViewById(R.id.passwordLayout);
        etPhone = findViewById(R.id.etPhone);
        etAge = findViewById(R.id.etAge);
        rgGender = findViewById(R.id.rgGender);
        rgBloodGroup = findViewById(R.id.rgBloodGroup);
        etAddress = findViewById(R.id.etAddress);
        etDiseases = findViewById(R.id.etDiseases);
        etAllergies = findViewById(R.id.etAllergies);
        etMedications = findViewById(R.id.etMedications);
        etPastSurgeries = findViewById(R.id.etPastSurgeries);
        etEmergencyContact = findViewById(R.id.etEmergencyContact);
        btnRegister = findViewById(R.id.btnRegister);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupPhoneInput();
        setupPasswordValidation();
        setupMultiSelectDialogs();

        btnRegister.setOnClickListener(v -> checkPhoneAndRegister());
    }

    private void setupPhoneInput() {
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
    }

    private void setupPasswordValidation() {
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
    }

    private void setupMultiSelectDialogs() {
        String[] diseasesList = {"Diabetes", "Hypertension", "Asthma", "Heart Disease", "Kidney Disease", "Thyroid", "Cancer", "Tuberculosis", "Arthritis", "None"};
        etDiseases.setOnClickListener(v -> showSearchableDialog("Select Diseases", diseasesList, selectedDiseases, etDiseases));

        String[] allergiesList = {"Penicillin", "Sulfa Drugs", "Aspirin", "Ibuprofen", "Latex", "Dust", "Pollen", "Pet Dander", "Peanuts", "Tree Nuts", "Milk", "Eggs", "Seafood", "Shellfish", "Soy", "Wheat", "Insect Stings", "Mold", "Fragrance", "None"};
        etAllergies.setOnClickListener(v -> showSearchableDialog("Select Allergies", allergiesList, selectedAllergies, etAllergies));

        String[] medicationsList = {"Paracetamol", "Ibuprofen", "Aspirin", "Metformin", "Insulin", "Amlodipine", "Losartan", "Atorvastatin", "Levothyroxine", "Salbutamol", "Omeprazole", "Pantoprazole", "Warfarin", "Clopidogrel", "Azithromycin", "Amoxicillin", "Cetirizine", "Montelukast", "Hydrochlorothiazide", "None"};
        etMedications.setOnClickListener(v -> showSearchableDialog("Select Medications", medicationsList, selectedMedications, etMedications));
    }

    private void showSearchableDialog(String title, String[] items, ArrayList<String> selectedList, EditText targetField) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_searchable_list, null);
        builder.setView(view);

        SearchView searchView = view.findViewById(R.id.searchView);
        ListView listView = view.findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, new ArrayList<>(Arrays.asList(items)));
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, v, position, id) -> {
            String item = adapter.getItem(position);
            if (selectedList.contains(item)) selectedList.remove(item);
            else selectedList.add(item);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> targetField.setText(selectedList.toString()));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void checkPhoneAndRegister() {
        String phone = etPhone.getText().toString().trim();
        if (!phone.matches("^\\+91[0-9]{10}$")) {
            Toast.makeText(this, "Enter valid 10-digit phone number with +91", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").whereEqualTo("phone", phone).get().addOnSuccessListener(userDocs -> {
            if (!userDocs.isEmpty()) {
                Toast.makeText(this, "Phone number already exists!", Toast.LENGTH_LONG).show();
            } else {
                db.collection("professionals").whereEqualTo("phone", phone).get().addOnSuccessListener(profDocs -> {
                    if (!profDocs.isEmpty()) {
                        Toast.makeText(this, "Phone number already exists!", Toast.LENGTH_LONG).show();
                    } else {
                        registerUser();
                    }
                });
            }
        });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || passwordLayout.getError() != null) {
            Toast.makeText(this, "Please check all fields and fix errors", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = task.getResult().getUser().getUid();
                saveUserData(userId, password);
            } else {
                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserData(String userId, String password) {
        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("name", etName.getText().toString().trim());
        basicInfo.put("age", etAge.getText().toString().trim());
        basicInfo.put("emergencyContact", etEmergencyContact.getText().toString().trim());

        RadioButton selectedGender = findViewById(rgGender.getCheckedRadioButtonId());
        RadioButton selectedBlood = findViewById(rgBloodGroup.getCheckedRadioButtonId());
        if (selectedGender != null) basicInfo.put("gender", selectedGender.getText().toString());
        if (selectedBlood != null) basicInfo.put("bloodGroup", selectedBlood.getText().toString());

        Map<String, Object> medicalInfo = new HashMap<>();
        medicalInfo.put("allergies", etAllergies.getText().toString().trim());
        medicalInfo.put("diseases", etDiseases.getText().toString().trim());
        medicalInfo.put("medications", etMedications.getText().toString().trim());
        medicalInfo.put("pastSurgeries", etPastSurgeries.getText().toString().trim());

        Map<String, Object> optionalRecords = new HashMap<>();
        optionalRecords.put("labReports", "");
        optionalRecords.put("scanReports", "");
        optionalRecords.put("vaccinationHistory", "");
        optionalRecords.put("familyHistory", "");
        optionalRecords.put("doctorNotes", "");

        Map<String, Object> user = new HashMap<>();
        user.put("email", etEmail.getText().toString().trim());
        user.put("phone", etPhone.getText().toString().trim());
        user.put("address", etAddress.getText().toString().trim());
        user.put("uuid", userId);
        user.put("role", "patient");
        user.put("password", password);
        user.put("basicInfo", basicInfo);
        user.put("medicalInfo", medicalInfo);
        user.put("optionalRecords", optionalRecords);
        user.put("pastHistory", new ArrayList<>());

        db.collection("users").document(userId).set(user).addOnSuccessListener(aVoid -> {
            SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
            prefs.edit().putString("USER_ID", userId).apply();
            Intent intent = new Intent(RegisterActivity.this, QrCodeActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
            finish();
        });
    }
}