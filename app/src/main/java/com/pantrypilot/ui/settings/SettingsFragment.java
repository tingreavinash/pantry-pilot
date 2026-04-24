package com.pantrypilot.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.pantrypilot.BuildConfig;
import com.pantrypilot.R;
import com.pantrypilot.data.local.GroceryStoreEntity;
import com.pantrypilot.databinding.BottomsheetAddStoreBinding;
import com.pantrypilot.databinding.FragmentSettingsBinding;
import com.pantrypilot.ui.auth.AuthActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    private static final String[] DAYS_OF_WEEK = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private StoresAdapter storesAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setupToolbar();
        setupHouseholdSection();
        setupReminderSection();
        setupStoresSection();
        setupSignOut();

        binding.tvAppVersion.setText("PantryPilot v" + BuildConfig.VERSION_NAME);

        viewModel.householdName.observe(getViewLifecycleOwner(), name ->
                binding.etHouseholdName.setText(name));

        viewModel.stores.observe(getViewLifecycleOwner(), stores ->
                storesAdapter.updateStores(stores));
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().onBackPressed());
    }

    private void setupHouseholdSection() {
        binding.btnSaveHousehold.setOnClickListener(v -> {
            String name = binding.etHouseholdName.getText().toString().trim();
            if (name.isEmpty()) {
                Snackbar.make(binding.getRoot(), "Name cannot be empty", Snackbar.LENGTH_SHORT).show();
                return;
            }
            viewModel.saveHouseholdName(name);
            Snackbar.make(binding.getRoot(), "Household name saved", Snackbar.LENGTH_SHORT).show();
        });
    }

    private void setupReminderSection() {
        // Populate day spinner
        binding.spinnerReminderDay.setAdapter(
                new android.widget.ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item, DAYS_OF_WEEK));
        binding.spinnerReminderDay.setSelection(viewModel.getSavedDay());

        // Hour picker
        binding.numberPickerHour.setMinValue(6);
        binding.numberPickerHour.setMaxValue(22);
        binding.numberPickerHour.setValue(viewModel.getSavedHour());

        binding.btnSaveReminder.setOnClickListener(v -> {
            int day = binding.spinnerReminderDay.getSelectedItemPosition();
            int hour = binding.numberPickerHour.getValue();
            viewModel.saveReminderPrefs(day, hour);
            Snackbar.make(binding.getRoot(), "Reminder updated", Snackbar.LENGTH_SHORT).show();
        });
    }

    private void setupStoresSection() {
        storesAdapter = new StoresAdapter(store -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Remove store")
                    .setMessage("Remove \"" + store.name + "\"?")
                    .setPositiveButton("Remove", (d, w) -> viewModel.deleteStore(store))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        binding.rvStores.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvStores.setAdapter(storesAdapter);
        binding.rvStores.setNestedScrollingEnabled(false);

        binding.btnAddStore.setOnClickListener(v -> showAddStoreSheet());
    }

    private void showAddStoreSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                R.style.ThemeOverlay_PantryPilot_BottomSheet);
        BottomsheetAddStoreBinding sheet =
                BottomsheetAddStoreBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        sheet.btnSave.setOnClickListener(v -> {
            String name = sheet.etStoreName.getText().toString().trim();
            String latStr = sheet.etLat.getText().toString().trim();
            String lngStr = sheet.etLng.getText().toString().trim();

            if (name.isEmpty() || latStr.isEmpty() || lngStr.isEmpty()) {
                Snackbar.make(sheet.getRoot(), "All fields required",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }
            try {
                GroceryStoreEntity store = new GroceryStoreEntity();
                store.name = name;
                store.lat = Double.parseDouble(latStr);
                store.lng = Double.parseDouble(lngStr);
                String radiusStr = sheet.etRadius.getText().toString().trim();
                store.radiusMeters = radiusStr.isEmpty() ? 200f : Float.parseFloat(radiusStr);
                viewModel.addStore(store);
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Snackbar.make(sheet.getRoot(), "Invalid coordinates",
                        Snackbar.LENGTH_SHORT).show();
            }
        });
        sheet.btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupSignOut() {
        binding.btnSignOut.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Sign out")
                        .setMessage("Are you sure you want to sign out?")
                        .setPositiveButton("Sign out", (d, w) ->
                                viewModel.signOut(() -> {
                                    startActivity(new Intent(requireContext(), AuthActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                }))
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
