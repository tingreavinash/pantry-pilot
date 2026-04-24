package com.pantrypilot.ui.pantry;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pantrypilot.R;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.databinding.ItemPantryBinding;

import java.util.ArrayList;
import java.util.List;

public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.ViewHolder> {

    private final OnItemClickListener listener;
    private List<PantryItem> items = new ArrayList<>();
    public PantryAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateItems(List<PantryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPantryBinding b = ItemPantryBinding.inflate(
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

    public interface OnItemClickListener {
        void onEdit(PantryItem item);

        void onDelete(PantryItem item);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPantryBinding binding;

        ViewHolder(ItemPantryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PantryItem item, OnItemClickListener listener) {
            binding.tvName.setText(item.name);
            binding.tvQuantityUnit.setText(item.quantity + " " + item.unit);
            binding.tvCategory.setText(item.category);

            // Stock chip
            switch (item.getStockStatus()) {
                case OUT:
                    binding.tvStockChip.setText("Out");
                    binding.tvStockChip.setBackgroundResource(R.drawable.chip_bg_red);
                    binding.tvStockChip.setTextColor(
                            binding.getRoot().getContext().getColor(R.color.chip_text_red));
                    break;
                case LOW:
                    binding.tvStockChip.setText("Low");
                    binding.tvStockChip.setBackgroundResource(R.drawable.chip_bg_amber);
                    binding.tvStockChip.setTextColor(
                            binding.getRoot().getContext().getColor(R.color.chip_text_amber));
                    break;
                default:
                    binding.tvStockChip.setText("OK");
                    binding.tvStockChip.setBackgroundResource(R.drawable.chip_bg_green);
                    binding.tvStockChip.setTextColor(
                            binding.getRoot().getContext().getColor(R.color.chip_text_green));
                    break;
            }

            // Expiry badge
            long days = item.daysUntilExpiry();
            if (days >= 0 && days <= 5) {
                binding.tvExpiryBadge.setVisibility(View.VISIBLE);
                binding.tvExpiryBadge.setText("Expires in " + days + " day" + (days == 1 ? "" : "s"));
            } else {
                binding.tvExpiryBadge.setVisibility(View.GONE);
            }

            binding.getRoot().setOnLongClickListener(v -> {
                listener.onEdit(item);
                return true;
            });
            binding.btnDelete.setOnClickListener(v -> listener.onDelete(item));
        }
    }
}
