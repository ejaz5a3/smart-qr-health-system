package com.example.qrhealthsystem;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EmergencyAccessibilityService extends AccessibilityService {

    private final ArrayList<String> keyPattern = new ArrayList<>();
    private long lastPressTime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();

        if (action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                long now = System.currentTimeMillis();
                if (now - lastPressTime > 3000) {
                    keyPattern.clear();
                }
                lastPressTime = now;

                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    keyPattern.add("UP");
                } else {
                    keyPattern.add("DOWN");
                }

                if (keyPattern.size() >= 4) {
                    int size = keyPattern.size();
                    if (keyPattern.get(size - 4).equals("UP") &&
                        keyPattern.get(size - 3).equals("UP") &&
                        keyPattern.get(size - 2).equals("DOWN") &&
                        keyPattern.get(size - 1).equals("DOWN")) {
                        
                        handler.post(this::triggerEmergency);
                        keyPattern.clear();
                    }
                }
            }
        }
        return super.onKeyEvent(event);
    }

    private void triggerEmergency() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : "locked_device";

                Map<String, Object> data = new HashMap<>();
                data.put("latitude", location.getLatitude());
                data.put("longitude", location.getLongitude());
                data.put("time", System.currentTimeMillis());
                data.put("patientId", uid);
                data.put("type", "HARDWARE_TRIGGER");

                db.collection("emergencies").add(data);
                Toast.makeText(this, "🚨 EMERGENCY ALERT SENT!", Toast.LENGTH_LONG).show();
                callAmbulance();
            }
        });
    }

    private void callAmbulance() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:108"));
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialIntent);
        }
    }
}