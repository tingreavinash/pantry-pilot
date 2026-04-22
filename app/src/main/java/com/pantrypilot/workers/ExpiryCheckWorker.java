package com.pantrypilot.workers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.pantrypilot.MainActivity;
import com.pantrypilot.data.model.PantryItem;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ExpiryCheckWorker extends Worker {

    public ExpiryCheckWorker(Context ctx, WorkerParameters params) {
        super(ctx, params);
    }

    @Override
    public Result doWork() {
        String uid = currentUid();
        if (uid == null) return Result.success();
        try {
            CountDownLatch latch = new CountDownLatch(1);
            StringBuilder names = new StringBuilder();
            int[] count = {0};

            FirebaseFirestore.getInstance()
                    .collection("households").document(uid)
                    .collection("pantryItems")
                    .get(Source.CACHE)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (PantryItem item : task.getResult().toObjects(PantryItem.class)) {
                                long days = item.daysUntilExpiry();
                                if (days >= 0 && days <= 2) {
                                    if (count[0] > 0) names.append(", ");
                                    names.append(item.name);
                                    count[0]++;
                                }
                            }
                        }
                        latch.countDown();
                    });

            latch.await(10, TimeUnit.SECONDS);

            if (count[0] > 0) {
                String body = count[0] == 1
                        ? names + " expires soon! Check your pantry."
                        : count[0] + " items expiring soon: " + names;
                notify(1001, "expiry_alerts", "Items expiring soon 🥛", body, "Pantry");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Result.success();
    }

    private String currentUid() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    private void notify(int id, String channel, String title, String body, String tab) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setData(android.net.Uri.parse("pantrypilot://tab/" + tab));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext(), channel)
                        .setSmallIcon(android.R.drawable.ic_popup_reminder)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pi)
                        .setAutoCancel(true);

        NotificationManager nm = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, builder.build());
    }
}
