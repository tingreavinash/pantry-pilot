package com.pantrypilot.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pantrypilot.data.local.AppDatabase;
import com.pantrypilot.data.local.GroceryStoreEntity;
import com.pantrypilot.workers.ShoppingReminderWorker;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

@HiltViewModel
public class SettingsViewModel extends ViewModel {

    private static final String PREFS = "pantrypilot_prefs";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final AppDatabase roomDb;
    public final MutableLiveData<String> householdName = new MutableLiveData<>("");
    public final MutableLiveData<List<GroceryStoreEntity>> stores = new MutableLiveData<>();
    private final ExecutorService executor;
    private final Context appContext;

    @Inject
    public SettingsViewModel(FirebaseAuth auth, FirebaseFirestore db,
                             AppDatabase roomDb, ExecutorService executor,
                             @ApplicationContext Context context) {
        this.auth = auth;
        this.db = db;
        this.roomDb = roomDb;
        this.executor = executor;
        this.appContext = context;

        String uid = uid();
        if (uid != null) {
            db.collection("households").document(uid).get()
                    .addOnSuccessListener(snap -> {
                        String name = snap.getString("householdName");
                        if (name != null) householdName.postValue(name);
                    });
        }

        roomDb.groceryStoreDao().getAllStores()
                .observeForever(list -> stores.postValue(list));
    }

    private String uid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public void saveHouseholdName(String name) {
        String uid = uid();
        if (uid == null) return;
        db.collection("households").document(uid).update("householdName", name);
        householdName.setValue(name);
    }

    public void addStore(GroceryStoreEntity store) {
        executor.execute(() -> roomDb.groceryStoreDao().insert(store));
    }

    public void deleteStore(GroceryStoreEntity store) {
        executor.execute(() -> roomDb.groceryStoreDao().deleteById(store.id));
    }

    public void saveReminderPrefs(int dayOfWeek, int hour) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(ShoppingReminderWorker.KEY_DAY, dayOfWeek)
                .putInt(ShoppingReminderWorker.KEY_HOUR, hour)
                .apply();
        // Reschedule worker with REPLACE policy so new timing takes effect
        WorkManager.getInstance(appContext)
                .enqueueUniquePeriodicWork("shopping_reminder",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        new PeriodicWorkRequest.Builder(
                                ShoppingReminderWorker.class, 7, TimeUnit.DAYS).build());
    }

    public int getSavedDay() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(ShoppingReminderWorker.KEY_DAY, 5); // Saturday default
    }

    public int getSavedHour() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(ShoppingReminderWorker.KEY_HOUR, 9);
    }

    public void signOut(Runnable onDone) {
        WorkManager.getInstance(appContext).cancelAllWork();
        auth.signOut();
        onDone.run();
    }
}
