package com.example.busdb;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class MyApplication extends Application { //Atividade em Segundo plano

    private static boolean isAppInBackground = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Registrar o ciclo de vida da Activity
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    public static boolean isAppInBackground() {
        return isAppInBackground;
    }
}

