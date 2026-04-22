package com.pantrypilot.ui.pantry;

import android.content.Context;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.material3.SnackbarHostState;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.MutableState;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.platform.LocalContext;
import androidx.navigation.NavController;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.List;

public class ReceiptScannerScreen {

    @Composable
    public static void Screen(NavController navController) {
        PantryViewModel vm = hiltViewModel();
        Context context = LocalContext.current;

        MutableState<List<ReceiptParser.ParsedItem>> parsedItems = remember {
            mutableStateOf(null)
        }
        MutableState<Boolean> processing = remember {
            mutableStateOf(false)
        }
        SnackbarHostState snackbar = remember {
            new SnackbarHostState()
        }

        // Gallery picker — no READ_EXTERNAL_STORAGE needed on API 30+
        ManagedActivityResultLauncher<String, Uri> picker =
                rememberLauncherForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {
                            if (uri == null) return;
                            processing.setValue(true);
                            try {
                                InputImage image = InputImage.fromFilePath(context, uri);
                                TextRecognition
                                        .getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                        .process(image)
                                        .addOnSuccessListener(result -> {
                                            List<String> pantryNames = new java.util.ArrayList<>();
                                            if (vm.pantryItems.getValue() != null) {
                                                for (var p : vm.pantryItems.getValue())
                                                    pantryNames.add(p.name);
                                            }
                                            List<ReceiptParser.ParsedItem> items =
                                                    ReceiptParser.parse(result.getText(), pantryNames);
                                            processing.setValue(false);
                                            parsedItems.setValue(items);
                                            if (items.size() < 3) {
                                                // Non-blocking: show snackbar
                                                new Thread(() -> {
                                                    try {
                                                        snackbar.showSnackbar(
                                                                "Receipt unclear — try better lighting or a flatter surface");
                                                    } catch (Exception ignored) {
                                                    }
                                                }).start();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            processing.setValue(false);
                                            parsedItems.setValue(List.of());
                                        });
                            } catch (Exception e) {
                                processing.setValue(false);
                            }
                        }
                );

        Scaffold(
                snackbarHost = {SnackbarHost(snackbar)},
                topBar = {
                        TopAppBar(
                                title = {Text("Scan Receipt")},
                                navigationIcon = {
                                        IconButton(onClick = () -> navController.popBackStack()){
                                        Icon(Icons.Filled.ArrowBack, "Back");
                                }
                            }
                    )
                }
        ){
            padding ->
                    Column(
                            modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(16.dp)
                    ) {
                if (processing.getValue()) {
                    // Processing state
                    Box(modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator();
                            Spacer(Modifier.height(12.dp));
                            Text("Analysing receipt…");
                        }
                    }

                } else if (parsedItems.getValue() == null) {
                    // Initial / idle state
                    Box(modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center) {
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🧾", fontSize = 56.sp);
                            Text("Scan a grocery receipt to\nauto-fill your pantry",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center);
                        }
                    }

                } else {
                    // Review screen
                    Text("Review detected items",
                            style = MaterialTheme.typography.titleMedium);
                    Spacer(Modifier.height(8.dp));

                    if (parsedItems.getValue().isEmpty()) {
                        Text("No items detected. Try picking another photo.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant);
                    } else {
                        LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(parsedItems.getValue()) {
                                item ->
                                        ReceiptItemRow(item);
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp));

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                                onClick = () -> parsedItems.setValue(null),
                                modifier = Modifier.weight(1f)
                        ) {
                            Text("Skip All")
                        }

                        Button(
                                onClick = () -> {
                                    confirmItems(vm, parsedItems.getValue());
                                    navController.popBackStack();
                                },
                                modifier = Modifier.weight(1f),
                                enabled = parsedItems.getValue().stream().anyMatch(i -> i.selected)
                        ) {
                            Text("Confirm")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp));

                Button(
                        onClick = () -> picker.launch("image/*"),
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Image, null, modifier = Modifier.size(18.dp));
                    Spacer(Modifier.width(8.dp));
                    Text(parsedItems.getValue() == null ? "Pick receipt photo" : "Pick another photo");
                }
            }
        }
    }

    @Composable
    private static void ReceiptItemRow(ReceiptParser.ParsedItem item) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                        checked = item.selected,
                        onCheckedChange = v -> item.selected = v);
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.bodyMedium);
                    if (item.isUpdate) {
                        Text("Update: " + item.matchedPantryItem,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary);
                    } else {
                        Text("Add new",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary);
                    }
                }
                Text("×" + String.format("%.0f", item.quantity),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant);
                Text("₹" + String.format("%.0f", item.price),
                        style = MaterialTheme.typography.bodySmall);
            }
        }
    }

    /**
     * Write confirmed items to Firestore via PantryViewModel
     */
    private static void confirmItems(PantryViewModel vm,
                                     List<ReceiptParser.ParsedItem> items) {
        if (items == null) return;
        for (ReceiptParser.ParsedItem parsed : items) {
            if (!parsed.selected) continue;
            com.pantrypilot.data.model.PantryItem item =
                    new com.pantrypilot.data.model.PantryItem();
            item.name = parsed.name;
            item.quantity = parsed.quantity;
            item.category = "Other";
            item.unit = "pcs";
            item.minThreshold = 1;
            vm.addItem(item);
        }
    }
}
