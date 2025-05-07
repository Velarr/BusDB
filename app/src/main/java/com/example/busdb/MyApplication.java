package com.example.busdb;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class MyApplication extends Application {

    private static boolean isAppInBackground = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Registrar o ciclo de vida da Activity
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                // Código para quando a Activity é criada
            }

            @Override
            public void onActivityStarted(Activity activity) {
                // Código para quando a Activity é iniciada
            }

            @Override
            public void onActivityResumed(Activity activity) {
                // Código para quando a Activity é retomada
            }

            @Override
            public void onActivityPaused(Activity activity) {
                // Código para quando a Activity é pausada
            }

            @Override
            public void onActivityStopped(Activity activity) {
                // Código para quando a Activity é parada
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                // Código para salvar o estado da Activity
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                // Código para quando a Activity for destruída
            }
        });
    }

    public static boolean isAppInBackground() {
        return isAppInBackground;
    }
}

