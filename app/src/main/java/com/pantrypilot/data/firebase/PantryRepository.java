package com.pantrypilot.data.firebase;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;
import com.pantrypilot.data.model.PantryItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PantryRepository {

    private final FirebaseFirestore db;
    private ListenerRegistration pantryListener;

    @Inject
    public PantryRepository(FirebaseFirestore db) {
        this.db = db;
    }

    // ── Subscribe to real-time updates ────────────────────────────────────────
    public void subscribePantryItems(String uid, MutableLiveData<List<PantryItem>> liveData) {
        removeListener();
        pantryListener = db.collection("households").document(uid)
                .collection("pantryItems")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null) {
                        List<PantryItem> items = snapshot.toObjects(PantryItem.class);
                        // Inject document ID into each item
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            items.get(i).id = snapshot.getDocuments().get(i).getId();
                        }
                        liveData.postValue(items);
                    }
                });
    }

    public void removeListener() {
        if (pantryListener != null) {
            pantryListener.remove();
            pantryListener = null;
        }
    }

    // ── CRUD operations ───────────────────────────────────────────────────────
    public void addItem(String uid, PantryItem item, Runnable onSuccess, Runnable onError) {
        Map<String, Object> data = itemToMap(item);
        data.put("createdAt", FieldValue.serverTimestamp());
        db.collection("households").document(uid)
                .collection("pantryItems")
                .add(data)
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(e -> onError.run());
    }

    public void updateItem(String uid, PantryItem item, Runnable onSuccess, Runnable onError) {
        db.collection("households").document(uid)
                .collection("pantryItems").document(item.id)
                .update(itemToMap(item))
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.run());
    }

    public void deleteItem(String uid, String itemId, Runnable onSuccess) {
        db.collection("households").document(uid)
                .collection("pantryItems").document(itemId)
                .delete()
                .addOnSuccessListener(v -> onSuccess.run());
    }

    private Map<String, Object> itemToMap(PantryItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", item.name);
        map.put("category", item.category);
        map.put("quantity", item.quantity);
        map.put("unit", item.unit);
        map.put("minThreshold", item.minThreshold);
        if (item.expiryDate != null) map.put("expiryDate", item.expiryDate);
        return map;
    }

    /**
     * One-time fetch for WorkManager (offline cache backed)
     */
    public void fetchPantryItemsOnce(String uid, MutableLiveData<List<PantryItem>> liveData) {
        db.collection("households").document(uid)
                .collection("pantryItems")
                .get(Source.CACHE)
                .addOnSuccessListener(snapshot -> {
                    List<PantryItem> items = snapshot.toObjects(PantryItem.class);
                    for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                        items.get(i).id = snapshot.getDocuments().get(i).getId();
                    }
                    liveData.postValue(items);
                });
    }
}
