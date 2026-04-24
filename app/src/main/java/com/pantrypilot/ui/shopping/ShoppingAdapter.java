package com.pantrypilot.ui.shopping;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pantrypilot.data.model.ShoppingItem;
import com.pantrypilot.databinding.ItemShoppingBinding;

import java.util.ArrayList;
import java.util.List;

public class ShoppingAdapter extends RecyclerView.Adapter<ShoppingAdapter.ViewHolder> {

    private final OnItemActionListener listener;
    private List<ShoppingItem> items = new ArrayList<>();
    public ShoppingAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void updateItems(List<ShoppingItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemShoppingBinding b = ItemShoppingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface OnItemActionListener {
        void onToggleBought(ShoppingItem item);

        void onDelete(ShoppingItem item);

        void onLongPress(ShoppingItem item);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemShoppingBinding binding;

        ViewHolder(ItemShoppingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ShoppingItem item, OnItemActionListener listener) {
            binding.checkboxBought.setOnCheckedChangeListener(null);
            binding.checkboxBought.setChecked(item.bought);
            binding.tvName.setText(item.name);

            // Strikethrough when bought
            if (item.bought) {
                binding.tvName.setPaintFlags(
                        binding.tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvName.setAlpha(0.5f);
            } else {
                binding.tvName.setPaintFlags(
                        binding.tvName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvName.setAlpha(1f);
            }

            // Cost
            if (item.estimatedCost > 0) {
                binding.tvCost.setVisibility(View.VISIBLE);
                binding.tvCost.setText(String.format("₹%.0f", item.estimatedCost));
            } else {
                binding.tvCost.setVisibility(View.GONE);
            }

            // Assigned member
            if (item.assignedTo != null && !item.assignedTo.isEmpty()) {
                binding.tvAssigned.setVisibility(View.VISIBLE);
                binding.tvAssigned.setText(item.assignedTo);
            } else {
                binding.tvAssigned.setVisibility(View.GONE);
            }

            binding.checkboxBought.setOnCheckedChangeListener(
                    (v, checked) -> listener.onToggleBought(item));
            binding.btnDelete.setOnClickListener(v -> listener.onDelete(item));
            binding.getRoot().setOnLongClickListener(v -> {
                listener.onLongPress(item);
                return true;
            });
        }
    }
}
