package com.pantrypilot.ui.dashboard;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.pantrypilot.data.firebase.PantryRepository;
import com.pantrypilot.data.firebase.ShoppingRepository;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.data.model.ShoppingItem;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DashboardViewModel extends ViewModel {

    public final MutableLiveData<List<PantryItem>> pantryItems = new MutableLiveData<>();
    public final MutableLiveData<List<ShoppingItem>> shoppingItems = new MutableLiveData<>();
    public final MutableLiveData<String> householdName = new MutableLiveData<>("My Home");
    private final PantryRepository pantryRepo;
    private final ShoppingRepository shoppingRepo;

    @Inject
    public DashboardViewModel(PantryRepository pr, ShoppingRepository sr, FirebaseAuth auth) {
        this.pantryRepo = pr;
        this.shoppingRepo = sr;
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (!uid.isEmpty()) {
            pantryRepo.subscribePantryItems(uid, pantryItems);
            shoppingRepo.subscribeShoppingItems(uid, shoppingItems);
        }
    }

    public int totalItems() {
        List<PantryItem> l = pantryItems.getValue();
        return l == null ? 0 : l.size();
    }

    public int lowStockCount() {
        List<PantryItem> l = pantryItems.getValue();
        if (l == null) return 0;
        int c = 0;
        for (PantryItem p : l) if (p.getStockStatus() != PantryItem.StockStatus.OK) c++;
        return c;
    }

    public int expiringCount() {
        List<PantryItem> l = pantryItems.getValue();
        if (l == null) return 0;
        int c = 0;
        for (PantryItem p : l) {
            long d = p.daysUntilExpiry();
            if (d >= 0 && d <= 5) c++;
        }
        return c;
    }

    public int shoppingCount() {
        List<ShoppingItem> l = shoppingItems.getValue();
        return l == null ? 0 : l.size();
    }

    @Override
    protected void onCleared() {
        pantryRepo.removeListener();
        shoppingRepo.removeListener();
        super.onCleared();
    }
}
