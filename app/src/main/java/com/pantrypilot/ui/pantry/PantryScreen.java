package com.pantrypilot.ui.pantry;

import androidx.compose.animation.core.InfiniteTransition;
import androidx.compose.animation.core.RepeatMode;
import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.layout.PaddingValues;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.ExposedDropdownMenuDefaults;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.material3.SnackbarHostState;
import androidx.compose.material3.SuggestionChipDefaults;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.State;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Color;
import androidx.compose.ui.hapticfeedback.HapticFeedback;
import androidx.compose.ui.hapticfeedback.HapticFeedbackType;
import androidx.compose.ui.platform.LocalHapticFeedback;
import androidx.navigation.NavController;

import com.google.firebase.auth.FirebaseAuth;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.ui.common.Navigation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PantryScreen {

    private static final List<String> CATEGORIES = Arrays.asList(
            "All", "Dairy", "Produce", "Grains", "Proteins", "Snacks", "Beverages", "Spices", "Frozen", "Other"
    );

    private static final List<String> UNITS = Arrays.asList(
            "pcs", "kg", "g", "L", "ml", "pack", "dozen", "bunch"
    );

    @Composable
    public static void PantryScreen(NavController navController) {
        PantryViewModel vm = hiltViewModel();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // Init on first compose
        LaunchedEffect(Unit) {
            if (auth.getCurrentUser() != null) vm.init(auth.getCurrentUser().getUid());
        }

        State<List<PantryItem>> allItems = vm.pantryItems.observeAsState(List.of());
        State<String> searchQuery = vm.searchQuery.observeAsState("");
        State<String> selectedCat = vm.selectedCategory.observeAsState("All");
        State<String> toastMsg = vm.toastMessage.observeAsState();

        MutableState<Boolean> showAddSheet = remember {
            mutableStateOf(false)
        }
        MutableState<PantryItem> editingItem = remember {
            mutableStateOf(null)
        }
        MutableState<PantryItem> deletedItem = remember {
            mutableStateOf(null)
        }
        SnackbarHostState snackbarHost = remember {
            new SnackbarHostState()
        }
        HapticFeedback haptic = LocalHapticFeedback.current;

        // Toast messages
        LaunchedEffect(toastMsg.getValue()) {
            if (toastMsg.getValue() != null) snackbarHost.showSnackbar(toastMsg.getValue());
        }

        // Filter items
        List<PantryItem> filtered = allItems.getValue().stream()
                .filter(i -> (selectedCat.getValue().equals("All") || i.category.equals(selectedCat.getValue()))
                        && i.name.toLowerCase().contains(searchQuery.getValue().toLowerCase()))
                .collect(Collectors.toList());

        // Group by category
        Map<String, List<PantryItem>> grouped = filtered.stream()
                .collect(Collectors.groupingBy(i -> i.category));

        Scaffold(
                snackbarHost = {SnackbarHost(snackbarHost)},
                topBar = {
                        TopAppBar(
                                title = {Text("Pantry")},
                                actions = {
                                        IconButton(onClick = () -> navController.navigate(Navigation.ROUTE_BARCODE)){
                                        Icon(Icons.Filled.QrCodeScanner, "Scan barcode");
                                }
                            }
                    )
                },
        floatingActionButton = {
                ExpandableFab(
                        onAddManually = () -> {
                            editingItem.setValue(null);
                            showAddSheet.setValue(true);
                        },
                        onScanBarcode = () -> navController.navigate(Navigation.ROUTE_BARCODE)
                );
                }
        ){
            padding ->
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                // Search bar
                OutlinedTextField(
                        value = searchQuery.getValue(),
                        onValueChange = v -> vm.searchQuery.setValue(v),
                        placeholder = {Text("Search items…")},
                        leadingIcon = {Icon(Icons.Filled.Search, null)},
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                );

                // Category filter chips
                LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(CATEGORIES) {
                        cat ->
                                FilterChip(
                                        selected = selectedCat.getValue().equals(cat),
                                        onClick = () -> vm.selectedCategory.setValue(cat),
                                        label = {Text(cat)}
                                );
                    }
                }

                // Items list
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (Map.Entry<String, List<PantryItem>> entry : grouped.entrySet()) {
                        stickyHeader {
                            Text(
                                    entry.getKey().toUpperCase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.background)
                                            .padding(vertical = 6.dp)
                            );
                        }
                        items(entry.getValue(), key = {it.id}) {
                            item ->
                                    SwipeablePantryItem(
                                            item = item,
                                            onDelete = () -> {
                                                deletedItem.setValue(item);
                                                vm.deleteItem(item);
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress);
                                                // Undo snackbar
                                                launchSnackbarUndo(snackbarHost, "Deleted " + item.name, vm, item, auth);
                                            },
                                            onEdit = () -> {
                                                editingItem.setValue(item);
                                                showAddSheet.setValue(true);
                                            }
                                    );
                        }
                    }
                }
            }
        }

        // Add / Edit bottom sheet
        if (showAddSheet.getValue()) {
            AddEditPantryBottomSheet(
                    item = editingItem.getValue(),
                    onDismiss = () -> showAddSheet.setValue(false),
                    onSave = item -> {
                        if (item.id == null || item.id.isEmpty()) vm.addItem(item);
                        else vm.updateItem(item);
                        showAddSheet.setValue(false);
                    }
            );
        }
    }

    @Composable
    private static void SwipeablePantryItem(PantryItem item, Runnable onDelete, Runnable onEdit) {
        SwipeToDismissBoxState state = rememberSwipeToDismissBoxState(
                confirmValueChange = v -> {
                    if (v == SwipeToDismissBoxValue.EndToStart) {
                        onDelete.run();
                        return true;
                    }
                    return false;
                }
        );

        SwipeToDismissBox(
                state = state,
                backgroundContent = {
                        Box(
                                modifier = Modifier.fillMaxSize()
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(end = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                        ){
                        Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error);
                    }
                }
        ){
            PantryItemCard(item = item, onEdit = onEdit);
        }
    }

    @Composable
    private static void PantryItemCard(PantryItem item, Runnable onEdit) {
        // Pulsing red border for OUT stock
        boolean isOut = item.getStockStatus() == PantryItem.StockStatus.OUT;
        InfiniteTransition infiniteTransition = rememberInfiniteTransition();
        float alpha = isOut ? infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
        ).getValue() : 1f;

        Card(
                onClick = onEdit::run,
                modifier = Modifier.fillMaxWidth(),
                border = isOut ? BorderStroke(1.5f.dp, MaterialTheme.colorScheme.error.copy(alpha = alpha)) : null
        ) {
            Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge);
                    Text(item.quantity + " " + item.unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant);
                    // Expiry badge
                    long days = item.daysUntilExpiry();
                    if (days >= 0 && days <= 5) {
                        Spacer(Modifier.height(4.dp));
                        SuggestionChip(
                                onClick = {},
                                label = {Text("Expires in " + days + " days", fontSize = 11.sp)},
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = new Color(0xFFFEF3C7)
                                )
                        );
                    }
                }
                StockChip(item.getStockStatus());
            }
        }
    }

    @Composable
    private static void AddEditPantryBottomSheet(
            PantryItem item,
            Runnable onDismiss,
            java.util.function.Consumer<PantryItem> onSave) {

        boolean isEdit = item != null;

        MutableState<String> name = remember {
            mutableStateOf(isEdit ? item.name : "")
        }
        MutableState<String> category = remember {
            mutableStateOf(isEdit ? item.category : "Produce")
        }
        MutableState<String> quantity = remember {
            mutableStateOf(isEdit ? String.valueOf(item.quantity) : "")
        }
        MutableState<String> unit = remember {
            mutableStateOf(isEdit ? item.unit : "pcs")
        }
        MutableState<String> minThreshold = remember {
            mutableStateOf(isEdit ? String.valueOf(item.minThreshold) : "1")
        }
        MutableState<Boolean> catExpanded = remember {
            mutableStateOf(false)
        }
        MutableState<Boolean> unitExpanded = remember {
            mutableStateOf(false)
        }

        ModalBottomSheet(
                onDismissRequest = onDismiss::run,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text(isEdit ? "Edit Item" : "Add to Pantry",
                        style = MaterialTheme.typography.titleLarge);
                Spacer(Modifier.height(16.dp));

                OutlinedTextField(value = name.getValue(), onValueChange = v -> name.setValue(v),
                        label = {Text("Item name")}, modifier = Modifier.fillMaxWidth(), singleLine = true);
                Spacer(Modifier.height(8.dp));

                // Category dropdown
                ExposedDropdownMenuBox(
                        expanded = catExpanded.getValue(),
                        onExpandedChange = v -> catExpanded.setValue(v)
                ) {
                    OutlinedTextField(value = category.getValue(), onValueChange = {},
                            label = {Text("Category")}, readOnly = true,
                            trailingIcon = {ExposedDropdownMenuDefaults.TrailingIcon(catExpanded.getValue()); },
                    modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = catExpanded.getValue(),
                            onDismissRequest = () -> catExpanded.setValue(false)) {
                        CATEGORIES.stream().filter(c -> !c.equals("All")).forEach(cat -> {
                            DropdownMenuItem(text = {Text(cat)}, onClick = () -> {
                                category.setValue(cat);
                                catExpanded.setValue(false);
                            });
                        });
                    }
                }
                Spacer(Modifier.height(8.dp));

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = quantity.getValue(),
                            onValueChange = v -> quantity.setValue(v),
                            label = {Text("Quantity")},
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f), singleLine = true);

                    ExposedDropdownMenuBox(
                            expanded = unitExpanded.getValue(),
                            onExpandedChange = v -> unitExpanded.setValue(v),
                            modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(value = unit.getValue(), onValueChange = {},
                                label = {Text("Unit")}, readOnly = true,
                                trailingIcon = {ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded.getValue()); },
                        modifier = Modifier.fillMaxWidth().menuAnchor())
                        ExposedDropdownMenu(expanded = unitExpanded.getValue(),
                                onDismissRequest = () -> unitExpanded.setValue(false)) {
                            UNITS.forEach(u -> {
                                DropdownMenuItem(text = {Text(u)}, onClick = () -> {
                                    unit.setValue(u);
                                    unitExpanded.setValue(false);
                                });
                            });
                        }
                    }
                }
                Spacer(Modifier.height(8.dp));

                OutlinedTextField(value = minThreshold.getValue(),
                        onValueChange = v -> minThreshold.setValue(v),
                        label = {Text("Min threshold (alert below this)")},
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), singleLine = true);

                Spacer(Modifier.height(20.dp));

                Button(
                        onClick = () -> {
                            PantryItem result = isEdit ? item : new PantryItem();
                            result.name = name.getValue();
                            result.category = category.getValue();
                            result.quantity = Double.parseDouble(quantity.getValue().isEmpty() ? "0" : quantity.getValue());
                            result.unit = unit.getValue();
                            result.minThreshold = Double.parseDouble(minThreshold.getValue().isEmpty() ? "1" : minThreshold.getValue());
                            onSave.accept(result);
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(isEdit ? "Save Changes" : "Add to Pantry");
                }

                Spacer(Modifier.height(32.dp));
            }
        }
    }

    @Composable
    private static void ExpandableFab(Runnable onAddManually, Runnable onScanBarcode) {
        MutableState<Boolean> expanded = remember {
            mutableStateOf(false)
        }
        Column(horizontalAlignment = Alignment.End) {
            AnimatedVisibility(visible = expanded.getValue(), enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically())
            {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)) {
                SmallFloatingActionButton(onClick = () -> {
                            onScanBarcode.run();
                            expanded.setValue(false);
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Icon(Icons.Filled.QrCodeScanner, "Scan barcode");
                }
                SmallFloatingActionButton(onClick = () -> {
                            onAddManually.run();
                            expanded.setValue(false);
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Icon(Icons.Filled.Edit, "Add manually");
                }
            }
            }
            FloatingActionButton(
                    onClick = () -> expanded.setValue(!expanded.getValue()),
                    containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(expanded.getValue() ? Icons.Filled.Close : Icons.Filled.Add, "FAB");
            }
        }
    }

    private static void launchSnackbarUndo(SnackbarHostState host, String message,
                                           PantryViewModel vm, PantryItem item,
                                           FirebaseAuth auth) {
        // Fire-and-forget coroutine via Compose's LaunchedEffect equivalent
        // In practice, call this inside a coroutineScope obtained via rememberCoroutineScope()
    }
}
