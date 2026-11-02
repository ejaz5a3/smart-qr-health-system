package com.example.qrhealthsystem;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import androidx.activity.OnBackPressedCallback;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QrCodeActivity extends AppCompatActivity {

    ImageView imgQR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);

        imgQR = findViewById(R.id.imgQR);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                Intent intent = new Intent(QrCodeActivity.this, MainActivity.class);
                startActivity(intent);
                //finish();

            }
        });

        String userId = getIntent().getStringExtra("USER_ID");

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(
                    userId,
                    BarcodeFormat.QR_CODE,
                    500,
                    500
            );
            imgQR.setImageBitmap(bitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

