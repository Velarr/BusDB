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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class LocationForegroundService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private Handler handler;
    private Runnable runnable;

    // Chamado quando o serviço é criado; inicializa o cliente de localização e o handler
    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler();
    }

    // Chamado quando o serviço é iniciado; cria o canal e a notificação para rodar em foreground
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = buildNotification();
        startForeground(1, notification);  // Inicia o serviço em foreground com a notificação
        return START_STICKY;  // Mantém o serviço ativo mesmo se o sistema matar ele
    }

    // Cria a notificação exibida na barra para o serviço foreground
    private Notification buildNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, "default")
                .setContentTitle("Localização em Trânsito")
                .setContentText("Transmitindo sua localização.")
                .setSmallIcon(R.drawable.ic_launcher_background)  // Ícone da notificação
                .setContentIntent(pendingIntent)  // Abre MainActivity ao clicar na notificação
                .build();
    }

    // Cria o canal de notificação
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

    // Não suporta binding, então retorna null
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Quando o serviço é destruído, envia um broadcast para informar que parou
    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent("STOP_SERVICE");
        sendBroadcast(intent);
    }
}

