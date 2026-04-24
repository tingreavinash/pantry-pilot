package com.pantrypilot.ui.members;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pantrypilot.R;
import com.pantrypilot.data.model.Member;
import com.pantrypilot.databinding.BottomsheetAddMemberBinding;
import com.pantrypilot.databinding.FragmentMembersBinding;

import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MembersFragment extends Fragment {

    private static final List<String> EMOJIS = Arrays.asList(
            "👨", "👩", "👦", "👧", "👶", "👴", "👵", "🧑", "👱", "👮",
            "🧑‍💻", "👩‍🍳", "🧑‍🌾", "👩‍🔬", "🧑‍🎨", "👩‍💼", "🧑‍🚀",
            "🐱", "🐶", "🦁", "🐯", "🐻", "🦊", "🐼", "🐨", "🦄", "🐸", "🐙", "🦋", "🌟"
    );

    private FragmentMembersBinding binding;
    private MembersViewModel viewModel;
    private MembersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMembersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MembersViewModel.class);

        adapter = new MembersAdapter(viewModel, member -> viewModel.deleteMember(member));
        binding.rvMembers.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvMembers.setAdapter(adapter);

        binding.fabAddMember.setOnClickListener(v -> showAddMemberSheet());

        viewModel.members.observe(getViewLifecycleOwner(), members -> {
            adapter.updateMembers(members);
            binding.tvEmptyState.setVisibility(members.isEmpty() ? View.VISIBLE : View.GONE);
            binding.rvMembers.setVisibility(members.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void showAddMemberSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                R.style.ThemeOverlay_PantryPilot_BottomSheet);
        BottomsheetAddMemberBinding sheet =
                BottomsheetAddMemberBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        final String[] selectedEmoji = {"👤"};

        // Emoji grid via adapter
        EmojiGridAdapter emojiAdapter = new EmojiGridAdapter(EMOJIS, emoji -> {
            selectedEmoji[0] = emoji;
            sheet.tvSelectedEmoji.setText(emoji);
        });
        sheet.rvEmojiGrid.setLayoutManager(new GridLayoutManager(requireContext(), 6));
        sheet.rvEmojiGrid.setAdapter(emojiAdapter);
        sheet.tvSelectedEmoji.setText(selectedEmoji[0]);

        sheet.btnSave.setOnClickListener(v -> {
            String name = sheet.etName.getText().toString().trim();
            if (name.isEmpty()) {
                sheet.tilName.setError("Required");
                return;
            }
            Member member = new Member();
            member.name = name;
            member.avatarEmoji = selectedEmoji[0];
            viewModel.addMember(member);
            dialog.dismiss();
        });
        sheet.btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
