package com.pantrypilot.ui.meals;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.pantrypilot.R;
import com.pantrypilot.data.model.Meal;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.databinding.BottomsheetEditMealBinding;
import com.pantrypilot.databinding.ItemMealDayBinding;

import java.util.ArrayList;
import java.util.List;

public class MealDayAdapter extends RecyclerView.Adapter<MealDayAdapter.ViewHolder> {

    private final MealViewModel viewModel;
    private final FragmentManager fragmentManager;
    private List<String> days = new ArrayList<>();

    public MealDayAdapter(MealViewModel vm, FragmentManager fm) {
        this.viewModel = vm;
        this.fragmentManager = fm;
    }

    public void setDays(List<String> days) {
        this.days = days;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMealDayBinding b = ItemMealDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String day = days.get(position);
        Meal meal = viewModel.getMealForDay(day);
        holder.bind(day, meal, viewModel, this);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMealDayBinding binding;
        private boolean expanded = false;

        ViewHolder(ItemMealDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String day, Meal meal, MealViewModel vm, MealDayAdapter adapter) {
            binding.tvDay.setText(day);

            if (meal != null && meal.mealName != null && !meal.mealName.isEmpty()) {
                binding.tvMealName.setText(meal.mealName);
                binding.tvMealName.setVisibility(View.VISIBLE);
                binding.tvNoMeal.setVisibility(View.GONE);
            } else {
                binding.tvMealName.setVisibility(View.GONE);
                binding.tvNoMeal.setVisibility(View.VISIBLE);
            }

            // Toggle expand
            binding.getRoot().setOnClickListener(v -> {
                expanded = !expanded;
                binding.layoutIngredients.setVisibility(expanded && meal != null
                        ? View.VISIBLE : View.GONE);
                if (expanded && meal != null)
                    populateIngredients(meal, vm, binding.layoutIngredients);
            });

            // Edit button
            binding.btnEditMeal.setOnClickListener(v -> showEditSheet(day, meal, vm));

            // Add missing button
            binding.btnAddMissing.setOnClickListener(v -> {
                if (meal != null) vm.addMissingToShoppingList(meal);
            });
        }

        private void populateIngredients(Meal meal, MealViewModel vm, LinearLayout container) {
            container.removeAllViews();
            if (meal.ingredients == null) return;
            for (String ing : meal.ingredients) {
                TextView tv = new TextView(container.getContext());
                PantryItem.StockStatus status = vm.ingredientStatus(ing);
                String emoji = status == PantryItem.StockStatus.OK ? "✅ "
                        : status == PantryItem.StockStatus.LOW ? "⚠️ " : "❌ ";
                tv.setText(emoji + ing);
                tv.setPadding(0, 4, 0, 4);
                container.addView(tv);
            }
        }

        private void showEditSheet(String day, Meal existing, MealViewModel vm) {
            BottomSheetDialog dialog = new BottomSheetDialog(binding.getRoot().getContext(),
                    R.style.ThemeOverlay_PantryPilot_BottomSheet);
            BottomsheetEditMealBinding sheet = BottomsheetEditMealBinding.inflate(
                    LayoutInflater.from(binding.getRoot().getContext()));
            dialog.setContentView(sheet.getRoot());

            sheet.tvSheetTitle.setText("Edit " + day);
            if (existing != null && existing.mealName != null)
                sheet.etMealName.setText(existing.mealName);

            // Populate existing ingredients as chips
            List<String> ingredients = new ArrayList<>();
            if (existing != null && existing.ingredients != null)
                ingredients.addAll(existing.ingredients);
            refreshIngredientChips(sheet, ingredients, dialog);

            sheet.btnAddIngredient.setOnClickListener(v -> {
                String ing = sheet.etIngredient.getText().toString().trim();
                if (!ing.isEmpty()) {
                    ingredients.add(ing);
                    sheet.etIngredient.setText("");
                    refreshIngredientChips(sheet, ingredients, dialog);
                }
            });

            sheet.btnSave.setOnClickListener(v -> {
                Meal meal = existing != null ? existing : new Meal();
                meal.day = day;
                meal.mealName = sheet.etMealName.getText().toString().trim();
                meal.ingredients = new ArrayList<>(ingredients);
                vm.upsertMeal(meal);
                dialog.dismiss();
            });

            sheet.btnCancel.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }

        private void refreshIngredientChips(BottomsheetEditMealBinding sheet,
                                            List<String> ingredients,
                                            BottomSheetDialog dialog) {
            sheet.chipGroupIngredients.removeAllViews();
            for (String ing : ingredients) {
                Chip chip = new Chip(sheet.getRoot().getContext());
                chip.setText(ing);
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(v -> {
                    ingredients.remove(ing);
                    refreshIngredientChips(sheet, ingredients, dialog);
                });
                sheet.chipGroupIngredients.addView(chip);
            }
        }
    }
}
