package com.pantrypilot.ui.shopping;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.pantrypilot.data.firebase.PantryRepository;
import com.pantrypilot.data.firebase.ShoppingRepository;
import com.pantrypilot.data.model.Member;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.data.model.ShoppingItem;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ShoppingViewModel extends ViewModel {

    public final MutableLiveData<List<ShoppingItem>> shoppingItems = new MutableLiveData<>();
    public final MutableLiveData<List<PantryItem>> pantryItems = new MutableLiveData<>();
    public final MutableLiveData<List<Member>> members = new MutableLiveData<>();
    private final ShoppingRepository shoppingRepo;
    private final PantryRepository pantryRepo;
    private final FirebaseAuth auth;

    @Inject
    public ShoppingViewModel(ShoppingRepository sr, PantryRepository pr, FirebaseAuth auth) {
        this.shoppingRepo = sr;
        this.pantryRepo = pr;
        this.auth = auth;
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (!uid.isEmpty()) {
            shoppingRepo.subscribeShoppingItems(uid, shoppingItems);
            pantryRepo.subscribePantryItems(uid, pantryItems);
        }
    }

    private String uid() {
        return auth.getCurrentUser().getUid();
    }

    public void toggleBought(ShoppingItem item) {
        shoppingRepo.setBought(uid(), item.id, !item.bought);
    }

    public void addItem(ShoppingItem item) {
        shoppingRepo.addItem(uid(), item);
    }

    public void deleteItem(ShoppingItem item) {
        shoppingRepo.deleteItem(uid(), item.id);
    }

    public void clearBought() {
        List<ShoppingItem> items = shoppingItems.getValue();
        if (items != null) shoppingRepo.clearBoughtItems(uid(), items);
    }

    public void autoPopulate() {
        List<PantryItem> pantry = pantryItems.getValue();
        List<ShoppingItem> shopping = shoppingItems.getValue();
        if (pantry != null && shopping != null)
            shoppingRepo.autoPopulate(uid(), pantry, shopping);
    }

    public double runningTotal() {
        List<ShoppingItem> items = shoppingItems.getValue();
        if (items == null) return 0;
        double total = 0;
        for (ShoppingItem i : items) if (!i.bought) total += i.estimatedCost;
        return total;
    }

    public void assignMember(ShoppingItem item, String memberName) {
        shoppingRepo.assignMember(uid(), item.id, memberName);
    }

    @Override
    protected void onCleared() {
        shoppingRepo.removeListener();
        pantryRepo.removeListener();
        super.onCleared();
    }
}
