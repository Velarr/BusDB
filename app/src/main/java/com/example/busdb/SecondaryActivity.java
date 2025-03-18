package com.example.busdb;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class SecondaryActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference locationRef;
    private TextView tvStatus;
    private Button btnSendLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        // Inicializa os componentes do layout
        tvStatus = findViewById(R.id.tvStatus);
        btnSendLocation = findViewById(R.id.btnSendLocation);

        // Inicializar cliente de localização e Firebase
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        locationRef = database.getReference("locations/secondaryApp");

        // Verifica e solicita permissão
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enviarLocalizacao();
        }

        // Quando o botão for pressionado, enviar localização
        btnSendLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarLocalizacao();
            }
        });
    }

    // Método para enviar localização
    private void enviarLocalizacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                Map<String, Double> locationData = new HashMap<>();
                                locationData.put("latitude", location.getLatitude());
                                locationData.put("longitude", location.getLongitude());

                                locationRef.setValue(locationData);
                                tvStatus.setText("Localização enviada: " + location.getLatitude() + ", " + location.getLongitude());
                            } else {
                                tvStatus.setText("Erro ao obter localização.");
                            }
                        }
                    });
        } else {
            tvStatus.setText("Permissão de localização não concedida.");
        }
    }

    // Resultado da solicitação de permissão
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enviarLocalizacao();
            } else {
                tvStatus.setText("Permissão negada. Ative manualmente nas configurações.");
            }
        }
    }
}
