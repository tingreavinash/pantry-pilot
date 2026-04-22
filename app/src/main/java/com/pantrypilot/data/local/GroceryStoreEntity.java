package com.pantrypilot.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "grocery_stores")
public class GroceryStoreEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public double lat;
    public double lng;
    public float radiusMeters = 200f;

    public GroceryStoreEntity() {
    }
}
