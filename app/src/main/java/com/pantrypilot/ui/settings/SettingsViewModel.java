package com.pantrypilot.ui.settings;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pantrypilot.data.local.AppDatabase;
import com.pantrypilot.data.local.GroceryStoreEntity;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends ViewModel {

    public final MutableLiveData<String> householdName = new MutableLiveData<>("");
    public final MutableLiveData<List<GroceryStoreEntity>> stores = new MutableLiveData<>();
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final AppDatabase roomDb;

    @Inject
    public SettingsViewModel(FirebaseAuth auth, FirebaseFirestore db, AppDatabase roomDb) {
        this.auth = auth;
        this.db = db;
        this.roomDb = roomDb;

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (!uid.isEmpty()) {
            db.collection("households").document(uid).get()
                    .addOnSuccessListener(snap -> {
                        String name = snap.getString("householdName");
                        if (name != null) householdName.postValue(name);
                    });
        }
        roomDb.groceryStoreDao().getAllStores()
                .observeForever(s -> stores.postValue(s));
    }

    public void saveHouseholdName(String name) {
        String uid = auth.getCurrentUser().getUid();
        db.collection("households").document(uid).update("householdName", name);
        householdName.setValue(name);
    }

    public void addStore(GroceryStoreEntity store) {
        new Thread(() -> roomDb.groceryStoreDao().insert(store)).start();
    }

    public void deleteStore(GroceryStoreEntity store) {
        new Thread(() -> roomDb.groceryStoreDao().deleteById(store.id)).start();
    }

    public void signOut(Runnable onDone) {
        auth.signOut();
        onDone.run();
    }
}
