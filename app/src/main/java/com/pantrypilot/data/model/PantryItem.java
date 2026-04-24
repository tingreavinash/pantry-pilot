package com.pantrypilot.data.model;

import com.google.firebase.Timestamp;

public class PantryItem {
    public String id;
    public String name;
    public String category;
    public double quantity;
    public String unit;
    public double minThreshold;
    public Timestamp expiryDate;
    public Timestamp createdAt;

    public PantryItem() {
    }

    public enum StockStatus {OK, LOW, OUT}

    public StockStatus getStockStatus() {
        if (quantity <= 0) return StockStatus.OUT;
        if (quantity < minThreshold) return StockStatus.LOW;
        return StockStatus.OK;
    }

    public long daysUntilExpiry() {
        if (expiryDate == null) return -1;
        long now = System.currentTimeMillis();
        long exp = expiryDate.toDate().getTime();
        return (exp - now) / (1000L * 60 * 60 * 24);
    }
}
