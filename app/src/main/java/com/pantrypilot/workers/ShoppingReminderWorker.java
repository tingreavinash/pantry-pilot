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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ShoppingReminderWorker extends Worker {

    public static final String PREFS = "pantrypilot_prefs";
    public static final String KEY_DAY = "reminder_day";
    public static final String KEY_HOUR = "reminder_hour";
    public static final String KEY_MINUTE = "reminder_minute";

    public ShoppingReminderWorker(Context ctx, WorkerParameters params) {
        super(ctx, params);
    }

    @Override
    public Result doWork() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return Result.success();
        String uid = auth.getCurrentUser().getUid();

        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger count = new AtomicInteger(0);

            FirebaseFirestore.getInstance()
                    .collection("households").document(uid)
                    .collection("shoppingList")
                    .whereEqualTo("bought", false)
                    .get(Source.CACHE)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) count.set(task.getResult().size());
                        latch.countDown();
                    });

            latch.await(10, TimeUnit.SECONDS);

            String body = count.get() > 0
                    ? "You have " + count.get() + " items on your list. Happy shopping! 🛒"
                    : "Time for your weekly grocery run!";

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setData(android.net.Uri.parse("pantrypilot://tab/Shopping"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 1003, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(getApplicationContext(), "shopping_reminders")
                            .setSmallIcon(android.R.drawable.ic_popup_reminder)
                            .setContentTitle("Shopping day! 🛒")
                            .setContentText(body)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pi)
                            .setAutoCancel(true);

            ((NotificationManager) getApplicationContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE))
                    .notify(1003, builder.build());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Result.success();
    }
}
