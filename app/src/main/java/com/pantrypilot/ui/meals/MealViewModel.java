// ── MealViewModel.java ────────────────────────────────────────────────────────
// Save to: ui/meals/MealViewModel.java
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

import java.util.ArrayList;
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

    private final MealRepository mealRepo;
    private final PantryRepository pantryRepo;
    private final ShoppingRepository shoppingRepo;
    private final FirebaseAuth auth;

    public final MutableLiveData<List<Meal>> meals = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<PantryItem>> pantryItems = new MutableLiveData<>(new ArrayList<>());

    @Inject
    public MealViewModel(MealRepository mr, PantryRepository pr,
                         ShoppingRepository sr, FirebaseAuth auth) {
        this.mealRepo = mr;
        this.pantryRepo = pr;
        this.shoppingRepo = sr;
        this.auth = auth;
        String uid = uid();
        if (uid != null) {
            mealRepo.subscribe(uid, meals);
            pantryRepo.subscribe(uid, pantryItems);
        }
    }

    private String uid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public void upsertMeal(Meal meal) {
        mealRepo.upsertMeal(uid(), meal);
    }

    /**
     * Returns the meal for a given day or null
     */
    public Meal getMealForDay(String day) {
        List<Meal> list = meals.getValue();
        if (list == null) return null;
        for (Meal m : list) if (day.equals(m.day)) return m;
        return null;
    }

    public PantryItem.StockStatus ingredientStatus(String ingredient) {
        List<PantryItem> pantry = pantryItems.getValue();
        if (pantry == null) return PantryItem.StockStatus.OUT;
        for (PantryItem p : pantry)
            if (p.name.equalsIgnoreCase(ingredient.trim())) return p.getStockStatus();
        return PantryItem.StockStatus.OUT;
    }

    public void addMissingToShoppingList(Meal meal) {
        if (meal.ingredients == null) return;
        String uid = uid();
        List<PantryItem> pantry = pantryItems.getValue();
        Set<String> pantryNames = new HashSet<>();
        if (pantry != null)
            for (PantryItem p : pantry) pantryNames.add(p.name.toLowerCase().trim());
        for (String ing : meal.ingredients) {
            if (!pantryNames.contains(ing.toLowerCase().trim())) {
                ShoppingItem item = new ShoppingItem();
                item.name = ing;
                item.category = "Produce";
                item.quantity = 1;
                item.unit = "pcs";
                shoppingRepo.addItem(uid, item);
            }
        }
    }

    @Override
    protected void onCleared() {
        mealRepo.removeListener();
        pantryRepo.removeListener();
        super.onCleared();
    }
}
