package com.example.busdb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StopServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("STOP_SERVICE".equals(intent.getAction())) {
            context.stopService(new Intent(context, LocationForegroundService.class));
        }
    }
}