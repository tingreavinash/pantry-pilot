package com.pantrypilot.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.pantrypilot.data.local.AppDatabase;
import com.pantrypilot.data.local.GroceryStoreEntity;

import java.util.ArrayList;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        new Thread(() -> {
            try {
                List<GroceryStoreEntity> stores =
                        AppDatabase.getInstance(context).groceryStoreDao().getAllStoresSync();
                if (stores.isEmpty()) return;

                List<Geofence> geofences = new ArrayList<>();
                for (GroceryStoreEntity store : stores) {
                    geofences.add(new Geofence.Builder()
                            .setRequestId(store.name)
                            .setCircularRegion(store.lat, store.lng, store.radiusMeters)
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                            .build());
                }

                GeofencingRequest request = new GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                        .addGeofences(geofences)
                        .build();

                Intent geofenceIntent = new Intent(context, GeofenceBroadcastReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                        geofenceIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

                GeofencingClient client = LocationServices.getGeofencingClient(context);
                client.addGeofences(request, pendingIntent)
                        .addOnSuccessListener(v -> Log.d(TAG, "Geofences re-registered after boot"))
                        .addOnFailureListener(e -> Log.w(TAG, "Failed to re-register geofences", e));

            } catch (Exception e) {
                Log.e(TAG, "BootReceiver error", e);
            }
        }).start();
    }
}
