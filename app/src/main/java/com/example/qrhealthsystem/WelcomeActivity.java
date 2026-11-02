package com.example.qrhealthsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        CardView cardDoctor = findViewById(R.id.cardDoctor);
        CardView cardNurse = findViewById(R.id.cardNurse);
        CardView cardReceptionist = findViewById(R.id.cardReceptionist);
        CardView cardPatient = findViewById(R.id.cardPatient);
        TextView tvLogin = findViewById(R.id.tvLogin);

        cardDoctor.setOnClickListener(v -> {
            startActivity(new Intent(this, DoctorRegisterActivity.class));
        });

        cardNurse.setOnClickListener(v -> {
            startActivity(new Intent(this, NurseRegisterActivity.class));
        });

        cardReceptionist.setOnClickListener(v -> {
            startActivity(new Intent(this, ReceptionRegisterActivity.class));
        });

        cardPatient.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }
}