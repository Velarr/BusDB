package com.example.busdb;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class LocationForegroundService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private Handler handler;
    private Runnable runnable;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Criar a notificação se não foi criada anteriormente
        createNotificationChannel();

        // Criando a notificação para o serviço em primeiro plano
        Notification notification = buildNotification();

        // Iniciando o serviço em primeiro plano com a notificação
        startForeground(1, notification); // A notificação precisa ser fornecida imediatamente aqui

        // Retornar para continuar o serviço em segundo plano
        return START_STICKY;
    }

    private void startLocationUpdates(String name) {
        runnable = new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(location -> {
                            if (location != null) {
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("locations").child(name);
                                Map<String, Object> data = new HashMap<>();
                                data.put("latitude", location.getLatitude());
                                data.put("longitude", location.getLongitude());
                                ref.setValue(data);
                            }
                        });
                handler.postDelayed(this, 5000); // A cada 5 segundos
            }
        };
        handler.post(runnable);
    }

    private Notification buildNotification() {
        // Criando o PendingIntent que será chamado ao clicar na notificação
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE
        );

        // Criando a notificação
        return new NotificationCompat.Builder(this, "default")
                .setContentTitle("Localização em Trânsito")
                .setContentText("Transmitindo sua localização.")
                .setSmallIcon(R.drawable.ic_launcher_background) // Substitua com um ícone válido
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Location Service Channel";
            String description = "Channel for location service notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("default", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }




    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Envia o broadcast para parar o serviço
        Intent intent = new Intent("STOP_SERVICE");
        sendBroadcast(intent);
    }

}
