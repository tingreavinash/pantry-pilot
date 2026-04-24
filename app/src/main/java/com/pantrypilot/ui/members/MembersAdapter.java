package com.pantrypilot.ui.members;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pantrypilot.data.model.Member;
import com.pantrypilot.databinding.ItemMemberBinding;

import java.util.ArrayList;
import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.ViewHolder> {

    private final MembersViewModel viewModel;
    private final OnDeleteListener deleteListener;
    private List<Member> members = new ArrayList<>();
    public MembersAdapter(MembersViewModel vm, OnDeleteListener dl) {
        this.viewModel = vm;
        this.deleteListener = dl;
    }

    public void updateMembers(List<Member> newMembers) {
        this.members = newMembers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMemberBinding b = ItemMemberBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(members.get(position), viewModel, deleteListener);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public interface OnDeleteListener {
        void onDelete(Member member);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMemberBinding binding;

        ViewHolder(ItemMemberBinding b) {
            super(b.getRoot());
            this.binding = b;
        }

        void bind(Member member, MembersViewModel vm, OnDeleteListener dl) {
            binding.tvEmoji.setText(member.avatarEmoji);
            binding.tvName.setText(member.name);
            binding.tvAssignedCount.setText(vm.assignedCount(member.name) + " items");
            binding.btnDelete.setOnClickListener(v -> dl.onDelete(member));
        }
    }
}
