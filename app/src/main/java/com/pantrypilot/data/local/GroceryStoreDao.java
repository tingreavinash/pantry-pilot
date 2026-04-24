package com.pantrypilot.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GroceryStoreDao {

    @Query("SELECT * FROM grocery_stores")
    LiveData<List<GroceryStoreEntity>> getAllStores();

    @Query("SELECT * FROM grocery_stores")
    List<GroceryStoreEntity> getAllStoresSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GroceryStoreEntity store);

    @Delete
    void delete(GroceryStoreEntity store);

    @Query("DELETE FROM grocery_stores WHERE id = :storeId")
    void deleteById(long storeId);
}
