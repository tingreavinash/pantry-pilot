package com.pantrypilot.ui.meals;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.layout.PaddingValues;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.State;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;

import com.pantrypilot.data.model.Meal;
import com.pantrypilot.data.model.PantryItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ─── Screen ───────────────────────────────────────────────────────────────────
public class MealPlannerScreen {

    @Composable
    public static void MealPlannerScreen() {
        MealViewModel vm = hiltViewModel();
        State<List<Meal>> meals = vm.meals.observeAsState(List.of());
        MutableState<Meal> editMeal = remember {
            mutableStateOf(null)
        }

        // Build day → meal map
        Map<String, Meal> mealByDay = new HashMap<>();
        for (Meal m : meals.getValue()) mealByDay.put(m.day, m);

        Scaffold(
                topBar = {TopAppBar(title = {Text("Meal Planner")}); }
        ){
            padding ->
                    LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                items(MealViewModel.DAYS) {
                    day ->
                            MealDayCard(
                                    day = day,
                                    meal = mealByDay.get(day),
                                    vm = vm,
                                    onEdit = meal -> editMeal.setValue(meal)
                            );
                }
            }
        }

        if (editMeal.getValue() != null) {
            EditMealSheet(
                    meal = editMeal.getValue(),
                    onDismiss = () -> editMeal.setValue(null),
                    onSave = meal -> {
                        vm.upsertMeal(meal);
                        editMeal.setValue(null);
                    }
            );
        }
    }

    @Composable
    private static void MealDayCard(String day, Meal meal, MealViewModel vm,
                                    java.util.function.Consumer<Meal> onEdit) {
        MutableState<Boolean> expanded = remember(day) {
            mutableStateOf(false)
        }
        boolean hasMeal = meal != null;

        Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = () -> expanded.setValue(!expanded.getValue())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Text(day, style = MaterialTheme.typography.titleMedium);
                    Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hasMeal) {
                            Text(meal.mealName, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary);
                        } else {
                            Text("Not planned", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant);
                        }
                        IconButton(onClick = () -> {
                            Meal target = hasMeal ? meal : new Meal();
                            if (target.day == null) target.day = day;
                            onEdit.accept(target);
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Edit, "Edit", modifier = Modifier.size(16.dp));
                        }
                    }
                }

                AnimatedVisibility(visible = expanded.getValue() && hasMeal) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text("Ingredients", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant);
                        Spacer(Modifier.height(6.dp));
                        meal.ingredients.forEach(ing -> {
                            PantryItem.StockStatus status = vm.ingredientStatus(ing);
                            Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                String statusEmoji;
                                switch (status) {
                                    case OK:
                                        statusEmoji = "✅";
                                        break;
                                    case LOW:
                                        statusEmoji = "⚠️";
                                        break;
                                    default:
                                        statusEmoji = "❌";
                                        break;
                                }
                                Text(statusEmoji, fontSize = 14.sp);
                                Text(ing, style = MaterialTheme.typography.bodySmall);
                            }
                        });
                        Spacer(Modifier.height(8.dp));
                        OutlinedButton(
                                onClick = () -> vm.addMissingToShoppingList(meal),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ShoppingCart, null, modifier = Modifier.size(14.dp));
                            Spacer(Modifier.width(6.dp));
                            Text("Add missing to shopping list", fontSize = 12.sp);
                        }
                    }
                }
            }
        }
    }

    @Composable
    private static void EditMealSheet(Meal meal, Runnable onDismiss,
                                      java.util.function.Consumer<Meal> onSave) {
        MutableState<String> mealName = remember {
            mutableStateOf(meal.mealName != null ? meal.mealName : "")
        }
        MutableState<List<String>> ingredients = remember {
            mutableStateOf(meal.ingredients != null ? new ArrayList<>(meal.ingredients) : new ArrayList<>())
        }
        MutableState<String> newIngredient = remember {
            mutableStateOf("")
        }

        ModalBottomSheet(onDismissRequest = onDismiss::run,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text("Edit " + meal.day, style = MaterialTheme.typography.titleLarge);
                Spacer(Modifier.height(16.dp));
                OutlinedTextField(value = mealName.getValue(), onValueChange = v -> mealName.setValue(v),
                        label = {Text("Meal name")}, modifier = Modifier.fillMaxWidth(), singleLine = true);
                Spacer(Modifier.height(12.dp));
                Text("Ingredients", style = MaterialTheme.typography.labelMedium);
                Spacer(Modifier.height(6.dp));
                // Ingredient chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ingredients.getValue()) {
                        ing ->
                                InputChip(
                                        selected = false,
                                        onClick = {},
                                        label = {Text(ing)},
                                        trailingIcon = {
                                                IconButton(onClick = () -> {
                                                    List<String> updated = new ArrayList<>(ingredients.getValue());
                                                    updated.remove(ing);
                                                    ingredients.setValue(updated);
                                                }, modifier = Modifier.size(18.dp)){
                                                Icon(Icons.Filled.Close, "Remove", modifier = Modifier.size(12.dp));
                                    }
                                }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp));
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                            value = newIngredient.getValue(),
                            onValueChange = v -> newIngredient.setValue(v),
                            label = {Text("Add ingredient")},
                            modifier = Modifier.weight(1f), singleLine = true
                    );
                    IconButton(onClick = () -> {
                        if (!newIngredient.getValue().isEmpty()) {
                            List<String> updated = new ArrayList<>(ingredients.getValue());
                            updated.add(newIngredient.getValue().trim());
                            ingredients.setValue(updated);
                            newIngredient.setValue("");
                        }
                    }) {
                        Icon(Icons.Filled.Add, "Add");
                    }
                }
                Spacer(Modifier.height(20.dp));
                Button(onClick = () -> {
                    meal.mealName = mealName.getValue();
                    meal.ingredients = ingredients.getValue();
                    onSave.accept(meal);
                }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Save Meal");
                }
                Spacer(Modifier.height(32.dp));
            }
        }
    }
}
