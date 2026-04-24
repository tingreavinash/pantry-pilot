package com.pantrypilot.ui.members;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EmojiGridAdapter extends RecyclerView.Adapter<EmojiGridAdapter.ViewHolder> {

    private final List<String> emojis;
    private final OnEmojiSelected listener;
    private int selectedPos = -1;
    public EmojiGridAdapter(List<String> emojis, OnEmojiSelected listener) {
        this.emojis = emojis;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setTextSize(24f);
        tv.setPadding(8, 8, 8, 8);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tv.setText(emojis.get(position));
        holder.tv.setBackgroundResource(position == selectedPos
                ? com.pantrypilot.R.drawable.chip_bg_green : 0);
        holder.tv.setOnClickListener(v -> {
            int prev = selectedPos;
            selectedPos = position;
            notifyItemChanged(prev);
            notifyItemChanged(position);
            listener.onSelected(emojis.get(position));
        });
    }

    @Override
    public int getItemCount() {
        return emojis.size();
    }

    public interface OnEmojiSelected {
        void onSelected(String emoji);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tv;

        ViewHolder(TextView tv) {
            super(tv);
            this.tv = tv;
        }
    }
}
