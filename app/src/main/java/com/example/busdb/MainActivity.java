package com.example.busdb;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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
    private EditText etName;
    private Button btnSendLocation, btnStopLocation;
    private Handler handler;
    private Runnable locationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        etName = findViewById(R.id.etName); // EditText para o nome do usuário
        btnSendLocation = findViewById(R.id.btnStartSharing);
        btnStopLocation = findViewById(R.id.btnStopSharing); // Botão para parar
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnSendLocation.setOnClickListener(v -> {
            String userName = etName.getText().toString();
            if (!userName.isEmpty()) {
                if (hasLocationPermission()) {
                    startLocationUpdates(userName);
                } else {
                    requestLocationPermission();
                }
            } else {
                tvStatus.setText("Por favor, insira um nome.");
            }
        });

        btnStopLocation.setOnClickListener(v -> {
            stopLocationUpdates();
        });

        btnStopLocation.setVisibility(Button.GONE); // Inicialmente escondido
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

    private void startLocationUpdates(String userName) {
        tvStatus.setText("Iniciando transmissão de localização...");
        locationRef = FirebaseDatabase.getInstance().getReference("locations/" + userName);

        handler = new Handler();
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                getAndSendLocation(userName);
                handler.postDelayed(this, 5000); // Enviar a cada 5 segundos
            }
        };

        handler.post(locationRunnable);
        btnSendLocation.setVisibility(Button.GONE);
        btnStopLocation.setVisibility(Button.VISIBLE);
        etName.setEnabled(false); // Desabilita o campo de nome
    }

    private void stopLocationUpdates() {
        if (handler != null && locationRunnable != null) {
            handler.removeCallbacks(locationRunnable);
        }

        String userName = etName.getText().toString();
        if (!userName.isEmpty()) {
            FirebaseDatabase.getInstance().getReference("locations/" + userName).removeValue()
                    .addOnCompleteListener(task -> {
                        tvStatus.setText("Localização parada e removida do Firebase.");
                        btnStopLocation.setVisibility(Button.GONE);
                        btnSendLocation.setVisibility(Button.VISIBLE);
                    });
        }
    }

    private void getAndSendLocation(String userName) {
        tvStatus.setText("Tentando obter localização...");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        tvStatus.setText("Localização obtida: " + location.getLatitude() + ", " + location.getLongitude());
                        sendLocationToFirebase(location, userName);
                    } else {
                        tvStatus.setText("Localização é nula. Tente novamente.");
                    }
                })
                .addOnFailureListener(e -> tvStatus.setText("Erro ao obter localização: " + e.getMessage()));
    }

    private void sendLocationToFirebase(Location location, String userName) {
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());

        locationRef.setValue(data)
                .addOnSuccessListener(aVoid -> tvStatus.setText("Localização enviada para " + userName + " a cada 5s"))
                .addOnFailureListener(e -> tvStatus.setText("Erro ao enviar: " + e.getMessage()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            String userName = etName.getText().toString();
            if (!userName.isEmpty()) {
                startLocationUpdates(userName);
            }
        } else {
            tvStatus.setText("Permissão de localização negada.");
        }
    }
}
