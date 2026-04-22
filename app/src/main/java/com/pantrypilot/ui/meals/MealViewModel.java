package com.pantrypilot.ui.meals;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.pantrypilot.data.firebase.MealRepository;
import com.pantrypilot.data.firebase.PantryRepository;
import com.pantrypilot.data.firebase.ShoppingRepository;
import com.pantrypilot.data.model.Meal;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.data.model.ShoppingItem;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MealViewModel extends ViewModel {

    public static final List<String> DAYS = Arrays.asList(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    public final MutableLiveData<List<Meal>> meals = new MutableLiveData<>();
    public final MutableLiveData<List<PantryItem>> pantryItems = new MutableLiveData<>();
    private final MealRepository mealRepo;
    private final PantryRepository pantryRepo;
    private final ShoppingRepository shoppingRepo;
    private final FirebaseAuth auth;

    @Inject
    public MealViewModel(MealRepository mr, PantryRepository pr,
                         ShoppingRepository sr, FirebaseAuth auth) {
        this.mealRepo = mr;
        this.pantryRepo = pr;
        this.shoppingRepo = sr;
        this.auth = auth;
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (!uid.isEmpty()) {
            mealRepo.subscribeMeals(uid, meals);
            pantryRepo.subscribePantryItems(uid, pantryItems);
        }
    }

    public void upsertMeal(Meal meal) {
        mealRepo.upsertMeal(auth.getCurrentUser().getUid(), meal);
    }

    public void addMissingToShoppingList(Meal meal) {
        if (meal.ingredients == null) return;
        String uid = auth.getCurrentUser().getUid();
        List<PantryItem> pantry = pantryItems.getValue();

        Set<String> pantryNames = new HashSet<>();
        if (pantry != null) {
            for (PantryItem p : pantry) pantryNames.add(p.name.toLowerCase().trim());
        }

        for (String ingredient : meal.ingredients) {
            if (!pantryNames.contains(ingredient.toLowerCase().trim())) {
                ShoppingItem item = new ShoppingItem();
                item.name = ingredient;
                item.category = "Produce";
                item.quantity = 1;
                item.unit = "pcs";
                shoppingRepo.addItem(uid, item);
            }
        }
    }

    public PantryItem.StockStatus ingredientStatus(String ingredient) {
        List<PantryItem> pantry = pantryItems.getValue();
        if (pantry == null) return PantryItem.StockStatus.OUT;
        for (PantryItem p : pantry) {
            if (p.name.equalsIgnoreCase(ingredient.trim())) return p.getStockStatus();
        }
        return PantryItem.StockStatus.OUT;
    }

    @Override
    protected void onCleared() {
        mealRepo.removeListener();
        pantryRepo.removeListener();
        super.onCleared();
    }
}
