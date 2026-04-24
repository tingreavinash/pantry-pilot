package com.pantrypilot.ui.settings;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pantrypilot.data.local.GroceryStoreEntity;
import com.pantrypilot.databinding.ItemStoreBinding;

import java.util.ArrayList;
import java.util.List;

public class StoresAdapter extends RecyclerView.Adapter<StoresAdapter.ViewHolder> {

    private final OnDeleteListener listener;
    private List<GroceryStoreEntity> stores = new ArrayList<>();
    public StoresAdapter(OnDeleteListener listener) {
        this.listener = listener;
    }

    public void updateStores(List<GroceryStoreEntity> newStores) {
        this.stores = newStores != null ? newStores : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStoreBinding b = ItemStoreBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(stores.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }

    public interface OnDeleteListener {
        void onDelete(GroceryStoreEntity store);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemStoreBinding binding;

        ViewHolder(ItemStoreBinding b) {
            super(b.getRoot());
            this.binding = b;
        }

        void bind(GroceryStoreEntity store, OnDeleteListener listener) {
            binding.tvStoreName.setText(store.name);
            binding.tvCoords.setText(String.format("%.4f, %.4f  •  %dm radius",
                    store.lat, store.lng, (int) store.radiusMeters));
            binding.btnDeleteStore.setOnClickListener(v -> listener.onDelete(store));
        }
    }
}
