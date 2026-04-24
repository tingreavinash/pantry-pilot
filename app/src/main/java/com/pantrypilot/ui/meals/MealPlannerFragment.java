package com.pantrypilot.ui.meals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pantrypilot.databinding.FragmentMealsBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MealPlannerFragment extends Fragment {

    private FragmentMealsBinding binding;
    private MealViewModel viewModel;
    private MealDayAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMealsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MealViewModel.class);

        adapter = new MealDayAdapter(viewModel, getParentFragmentManager());
        binding.rvMeals.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMeals.setAdapter(adapter);

        // Seed adapter with 7 days; data fills in from LiveData
        adapter.setDays(MealViewModel.DAYS);

        viewModel.meals.observe(getViewLifecycleOwner(), meals -> adapter.notifyDataSetChanged());
        viewModel.pantryItems.observe(getViewLifecycleOwner(), p -> adapter.notifyDataSetChanged());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
