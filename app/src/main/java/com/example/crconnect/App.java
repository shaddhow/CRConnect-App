package com.example.crconnect;

import android.app.Application;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.database.FirebaseDatabase;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

            // Enable offline persistence for Firebase Realtime Database
            try {
                FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").setPersistenceEnabled(true);
            } catch (Exception e) {
                Log.w("App", "Firebase Database persistence initialization notice: " + e.getMessage());
            }
            // Initialize FCM notification channel
            MyFirebaseMessagingService.createNotificationChannel(this);
        } catch (Exception e) {
            Log.e("App", "Failed to initialize App settings", e);
        }
    }
}
