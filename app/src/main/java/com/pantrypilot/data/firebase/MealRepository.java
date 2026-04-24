package com.pantrypilot.data.firebase;

import androidx.lifecycle.MutableLiveData;

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
    private ListenerRegistration listener;

    @Inject
    public MealRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public void subscribe(String uid, MutableLiveData<List<Meal>> liveData) {
        if (listener != null) listener.remove();
        listener = db.collection("households").document(uid)
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
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    public void upsertMeal(String uid, Meal meal) {
        Map<String, Object> data = new HashMap<>();
        data.put("day", meal.day);
        data.put("mealName", meal.mealName);
        data.put("ingredients", meal.ingredients);

        if (meal.id != null && !meal.id.isEmpty()) {
            db.collection("households").document(uid)
                    .collection("meals").document(meal.id).set(data);
        } else {
            db.collection("households").document(uid)
                    .collection("meals").add(data);
        }
    }
}
