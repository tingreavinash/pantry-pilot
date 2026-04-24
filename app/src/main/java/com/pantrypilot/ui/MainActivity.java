package com.pantrypilot.ui;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.pantrypilot.R;
import com.pantrypilot.databinding.ActivityMainBinding;
import com.pantrypilot.ui.auth.AuthActivity;
import com.pantrypilot.ui.auth.AuthViewModel;
import com.pantrypilot.workers.ExpiryCheckWorker;
import com.pantrypilot.workers.LowStockWorker;
import com.pantrypilot.workers.ShoppingReminderWorker;

import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Nav setup
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHost.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        // Handle deep links from notifications
        handleDeepLink(getIntent());

        // Observe auth — if signed out, go back to login
        AuthViewModel authVM = new ViewModelProvider(this).get(AuthViewModel.class);
        authVM.currentUser.observe(this, user -> {
            if (user == null) {
                startActivity(new Intent(this, AuthActivity.class));
                finish();
            }
        });

        // Offline banner
        observeConnectivity();

        // Schedule background workers
        scheduleWorkers();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        String path = intent.getData().getLastPathSegment();
        if (path == null) return;
        switch (path) {
            case "Dashboard":
                navController.navigate(R.id.nav_dashboard);
                break;
            case "Pantry":
                navController.navigate(R.id.nav_pantry);
                break;
            case "Shopping":
                navController.navigate(R.id.nav_shopping);
                break;
            case "Meals":
                navController.navigate(R.id.nav_meals);
                break;
            case "Members":
                navController.navigate(R.id.nav_members);
                break;
        }
    }

    private void observeConnectivity() {
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        NetworkRequest req = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        cm.registerNetworkCallback(req, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> binding.tvOfflineBanner.setVisibility(View.GONE));
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> binding.tvOfflineBanner.setVisibility(View.VISIBLE));
            }
        });
    }

    private void scheduleWorkers() {
        WorkManager wm = WorkManager.getInstance(getApplicationContext());
        wm.enqueueUniquePeriodicWork("expiry_check",
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(ExpiryCheckWorker.class, 24, TimeUnit.HOURS).build());
        wm.enqueueUniquePeriodicWork("low_stock_check",
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(LowStockWorker.class, 12, TimeUnit.HOURS).build());
        wm.enqueueUniquePeriodicWork("shopping_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(ShoppingReminderWorker.class, 7, TimeUnit.DAYS).build());
    }
}
