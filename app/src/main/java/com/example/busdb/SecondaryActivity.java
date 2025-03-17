package com.example.busdb;
import android.location.Location;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SecondaryActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference locationRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        // Inicializar o cliente de localização e o Firebase
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        locationRef = database.getReference("locations");

        // Obter a última localização conhecida
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Criar um mapa com latitude e longitude
                            Map<String, Double> locationData = new HashMap<>();
                            locationData.put("latitude", location.getLatitude());
                            locationData.put("longitude", location.getLongitude());

                            // Enviar para o Firebase
                            locationRef.child("secondaryApp").setValue(locationData);
                        }
                    }
                });
    }
}
