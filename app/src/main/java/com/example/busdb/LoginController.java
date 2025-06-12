package com.example.busdb;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginController {

    private final FirebaseAuth auth;

    public LoginController() {
        this.auth = FirebaseAuth.getInstance();
    }

    public boolean isUserLoggedIn() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null;
    }

    public void redirectIfNotLoggedIn(Context context, Class<?> redirectActivity) {
        if (!isUserLoggedIn()) {
            Intent intent = new Intent(context, redirectActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public void logout(Context context, Class<?> redirectActivity) {
        auth.signOut();
        Intent intent = new Intent(context, redirectActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
