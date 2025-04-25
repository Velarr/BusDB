package com.example.busdb;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference locationRef;
    private TextView tvStatus;
    private Button btnSendLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        btnSendLocation = findViewById(R.id.btnSendLocation);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRef = FirebaseDatabase.getInstance().getReference("locations/secondaryApp");

        btnSendLocation.setOnClickListener(v -> {
            if (hasLocationPermission()) {
                getAndSendLocation();
            } else {
                requestLocationPermission();
            }
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST);
    }

    private void getAndSendLocation() {
        tvStatus.setText("Tentando obter localização...");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        tvStatus.setText("Localização obtida: " + location.getLatitude() + ", " + location.getLongitude());
                        sendLocationToFirebase(location);
                    } else {
                        tvStatus.setText("Localização é nula. Tente novamente.");
                    }
                })
                .addOnFailureListener(e -> tvStatus.setText("Erro ao obter localização: " + e.getMessage()));
    }


    private void sendLocationToFirebase(Location location) {
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());

        locationRef.setValue(data)
                .addOnSuccessListener(aVoid -> tvStatus.setText("Localização enviada!"))
                .addOnFailureListener(e -> tvStatus.setText("Erro ao enviar: " + e.getMessage()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getAndSendLocation();
        } else {
            tvStatus.setText("Permissão de localização negada.");
        }
    }
}
