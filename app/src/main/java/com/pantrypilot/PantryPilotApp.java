package com.pantrypilot;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class PantryPilotApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        configureFirestore();
        createNotificationChannels();
    }

    private void configureFirestore() {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);
    }

    private void createNotificationChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);

        NotificationChannel expiryChannel = new NotificationChannel(
                "expiry_alerts",
                "Expiry Alerts",
                NotificationManager.IMPORTANCE_DEFAULT);
        expiryChannel.setDescription("Alerts when items are about to expire");

        NotificationChannel stockChannel = new NotificationChannel(
                "stock_alerts",
                "Low Stock Alerts",
                NotificationManager.IMPORTANCE_DEFAULT);
        stockChannel.setDescription("Alerts when items fall below minimum threshold");

        NotificationChannel shoppingChannel = new NotificationChannel(
                "shopping_reminders",
                "Shopping Reminders",
                NotificationManager.IMPORTANCE_DEFAULT);
        shoppingChannel.setDescription("Weekly shopping day reminders");

        NotificationChannel geofenceChannel = new NotificationChannel(
                "geofence_alerts",
                "Store Proximity Alerts",
                NotificationManager.IMPORTANCE_HIGH);
        geofenceChannel.setDescription("Alerts when near a saved grocery store");

        nm.createNotificationChannel(expiryChannel);
        nm.createNotificationChannel(stockChannel);
        nm.createNotificationChannel(shoppingChannel);
        nm.createNotificationChannel(geofenceChannel);
    }
}
