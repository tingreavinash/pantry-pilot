package com.pantrypilot.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pantrypilot.R;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.databinding.FragmentDashboardBinding;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private RecentItemsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        setupRecyclerView();
        setupClickListeners(view);
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new RecentItemsAdapter(new ArrayList<>());
        binding.rvRecentActivity.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentActivity.setAdapter(adapter);
        binding.rvRecentActivity.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners(View view) {
        binding.cardTotalItems.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_pantry));
        binding.cardLowStock.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_pantry));
        binding.cardExpiring.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_pantry));
        binding.cardShopping.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_shopping));

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                Navigation.findNavController(view).navigate(R.id.settingsFragment);
                return true;
            }
            return false;
        });
    }

    private void observeViewModel() {
        viewModel.householdName.observe(getViewLifecycleOwner(), name ->
                binding.toolbar.setTitle(name));

        viewModel.pantryItems.observe(getViewLifecycleOwner(), items -> {
            binding.tvTotalItems.setText(String.valueOf(viewModel.totalItems()));
            binding.tvLowStock.setText(String.valueOf(viewModel.lowStockCount()));
            binding.tvExpiring.setText(String.valueOf(viewModel.expiringCount()));

            // Show last 5 items as recent activity
            List<PantryItem> recent = items.subList(0, Math.min(5, items.size()));
            adapter.updateItems(recent);

            binding.tvEmptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            binding.rvRecentActivity.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.shoppingItems.observe(getViewLifecycleOwner(), items ->
                binding.tvShoppingCount.setText(String.valueOf(viewModel.shoppingCount())));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
