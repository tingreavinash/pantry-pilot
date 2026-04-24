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
    private ListenerRegistration listener;

    @Inject
    public PantryRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public void subscribe(String uid, MutableLiveData<List<PantryItem>> liveData) {
        removeListener();
        listener = db.collection("households").document(uid)
                .collection("pantryItems")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null) {
                        List<PantryItem> items = snapshot.toObjects(PantryItem.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            items.get(i).id = snapshot.getDocuments().get(i).getId();
                        }
                        liveData.postValue(items);
                    }
                });
    }

    public void removeListener() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    public void addItem(String uid, PantryItem item, Runnable onSuccess, Runnable onError) {
        Map<String, Object> data = toMap(item);
        data.put("createdAt", FieldValue.serverTimestamp());
        db.collection("households").document(uid).collection("pantryItems")
                .add(data)
                .addOnSuccessListener(r -> onSuccess.run())
                .addOnFailureListener(e -> onError.run());
    }

    public void updateItem(String uid, PantryItem item, Runnable onSuccess, Runnable onError) {
        db.collection("households").document(uid).collection("pantryItems")
                .document(item.id).update(toMap(item))
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.run());
    }

    public void deleteItem(String uid, String itemId, Runnable onSuccess) {
        db.collection("households").document(uid).collection("pantryItems")
                .document(itemId).delete().addOnSuccessListener(v -> onSuccess.run());
    }

    /**
     * Cache-first fetch for workers
     */
    public void fetchOnce(String uid, MutableLiveData<List<PantryItem>> liveData) {
        db.collection("households").document(uid).collection("pantryItems")
                .get(Source.CACHE)
                .addOnSuccessListener(snapshot -> {
                    List<PantryItem> items = snapshot.toObjects(PantryItem.class);
                    for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                        items.get(i).id = snapshot.getDocuments().get(i).getId();
                    }
                    liveData.postValue(items);
                });
    }

    private Map<String, Object> toMap(PantryItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", item.name);
        map.put("category", item.category);
        map.put("quantity", item.quantity);
        map.put("unit", item.unit);
        map.put("minThreshold", item.minThreshold);
        if (item.expiryDate != null) map.put("expiryDate", item.expiryDate);
        return map;
    }
}
