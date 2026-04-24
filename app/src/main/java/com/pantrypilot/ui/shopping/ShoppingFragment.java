package com.pantrypilot.ui.shopping;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.pantrypilot.R;
import com.pantrypilot.data.model.Member;
import com.pantrypilot.data.model.ShoppingItem;
import com.pantrypilot.databinding.BottomsheetAddShoppingBinding;
import com.pantrypilot.databinding.BottomsheetAssignMemberBinding;
import com.pantrypilot.databinding.FragmentShoppingBinding;
import com.pantrypilot.ui.members.MembersViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ShoppingFragment extends Fragment {

    private FragmentShoppingBinding binding;
    private ShoppingViewModel viewModel;
    private MembersViewModel membersViewModel;
    private ShoppingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentShoppingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ShoppingViewModel.class);
        membersViewModel = new ViewModelProvider(this).get(MembersViewModel.class);

        setupRecyclerView();
        setupToolbar();
        setupFab();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new ShoppingAdapter(new ShoppingAdapter.OnItemActionListener() {
            @Override
            public void onToggleBought(ShoppingItem item) {
                viewModel.toggleBought(item);
            }

            @Override
            public void onDelete(ShoppingItem item) {
                viewModel.deleteItem(item);
                Snackbar.make(binding.getRoot(), "Deleted " + item.name, Snackbar.LENGTH_LONG)
                        .setAction("Undo", v -> viewModel.addItem(item))
                        .show();
            }

            @Override
            public void onLongPress(ShoppingItem item) {
                showAssignMemberSheet(item);
            }
        });
        binding.rvShopping.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvShopping.setAdapter(adapter);
    }

    private void setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_bought) {
                viewModel.clearBought();
                return true;
            }
            return false;
        });
    }

    private void setupFab() {
        binding.fabAddItem.setOnClickListener(v -> showAddItemSheet());
        binding.btnAutoPopulate.setOnClickListener(v -> {
            viewModel.autoPopulate();
            Snackbar.make(binding.getRoot(), "Shopping list updated from pantry",
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.shoppingItems.observe(getViewLifecycleOwner(), items -> {
            adapter.updateItems(items);
            binding.tvEmptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            binding.rvShopping.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
            binding.toolbar.setSubtitle(String.format("Total: ₹%.0f", viewModel.runningTotal()));
        });
    }

    private void showAddItemSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                R.style.ThemeOverlay_PantryPilot_BottomSheet);
        BottomsheetAddShoppingBinding sheet =
                BottomsheetAddShoppingBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        sheet.btnSave.setOnClickListener(v -> {
            String name = sheet.etName.getText().toString().trim();
            if (name.isEmpty()) {
                sheet.tilName.setError("Required");
                return;
            }
            ShoppingItem item = new ShoppingItem();
            item.name = name;
            item.category = sheet.etCategory.getText().toString().trim();
            if (item.category.isEmpty()) item.category = "Other";
            try {
                item.quantity = Double.parseDouble(sheet.etQuantity.getText().toString());
            } catch (NumberFormatException e) {
                item.quantity = 1;
            }
            try {
                item.estimatedCost = Double.parseDouble(sheet.etCost.getText().toString());
            } catch (NumberFormatException e) {
                item.estimatedCost = 0;
            }
            viewModel.addItem(item);
            dialog.dismiss();
        });
        sheet.btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showAssignMemberSheet(ShoppingItem shoppingItem) {
        List<Member> members = membersViewModel.members.getValue();
        if (members == null || members.isEmpty()) {
            Snackbar.make(binding.getRoot(), "No members added yet", Snackbar.LENGTH_SHORT).show();
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                R.style.ThemeOverlay_PantryPilot_BottomSheet);
        BottomsheetAssignMemberBinding sheet =
                BottomsheetAssignMemberBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        List<String> names = new ArrayList<>();
        for (Member m : members) names.add(m.avatarEmoji + " " + m.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, names);
        sheet.lvMembers.setAdapter(adapter);
        sheet.lvMembers.setOnItemClickListener((parent, view, position, id) -> {
            viewModel.assignMember(shoppingItem, members.get(position).name);
            dialog.dismiss();
        });
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
