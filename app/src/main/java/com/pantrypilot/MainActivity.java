package com.pantrypilot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.pantrypilot.ui.common.Navigation;
import com.pantrypilot.workers.ExpiryCheckWorker;
import com.pantrypilot.workers.LowStockWorker;
import com.pantrypilot.workers.ShoppingReminderWorker;

import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends ComponentActivity {

    private ActivityResultLauncher<String> notificationPermLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupNotificationPermission();
        scheduleWorkers();

        setContent(() -> Navigation.AppNavigation());
    }

    private void setupNotificationPermission() {
        notificationPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { /* notification permission result handled silently */ }
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void scheduleWorkers() {
        WorkManager wm = WorkManager.getInstance(getApplicationContext());

        PeriodicWorkRequest expiryWork = new PeriodicWorkRequest.Builder(
                ExpiryCheckWorker.class, 24, TimeUnit.HOURS)
                .build();
        wm.enqueueUniquePeriodicWork(
                "expiry_check", ExistingPeriodicWorkPolicy.KEEP, expiryWork);

        PeriodicWorkRequest stockWork = new PeriodicWorkRequest.Builder(
                LowStockWorker.class, 12, TimeUnit.HOURS)
                .build();
        wm.enqueueUniquePeriodicWork(
                "low_stock_check", ExistingPeriodicWorkPolicy.KEEP, stockWork);

        PeriodicWorkRequest shoppingWork = new PeriodicWorkRequest.Builder(
                ShoppingReminderWorker.class, 7, TimeUnit.DAYS)
                .build();
        wm.enqueueUniquePeriodicWork(
                "shopping_reminder", ExistingPeriodicWorkPolicy.KEEP, shoppingWork);
    }
}
