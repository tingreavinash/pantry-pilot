package com.pantrypilot.data.firebase;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.pantrypilot.data.model.Meal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MealRepository {

    private final FirebaseFirestore db;
    private ListenerRegistration mealListener;

    @Inject
    public MealRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public void subscribeMeals(String uid, MutableLiveData<List<Meal>> liveData) {
        if (mealListener != null) mealListener.remove();
        mealListener = db.collection("households").document(uid)
                .collection("meals")
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null) {
                        List<Meal> meals = snapshot.toObjects(Meal.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            meals.get(i).id = snapshot.getDocuments().get(i).getId();
                        }
                        liveData.postValue(meals);
                    }
                });
    }

    public void removeListener() {
        if (mealListener != null) {
            mealListener.remove();
            mealListener = null;
        }
    }

    /**
     * Upsert a meal by its existing ID, or add a new document if ID is absent.
     */
    public void upsertMeal(String uid, Meal meal) {
        Map<String, Object> data = new HashMap<>();
        data.put("day", meal.day);
        data.put("mealName", meal.mealName);
        data.put("ingredients", meal.ingredients);

        CollectionReference col = db.collection("households").document(uid)
                .collection("meals");

        if (meal.id != null && !meal.id.isEmpty()) {
            col.document(meal.id).set(data);
        } else {
            col.add(data);
        }
    }
}
