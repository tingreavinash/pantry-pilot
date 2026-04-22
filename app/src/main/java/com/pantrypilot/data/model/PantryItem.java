package com.pantrypilot.data.model;

import com.google.firebase.Timestamp;

/**
 * Mirrors Firestore: households/{uid}/pantryItems/{docId}
 * Field names match the existing web app schema exactly.
 */
public class PantryItem {
    public String id;          // injected from DocumentSnapshot.getId()
    public String name;
    public String category;
    public double quantity;
    public String unit;
    public double minThreshold;
    public Timestamp expiryDate;
    public Timestamp createdAt;

    public PantryItem() {
    }

    public StockStatus getStockStatus() {
        if (quantity <= 0) return StockStatus.OUT;
        if (quantity < minThreshold) return StockStatus.LOW;
        return StockStatus.OK;
    }

    /**
     * Returns days until expiry, or -1 if no expiry date set.
     */
    public long daysUntilExpiry() {
        if (expiryDate == null) return -1;
        long now = System.currentTimeMillis();
        long exp = expiryDate.toDate().getTime();
        return (exp - now) / (1000L * 60 * 60 * 24);
    }

    public enum StockStatus {OK, LOW, OUT}
}
