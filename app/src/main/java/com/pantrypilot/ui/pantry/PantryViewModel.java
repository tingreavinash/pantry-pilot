package com.pantrypilot.ui.pantry;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pantrypilot.data.firebase.PantryRepository;
import com.pantrypilot.data.model.PantryItem;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PantryViewModel extends ViewModel {

    public final MutableLiveData<List<PantryItem>> pantryItems = new MutableLiveData<>();
    public final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    public final MutableLiveData<String> selectedCategory = new MutableLiveData<>("All");
    public final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final PantryRepository pantryRepo;
    private String currentUid;

    @Inject
    public PantryViewModel(PantryRepository pantryRepo) {
        this.pantryRepo = pantryRepo;
    }

    public void init(String uid) {
        this.currentUid = uid;
        pantryRepo.subscribePantryItems(uid, pantryItems);
    }

    public void addItem(PantryItem item) {
        pantryRepo.addItem(currentUid, item,
                () -> toastMessage.postValue("Item added"),
                () -> toastMessage.postValue("Failed to add item"));
    }

    public void updateItem(PantryItem item) {
        pantryRepo.updateItem(currentUid, item,
                () -> toastMessage.postValue("Item updated"),
                () -> toastMessage.postValue("Update failed"));
    }

    public void deleteItem(PantryItem item) {
        pantryRepo.deleteItem(currentUid, item.id,
                () -> toastMessage.postValue("Deleted"));
    }

    /**
     * Returns low stock count for dashboard summary
     */
    public int getLowStockCount() {
        List<PantryItem> items = pantryItems.getValue();
        if (items == null) return 0;
        int count = 0;
        for (PantryItem item : items) {
            if (item.getStockStatus() != PantryItem.StockStatus.OK) count++;
        }
        return count;
    }

    /**
     * Returns count of items expiring within 5 days
     */
    public int getExpiringCount() {
        List<PantryItem> items = pantryItems.getValue();
        if (items == null) return 0;
        int count = 0;
        for (PantryItem item : items) {
            long days = item.daysUntilExpiry();
            if (days >= 0 && days <= 5) count++;
        }
        return count;
    }

    @Override
    protected void onCleared() {
        pantryRepo.removeListener();
        super.onCleared();
    }
}
