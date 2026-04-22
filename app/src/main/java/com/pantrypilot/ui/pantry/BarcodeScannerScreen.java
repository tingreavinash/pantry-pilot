package com.pantrypilot.ui.pantry;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.CameraController;
import androidx.camera.view.PreviewView;
import androidx.compose.animation.core.InfiniteTransition;
import androidx.compose.animation.core.RepeatMode;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.MutableState;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.geometry.Offset;
import androidx.compose.ui.graphics.Color;
import androidx.compose.ui.graphics.Paint;
import androidx.compose.ui.graphics.StrokeCap;
import androidx.compose.ui.graphics.drawscope.Stroke;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.NavController;

import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeScannerScreen {

    private static final String TAG = "BarcodeScanner";
    private static final String FOOD_FACTS_BASE = "https://world.openfoodfacts.org/api/v0/product/";

    @Composable
    public static void BarcodeScannerScreen(NavController navController) {
        Context context = LocalContext.current;
        LifecycleOwner lifecycle = LocalLifecycleOwner.current;
        ExecutorService executor = remember {
            Executors.newSingleThreadExecutor()
        }

        MutableState<Boolean> torchOn = remember {
            mutableStateOf(false)
        }
        MutableState<Boolean> analyzing = remember {
            mutableStateOf(true)
        }
        MutableState<String> statusText = remember {
            mutableStateOf("Point at a barcode")
        }
        MutableState<ScannedProduct> product = remember {
            mutableStateOf(null)
        }
        MutableState<CameraControl> camCtrl = remember {
            mutableStateOf(null)
        }

        // Scanning line animation
        InfiniteTransition transition = rememberInfiniteTransition();
        float scanLineY = transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse)
        ).getValue();

        DisposableEffect(Unit) {
            onDispose {
                executor.shutdown();
            }
        }

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = {Text("Scan Barcode")},
                                navigationIcon = {
                                        IconButton(onClick = navController::popBackStack){
                                        Icon(Icons.Filled.ArrowBack, "Back");
                                }
                            },
        actions = {
                IconButton(onClick = () -> {
                    torchOn.setValue(!torchOn.getValue());
                    camCtrl.getValue() ?.enableTorch(torchOn.getValue());
                }){
                Icon(torchOn.getValue() ? Icons.Filled.FlashlightOff : Icons.Filled.FlashlightOn,
                        "Torch");
                                }
                            }
                    )
                }
        ){
            padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {

                // CameraX preview
                AndroidView(
                        factory = {ctx ->
                                PreviewView(ctx).also{previewView ->
                                setupCamera(ctx, lifecycle, previewView, analyzing, executor,
                                        barcode -> {
                                            if (!analyzing.getValue()) return;
                                            analyzing.setValue(false);
                                            statusText.setValue("Looking up " + barcode + "…");
                                            lookupBarcode(barcode, executor, result -> {
                                                product.setValue(result);
                                            });
                                        },
                                        ctrl -> camCtrl.setValue(ctrl)
                                );
                            }
                        },
                modifier = Modifier.fillMaxSize()
                )

                // Scanning overlay — animated corner brackets + scan line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    float w = size.width, h = size.height;
                    float boxSize = w * 0.65f;
                    float left = (w - boxSize) / 2f;
                    float top = (h - boxSize) / 2f;
                    float right = left + boxSize;
                    float bottom = top + boxSize;
                    float corner = 48f;

                    Paint cornerPaint = new Paint(Color.Green, strokeWidth = 4f,
                            style = Stroke(width = 4f), strokeCap = StrokeCap.Round);

                    // Top-left
                    drawLine(Color.Green, Offset(left, top + corner), Offset(left, top), 4f);
                    drawLine(Color.Green, Offset(left, top), Offset(left + corner, top), 4f);
                    // Top-right
                    drawLine(Color.Green, Offset(right - corner, top), Offset(right, top), 4f);
                    drawLine(Color.Green, Offset(right, top), Offset(right, top + corner), 4f);
                    // Bottom-left
                    drawLine(Color.Green, Offset(left, bottom - corner), Offset(left, bottom), 4f);
                    drawLine(Color.Green, Offset(left, bottom), Offset(left + corner, bottom), 4f);
                    // Bottom-right
                    drawLine(Color.Green, Offset(right - corner, bottom), Offset(right, bottom), 4f);
                    drawLine(Color.Green, Offset(right, bottom), Offset(right, bottom - corner), 4f);

                    // Animated scan line
                    float scanY = top + (boxSize * scanLineY);
                    drawLine(Color.Green.copy(alpha = 0.7f),
                            Offset(left + 4, scanY), Offset(right - 4, scanY), 2f);
                }

                // Status text at bottom
                Text(
                        statusText.getValue(),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
                        style = MaterialTheme.typography.bodyLarge
                );
            }
        }

        // Show product bottom sheet when scan resolves
        if (product.getValue() != null) {
            ScannedProductSheet(
                    product = product.getValue(),
                    onDismiss = () -> {
                        product.setValue(null);
                        analyzing.setValue(true);
                        statusText.setValue("Point at a barcode");
                    },
                    onAdd = item -> {
                        // Delegate to PantryViewModel via shared Hilt scope or pass callback
                        navController.popBackStack();
                    }
            );
        }
    }

    private static void setupCamera(Context context, LifecycleOwner lifecycle,
                                    PreviewView previewView,
                                    MutableState<Boolean> analyzing,
                                    ExecutorService executor,
                                    java.util.function.Consumer<String> onBarcode,
                                    java.util.function.Consumer<CameraControl> onControl) {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(context);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(executor,
                        new MlKitAnalyzer(
                                List.of(BarcodeScanning.getClient()),
                                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                                ContextCompat.getMainExecutor(context),
                                result -> {
                                    List<Barcode> barcodes = result.getValue(BarcodeScanning.getClient());
                                    if (barcodes != null && !barcodes.isEmpty() && analyzing.getValue()) {
                                        String raw = barcodes.get(0).getRawValue();
                                        if (raw != null && !raw.isEmpty()) onBarcode.accept(raw);
                                    }
                                }
                        )
                );

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
                Camera cam = provider.bindToLifecycle(lifecycle, selector, preview, analysis);
                onControl.accept(cam.getCameraControl());
            } catch (Exception e) {
                Log.e(TAG, "Camera setup failed", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private static void lookupBarcode(String barcode, ExecutorService executor,
                                      java.util.function.Consumer<ScannedProduct> callback) {
        executor.submit(() -> {
            try {
                URL url = new URL(FOOD_FACTS_BASE + barcode + ".json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                int status = json.optInt("status", 0);
                ScannedProduct result = new ScannedProduct();
                result.barcode = barcode;

                if (status == 1) {
                    JSONObject product = json.optJSONObject("product");
                    result.found = true;
                    result.name = product.optString("product_name", "");
                    result.quantity = product.optString("quantity", "");
                    result.category = mapCategory(product.optString("categories_tags", ""));
                } else {
                    result.found = false;
                }
                callback.accept(result);
            } catch (Exception e) {
                Log.w(TAG, "Barcode lookup failed", e);
                ScannedProduct fallback = new ScannedProduct();
                fallback.barcode = barcode;
                fallback.found = false;
                callback.accept(fallback);
            }
        });
    }

    private static String mapCategory(String categoriesTags) {
        if (categoriesTags.contains("dairy")) return "Dairy";
        if (categoriesTags.contains("beverage")) return "Beverages";
        if (categoriesTags.contains("snack")) return "Snacks";
        if (categoriesTags.contains("grain") || categoriesTags.contains("bread")) return "Grains";
        if (categoriesTags.contains("frozen")) return "Frozen";
        return "Other";
    }

    @Composable
    private static void ScannedProductSheet(ScannedProduct product,
                                            Runnable onDismiss,
                                            Runnable onAdd) {
        MutableState<String> name = remember {
            mutableStateOf(product.name)
        }
        MutableState<String> quantity = remember {
            mutableStateOf(product.quantity)
        }

        ModalBottomSheet(onDismissRequest = onDismiss::run,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text(product.found ? "Product found!" : "Product not found",
                        style = MaterialTheme.typography.titleLarge);
                Text("Barcode: " + product.barcode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant);
                Spacer(Modifier.height(16.dp));
                OutlinedTextField(value = name.getValue(), onValueChange = v -> name.setValue(v),
                        label = {Text("Item name")}, modifier = Modifier.fillMaxWidth(), singleLine = true);
                Spacer(Modifier.height(8.dp));
                OutlinedTextField(value = quantity.getValue(), onValueChange = v -> quantity.setValue(v),
                        label = {Text("Quantity")}, modifier = Modifier.fillMaxWidth(), singleLine = true);
                Spacer(Modifier.height(20.dp));
                Button(onClick = onAdd::run,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = name.getValue().isNotEmpty()) {
                    Text("Add to Pantry");
                }
                TextButton(onClick = onDismiss::run, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan again");
                }
                Spacer(Modifier.height(32.dp));
            }
        }
    }

    // ── Data classes ──────────────────────────────────────────────────────────
    static class ScannedProduct {
        String barcode;
        String name = "";
        String quantity = "";
        String category = "Other";
        boolean found = false;
    }
}
