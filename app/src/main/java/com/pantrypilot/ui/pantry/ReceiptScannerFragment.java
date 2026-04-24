package com.pantrypilot.ui.pantry;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.databinding.FragmentReceiptScannerBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ReceiptScannerFragment extends Fragment {

    private FragmentReceiptScannerBinding binding;
    private PantryViewModel viewModel;
    private ReceiptItemsAdapter adapter;
    private List<ReceiptParser.ParsedItem> parsedItems = null;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                processImage(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReceiptScannerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PantryViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        adapter = new ReceiptItemsAdapter();
        binding.rvReceiptItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReceiptItems.setAdapter(adapter);

        binding.btnPickPhoto.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        binding.btnConfirm.setOnClickListener(v -> {
            if (parsedItems != null) {
                confirmItems();
                Navigation.findNavController(view).popBackStack();
            }
        });

        binding.btnSkipAll.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        showIdleState();
    }

    private void processImage(Uri uri) {
        showProcessingState();
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), uri);
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener(result -> {
                        List<String> pantryNames = new ArrayList<>();
                        if (viewModel.pantryItems.getValue() != null)
                            for (PantryItem p : viewModel.pantryItems.getValue())
                                pantryNames.add(p.name);

                        parsedItems = ReceiptParser.parse(result.getText(), pantryNames);
                        showReviewState();

                        if (parsedItems.size() < 3) {
                            Snackbar.make(binding.getRoot(),
                                    "Receipt unclear — try better lighting or a flatter surface",
                                    Snackbar.LENGTH_LONG).show();
                        }
                        adapter.setItems(parsedItems);
                    })
                    .addOnFailureListener(e -> {
                        showIdleState();
                        Snackbar.make(binding.getRoot(),
                                "OCR failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    });
        } catch (IOException e) {
            showIdleState();
            Snackbar.make(binding.getRoot(), "Could not read image", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void confirmItems() {
        for (ReceiptParser.ParsedItem parsed : parsedItems) {
            if (!parsed.selected) continue;
            PantryItem item = new PantryItem();
            item.name = parsed.name;
            item.quantity = parsed.quantity;
            item.category = "Other";
            item.unit = "pcs";
            item.minThreshold = 1;
            viewModel.addItem(item);
        }
    }

    private void showIdleState() {
        binding.groupIdle.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
        binding.groupReview.setVisibility(View.GONE);
    }

    private void showProcessingState() {
        binding.groupIdle.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.groupReview.setVisibility(View.GONE);
    }

    private void showReviewState() {
        binding.groupIdle.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.GONE);
        binding.groupReview.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
