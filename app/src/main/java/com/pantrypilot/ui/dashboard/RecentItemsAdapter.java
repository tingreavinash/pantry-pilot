package com.pantrypilot.ui.dashboard;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pantrypilot.R;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.databinding.ItemRecentBinding;

import java.util.List;

public class RecentItemsAdapter extends RecyclerView.Adapter<RecentItemsAdapter.ViewHolder> {

    private List<PantryItem> items;

    public RecentItemsAdapter(List<PantryItem> items) {
        this.items = items;
    }

    public void updateItems(List<PantryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecentBinding b = ItemRecentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecentBinding binding;

        ViewHolder(ItemRecentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PantryItem item) {
            binding.tvItemName.setText(item.name);
            binding.tvCategory.setText(item.category);

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
        }
    }
}
