package com.pantrypilot.data.model;

import com.google.firebase.Timestamp;

/**
 * Mirrors Firestore: households/{uid}/shoppingList/{docId}
 */
public class ShoppingItem {
    public String id;
    public String name;
    public String category;
    public double quantity;
    public String unit;
    public double estimatedCost;
    public boolean bought;
    public String assignedTo;
    public Timestamp createdAt;

    public ShoppingItem() {
    }
}
