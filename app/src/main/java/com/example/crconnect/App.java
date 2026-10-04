package com.example.crconnect;

import android.app.Application;
import android.util.Log;
import com.google.firebase.database.FirebaseDatabase;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Enable offline persistence for Firebase Realtime Database
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            // Initialize FCM notification channel
            MyFirebaseMessagingService.createNotificationChannel(this);
        } catch (Exception e) {
            Log.e("App", "Failed to initialize App settings", e);
        }
    }
}
