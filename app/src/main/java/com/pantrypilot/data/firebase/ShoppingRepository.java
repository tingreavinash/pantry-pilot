package com.pantrypilot.data.firebase;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.data.model.ShoppingItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShoppingRepository {

    private final FirebaseFirestore db;
    private ListenerRegistration shoppingListener;

    @Inject
    public ShoppingRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public void subscribeShoppingItems(String uid, MutableLiveData<List<ShoppingItem>> liveData) {
        if (shoppingListener != null) shoppingListener.remove();
        shoppingListener = db.collection("households").document(uid)
                .collection("shoppingList")
                .orderBy("category")
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null) {
                        List<ShoppingItem> items = snapshot.toObjects(ShoppingItem.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            items.get(i).id = snapshot.getDocuments().get(i).getId();
                        }
                        liveData.postValue(items);
                    }
                });
    }

    public void removeListener() {
        if (shoppingListener != null) {
            shoppingListener.remove();
            shoppingListener = null;
        }
    }

    public void addItem(String uid, ShoppingItem item) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", item.name);
        data.put("category", item.category);
        data.put("quantity", item.quantity);
        data.put("unit", item.unit);
        data.put("estimatedCost", item.estimatedCost);
        data.put("bought", false);
        data.put("assignedTo", item.assignedTo);
        data.put("createdAt", FieldValue.serverTimestamp());
        db.collection("households").document(uid).collection("shoppingList").add(data);
    }

    public void setBought(String uid, String itemId, boolean bought) {
        db.collection("households").document(uid)
                .collection("shoppingList").document(itemId)
                .update("bought", bought);
    }

    public void assignMember(String uid, String itemId, String memberName) {
        db.collection("households").document(uid)
                .collection("shoppingList").document(itemId)
                .update("assignedTo", memberName);
    }

    public void deleteItem(String uid, String itemId) {
        db.collection("households").document(uid)
                .collection("shoppingList").document(itemId).delete();
    }

    /**
     * Clear all bought items in a single batch
     */
    public void clearBoughtItems(String uid, List<ShoppingItem> allItems) {
        WriteBatch batch = db.batch();
        for (ShoppingItem item : allItems) {
            if (item.bought) {
                batch.delete(db.collection("households").document(uid)
                        .collection("shoppingList").document(item.id));
            }
        }
        batch.commit();
    }

    /**
     * Auto-populate: add pantry items below threshold that aren't already
     * in the shopping list (name-based deduplication, case-insensitive).
     */
    public void autoPopulate(String uid, List<PantryItem> pantryItems,
                             List<ShoppingItem> existingShoppingItems) {
        List<String> existingNames = new ArrayList<>();
        for (ShoppingItem s : existingShoppingItems) {
            existingNames.add(s.name.toLowerCase().trim());
        }
        WriteBatch batch = db.batch();
        boolean hasNewItems = false;
        CollectionReference col = db.collection("households").document(uid)
                .collection("shoppingList");
        for (PantryItem p : pantryItems) {
            if (p.getStockStatus() != PantryItem.StockStatus.OK
                    && !existingNames.contains(p.name.toLowerCase().trim())) {
                Map<String, Object> data = new HashMap<>();
                data.put("name", p.name);
                data.put("category", p.category);
                data.put("quantity", 1.0);
                data.put("unit", p.unit);
                data.put("estimatedCost", 0.0);
                data.put("bought", false);
                data.put("assignedTo", "");
                data.put("createdAt", FieldValue.serverTimestamp());
                batch.set(col.document(), data);
                hasNewItems = true;
            }
        }
        if (hasNewItems) batch.commit();
    }
}
