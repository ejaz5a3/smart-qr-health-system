package com.example.qrhealthsystem;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EmergencyForegroundService extends Service {

    private static final String CHANNEL_ID = "EmergencyServiceChannel";
    private VolumeReceiver volumeReceiver;
    private final ArrayList<String> pattern = new ArrayList<>();
    private long lastPatternTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Emergency Protection Active")
                .setContentText("Volume pattern (UP-UP-DOWN-DOWN) is active")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();

        startForeground(1, notification);

        volumeReceiver = new VolumeReceiver();
        IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(volumeReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (volumeReceiver != null) {
            unregisterReceiver(volumeReceiver);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Emergency Protection",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private class VolumeReceiver extends BroadcastReceiver {
        private int lastVolume = -1;

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                int newVolume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1);
                long currentTime = System.currentTimeMillis();

                if (lastVolume != -1) {
                    if (newVolume > lastVolume) {
                        handleVolumePress(true, currentTime);
                    } else if (newVolume < lastVolume) {
                        handleVolumePress(false, currentTime);
                    }
                }
                lastVolume = newVolume;
            }
        }
    }

    private void handleVolumePress(boolean isUp, long time) {
        if (time - lastPatternTime > 3000) {
            pattern.clear();
        }
        lastPatternTime = time;

        pattern.add(isUp ? "UP" : "DOWN");

        if (pattern.size() >= 4) {
            int size = pattern.size();
            if (pattern.get(size - 4).equals("UP") &&
                pattern.get(size - 3).equals("UP") &&
                pattern.get(size - 2).equals("DOWN") &&
                pattern.get(size - 1).equals("DOWN")) {
                sendEmergency();
                pattern.clear();
            }
        }
    }

    private void sendEmergency() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : "unknown_user";

                Map<String, Object> data = new HashMap<>();
                data.put("latitude", location.getLatitude());
                data.put("longitude", location.getLongitude());
                data.put("time", System.currentTimeMillis());
                data.put("patientId", uid);
                data.put("type", "VOLUME_PATTERN_TRIGGER");

                db.collection("emergencies").add(data);
                
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:108"));
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(callIntent);
                }
                Toast.makeText(this, "🚨 VOLUME PATTERN DETECTED: EMERGENCY SENT!", Toast.LENGTH_LONG).show();
            }
        });
    }
}