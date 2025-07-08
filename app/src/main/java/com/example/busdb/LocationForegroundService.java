package com.example.busdb;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocationForegroundService extends Service {
    private static final String CHANNEL_ID = "location_channel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference databaseReference;
    private String driverName, routeId, routeName, routeNumber, routeColor;
    private long startTimeMillis;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        startTimeMillis = System.currentTimeMillis(); // Salva o horário de início
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        driverName = intent.getStringExtra("driverName");
        routeId = intent.getStringExtra("routeId");
        routeName = intent.getStringExtra("routeName");
        routeNumber = intent.getStringExtra("routeNumber");
        routeColor = intent.getStringExtra("routeColor");

        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MBus - A enviar localização")
                .setContentText("A localização está a ser partilhada")
                .build();

        startForeground(1, notification);
        startLocationUpdates();

        return START_NOT_STICKY;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000); // 5 segundos
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    sendLocationToFirebase(location);
                    sendSpeedBroadcast(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void sendLocationToFirebase(Location location) {
        String elapsedTime = getElapsedTimeString();
        Log.d("DEBUG", "Enviando tempo: " + elapsedTime); // ADICIONADO

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("driverName", driverName);
        locationData.put("routeId", routeId);
        locationData.put("routeName", routeName);
        locationData.put("routeNumber", routeNumber);
        locationData.put("color", routeColor);
        locationData.put("tempo", elapsedTime); // Adicionado o tempo

        databaseReference.child("locations").child(routeName).setValue(locationData);
    }


    private void sendSpeedBroadcast(Location location) {
        float speed = location.hasSpeed() ? location.getSpeed() : 0f;

        Intent intent = new Intent("LOCATION_UPDATE");
        intent.putExtra("speed", speed);
        sendBroadcast(intent);
    }

    private String getElapsedTimeString() {
        long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        long seconds = (elapsedMillis / 1000) % 60;
        long minutes = (elapsedMillis / (1000 * 60)) % 60;
        long hours = (elapsedMillis / (1000 * 60 * 60));

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Canal de Localização",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Intent stopIntent = new Intent("STOP_SERVICE");
        sendBroadcast(stopIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
