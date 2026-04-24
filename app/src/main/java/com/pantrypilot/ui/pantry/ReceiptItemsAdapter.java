package com.pantrypilot.ui.pantry;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pantrypilot.databinding.ItemReceiptBinding;

import java.util.ArrayList;
import java.util.List;

public class ReceiptItemsAdapter extends RecyclerView.Adapter<ReceiptItemsAdapter.ViewHolder> {

    private List<ReceiptParser.ParsedItem> items = new ArrayList<>();

    public void setItems(List<ReceiptParser.ParsedItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReceiptBinding b = ItemReceiptBinding.inflate(
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
        private final ItemReceiptBinding binding;

        ViewHolder(ItemReceiptBinding b) {
            super(b.getRoot());
            this.binding = b;
        }

        void bind(ReceiptParser.ParsedItem item) {
            binding.checkboxSelected.setOnCheckedChangeListener(null);
            binding.checkboxSelected.setChecked(item.selected);
            binding.tvItemName.setText(item.name);
            binding.tvQuantity.setText("×" + String.format("%.0f", item.quantity));
            binding.tvPrice.setText(String.format("₹%.0f", item.price));

            if (item.isUpdate) {
                binding.tvMatchHint.setVisibility(View.VISIBLE);
                binding.tvMatchHint.setText("Update: " + item.matchedName);
            } else {
                binding.tvMatchHint.setVisibility(View.GONE);
            }

            binding.checkboxSelected.setOnCheckedChangeListener(
                    (v, checked) -> item.selected = checked);
        }
    }
}
