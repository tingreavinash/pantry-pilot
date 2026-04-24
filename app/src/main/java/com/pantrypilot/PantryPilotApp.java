package com.pantrypilot;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class PantryPilotApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);

        nm.createNotificationChannel(new NotificationChannel(
                "expiry_alerts", "Expiry Alerts",
                NotificationManager.IMPORTANCE_DEFAULT));

        nm.createNotificationChannel(new NotificationChannel(
                "stock_alerts", "Low Stock Alerts",
                NotificationManager.IMPORTANCE_DEFAULT));

        nm.createNotificationChannel(new NotificationChannel(
                "shopping_reminders", "Shopping Reminders",
                NotificationManager.IMPORTANCE_DEFAULT));

        nm.createNotificationChannel(new NotificationChannel(
                "geofence_alerts", "Store Proximity Alerts",
                NotificationManager.IMPORTANCE_HIGH));
    }
}
