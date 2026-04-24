package com.pantrypilot.ui.pantry;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.pantrypilot.R;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.databinding.BottomsheetAddPantryBinding;
import com.pantrypilot.databinding.FragmentPantryBinding;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PantryFragment extends Fragment {

    private static final String[] CATEGORIES = {
            "Dairy", "Produce", "Grains", "Proteins", "Snacks",
            "Beverages", "Spices", "Frozen", "Other"
    };
    private static final String[] UNITS = {
            "pcs", "kg", "g", "L", "ml", "pack", "dozen", "bunch"
    };

    private FragmentPantryBinding binding;
    private PantryViewModel viewModel;
    private PantryAdapter adapter;
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPantryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PantryViewModel.class);

        setupRecyclerView();
        setupSearch();
        setupFab(view);
        observeViewModel(view);
    }

    private void setupRecyclerView() {
        adapter = new PantryAdapter(new PantryAdapter.OnItemClickListener() {
            @Override
            public void onEdit(PantryItem item) {
                showAddEditSheet(item);
            }

            @Override
            public void onDelete(PantryItem item) {
                viewModel.deleteItem(item);
                Snackbar.make(binding.getRoot(), "Deleted " + item.name, Snackbar.LENGTH_LONG)
                        .setAction("Undo", v -> viewModel.addItem(item))
                        .show();
            }
        });
        binding.rvPantry.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPantry.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                searchQuery = s.toString().toLowerCase(Locale.getDefault());
                filterAndUpdate();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFab(View view) {
        binding.fabAddItem.setOnClickListener(v -> showAddEditSheet(null));
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_scan_barcode) {
                Navigation.findNavController(view).navigate(R.id.action_pantry_to_barcode);
                return true;
            }
            if (item.getItemId() == R.id.action_scan_receipt) {
                Navigation.findNavController(view).navigate(R.id.action_pantry_to_receipt);
                return true;
            }
            return false;
        });
    }

    private void observeViewModel(View view) {
        viewModel.pantryItems.observe(getViewLifecycleOwner(), items -> {
            binding.tvEmptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            filterAndUpdate();
        });
        viewModel.toastMessage.observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty())
                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show();
        });
    }

    private void filterAndUpdate() {
        List<PantryItem> all = viewModel.pantryItems.getValue();
        if (all == null) return;
        List<PantryItem> filtered = all.stream()
                .filter(i -> searchQuery.isEmpty() ||
                        i.name.toLowerCase(Locale.getDefault()).contains(searchQuery))
                .collect(Collectors.toList());
        adapter.updateItems(filtered);
    }

    private void showAddEditSheet(@Nullable PantryItem existing) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                R.style.ThemeOverlay_PantryPilot_BottomSheet);
        BottomsheetAddPantryBinding sheet =
                BottomsheetAddPantryBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        // Populate dropdowns
        sheet.spinnerCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, CATEGORIES));
        sheet.spinnerUnit.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, UNITS));

        // Pre-fill if editing
        if (existing != null) {
            sheet.tvSheetTitle.setText("Edit Item");
            sheet.etName.setText(existing.name);
            sheet.etQuantity.setText(String.valueOf(existing.quantity));
            sheet.etMinThreshold.setText(String.valueOf(existing.minThreshold));
            int catIdx = indexOf(CATEGORIES, existing.category);
            if (catIdx >= 0) sheet.spinnerCategory.setSelection(catIdx);
            int unitIdx = indexOf(UNITS, existing.unit);
            if (unitIdx >= 0) sheet.spinnerUnit.setSelection(unitIdx);
        }

        sheet.btnSave.setOnClickListener(v -> {
            String name = sheet.etName.getText().toString().trim();
            if (name.isEmpty()) {
                sheet.tilName.setError("Name is required");
                return;
            }
            PantryItem item = existing != null ? existing : new PantryItem();
            item.name = name;
            item.category = sheet.spinnerCategory.getSelectedItem().toString();
            item.unit = sheet.spinnerUnit.getSelectedItem().toString();
            try {
                item.quantity = Double.parseDouble(sheet.etQuantity.getText().toString());
                item.minThreshold = Double.parseDouble(sheet.etMinThreshold.getText().toString());
            } catch (NumberFormatException e) {
                item.quantity = 1;
                item.minThreshold = 1;
            }

            if (existing != null) viewModel.updateItem(item);
            else viewModel.addItem(item);
            dialog.dismiss();
        });

        sheet.btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private int indexOf(String[] arr, String val) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(val)) return i;
        return -1;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
