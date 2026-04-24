package com.pantrypilot.receivers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.pantrypilot.ui.MainActivity;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null || event.hasError()) return;
        if (event.getGeofenceTransition() != Geofence.GEOFENCE_TRANSITION_ENTER) return;

        List<Geofence> triggered = event.getTriggeringGeofences();
        if (triggered == null || triggered.isEmpty()) return;
        String storeName = triggered.get(0).getRequestId();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        AtomicInteger count = new AtomicInteger(0);
        FirebaseFirestore.getInstance()
                .collection("households").document(uid)
                .collection("shoppingList")
                .whereEqualTo("bought", false)
                .get(Source.CACHE)
                .addOnSuccessListener(snap -> {
                    count.set(snap.size());
                    fireNotification(context, storeName, count.get());
                })
                .addOnFailureListener(e -> fireNotification(context, storeName, 0));
    }

    private void fireNotification(Context ctx, String storeName, int itemCount) {
        String body = itemCount > 0
                ? "📍 You're near " + storeName + "! You have " + itemCount + " items on your list."
                : "📍 You're near " + storeName + "! Check your shopping list.";

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setData(android.net.Uri.parse("pantrypilot://tab/Shopping"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 2001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, "geofence_alerts")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Grocery store nearby!")
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi).setAutoCancel(true);

        ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(2001, builder.build());
    }
}
