package com.pantrypilot.data.model;

import java.util.List;

/**
 * Mirrors Firestore: households/{uid}/meals/{docId}
 */
public class Meal {
    public String id;
    public String day;           // "Monday" … "Sunday"
    public String mealName;
    public List<String> ingredients;

    public Meal() {
    }
}
