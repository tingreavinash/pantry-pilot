package com.pantrypilot.ui.pantry;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.pantrypilot.R;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.databinding.BottomsheetAddPantryBinding;
import com.pantrypilot.databinding.FragmentBarcodeScannerBinding;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BarcodeScannerFragment extends Fragment {

    private static final String TAG = "BarcodeScanner";
    private static final String FOOD_FACTS_URL = "https://world.openfoodfacts.org/api/v0/product/";
    private final AtomicBoolean analyzing = new AtomicBoolean(true);
    private FragmentBarcodeScannerBinding binding;
    private PantryViewModel viewModel;
    private ExecutorService cameraExecutor;
    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else Snackbar.make(binding.getRoot(),
                        "Camera permission required for barcode scanning",
                        Snackbar.LENGTH_LONG).show();
            });
    private Camera camera;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBarcodeScannerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PantryViewModel.class);
        cameraExecutor = Executors.newSingleThreadExecutor();

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        binding.btnTorch.setOnClickListener(v -> {
            if (camera != null) {
                boolean newState = !(camera.getCameraInfo().getTorchState().getValue() == 1);
                camera.getCameraControl().enableTorch(newState);
                binding.btnTorch.setIconResource(newState
                        ? R.drawable.ic_flashlight_off : R.drawable.ic_flashlight_on);
            }
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (!analyzing.get()) {
                        imageProxy.close();
                        return;
                    }

                    InputImage image = InputImage.fromMediaImage(
                            imageProxy.getImage(),
                            imageProxy.getImageInfo().getRotationDegrees());

                    BarcodeScanning.getClient().process(image)
                            .addOnSuccessListener(barcodes -> {
                                for (Barcode barcode : barcodes) {
                                    String raw = barcode.getRawValue();
                                    if (raw != null && !raw.isEmpty() && analyzing.getAndSet(false)) {
                                        requireActivity().runOnUiThread(() ->
                                                binding.tvStatus.setText("Looking up " + raw + "…"));
                                        lookupBarcode(raw);
                                    }
                                }
                            })
                            .addOnCompleteListener(t -> imageProxy.close());
                });

                provider.bindToLifecycle(getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);

            } catch (Exception e) {
                Log.e(TAG, "Camera start failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void lookupBarcode(String barcode) {
        cameraExecutor.execute(() -> {
            try {
                URL url = new URL(FOOD_FACTS_URL + barcode + ".json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                boolean found = json.optInt("status", 0) == 1;
                String name = "";
                String quantity = "";
                String category = "Other";

                if (found) {
                    JSONObject product = json.optJSONObject("product");
                    if (product != null) {
                        name = product.optString("product_name", "");
                        quantity = product.optString("quantity", "");
                        category = mapCategory(product.optString("categories_tags", ""));
                    }
                }

                final String fName = name;
                final String fQuantity = quantity;
                final String fCategory = category;

                requireActivity().runOnUiThread(() ->
                        showResultSheet(barcode, fName, fQuantity, fCategory));

            } catch (Exception e) {
                Log.w(TAG, "Lookup failed", e);
                requireActivity().runOnUiThread(() ->
                        showResultSheet(barcode, "", "", "Other"));
            }
        });
    }

    private void showResultSheet(String barcode, String name, String quantity, String category) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                R.style.ThemeOverlay_PantryPilot_BottomSheet);
        BottomsheetAddPantryBinding sheet =
                BottomsheetAddPantryBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        sheet.tvSheetTitle.setText(!name.isEmpty() ? "Product found!" : "Product not found");
        sheet.etName.setText(name);

        // Parse quantity number from string like "500g" or "1L"
        double qty = 1;
        try {
            qty = Double.parseDouble(quantity.replaceAll("[^0-9.]", ""));
        } catch (Exception ignored) {
        }
        sheet.etQuantity.setText(String.valueOf(qty));

        // Set category in spinner
        String[] categories = {"Dairy", "Produce", "Grains", "Proteins", "Snacks",
                "Beverages", "Spices", "Frozen", "Other"};
        sheet.spinnerCategory.setAdapter(new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, categories));
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(category)) {
                sheet.spinnerCategory.setSelection(i);
                break;
            }
        }

        sheet.spinnerUnit.setAdapter(new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item,
                new String[]{"pcs", "kg", "g", "L", "ml", "pack", "dozen", "bunch"}));

        sheet.btnSave.setOnClickListener(v -> {
            String itemName = sheet.etName.getText().toString().trim();
            if (itemName.isEmpty()) {
                sheet.tilName.setError("Required");
                return;
            }
            PantryItem item = new PantryItem();
            item.name = itemName;
            item.category = sheet.spinnerCategory.getSelectedItem().toString();
            item.unit = sheet.spinnerUnit.getSelectedItem().toString();
            try {
                item.quantity = Double.parseDouble(sheet.etQuantity.getText().toString());
            } catch (Exception e) {
                item.quantity = 1;
            }
            item.minThreshold = 1;
            viewModel.addItem(item);
            dialog.dismiss();
            Navigation.findNavController(requireView()).popBackStack();
        });

        sheet.btnCancel.setOnClickListener(v -> {
            analyzing.set(true);
            binding.tvStatus.setText("Point at a barcode");
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> {
            if (analyzing.get()) binding.tvStatus.setText("Point at a barcode");
        });

        dialog.show();
    }

    private String mapCategory(String tags) {
        if (tags.contains("dairy")) return "Dairy";
        if (tags.contains("beverage")) return "Beverages";
        if (tags.contains("snack")) return "Snacks";
        if (tags.contains("grain") || tags.contains("bread")) return "Grains";
        if (tags.contains("frozen")) return "Frozen";
        return "Other";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        binding = null;
    }
}
