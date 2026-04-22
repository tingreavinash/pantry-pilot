package com.pantrypilot.ui.shopping;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.layout.PaddingValues;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.State;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.hapticfeedback.HapticFeedback;
import androidx.compose.ui.hapticfeedback.HapticFeedbackType;
import androidx.compose.ui.platform.LocalHapticFeedback;
import androidx.compose.ui.text.style.TextDecoration;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.pantrypilot.data.firebase.PantryRepository;
import com.pantrypilot.data.firebase.ShoppingRepository;
import com.pantrypilot.data.model.Member;
import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.data.model.ShoppingItem;
import com.pantrypilot.ui.common.Components;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

// ─── ViewModel ────────────────────────────────────────────────────────────────
@HiltViewModel
class ShoppingViewModel extends ViewModel {

    public final MutableLiveData<List<ShoppingItem>> shoppingItems = new MutableLiveData<>(List.of());
    public final MutableLiveData<List<PantryItem>> pantryItems = new MutableLiveData<>(List.of());
    public final MutableLiveData<List<Member>> members = new MutableLiveData<>(List.of());
    private final ShoppingRepository shoppingRepo;
    private final PantryRepository pantryRepo;
    private final FirebaseAuth auth;

    @Inject
    ShoppingViewModel(ShoppingRepository sr, PantryRepository pr, FirebaseAuth auth) {
        this.shoppingRepo = sr;
        this.pantryRepo = pr;
        this.auth = auth;
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (!uid.isEmpty()) {
            shoppingRepo.subscribeShoppingItems(uid, shoppingItems);
            pantryRepo.subscribePantryItems(uid, pantryItems);
        }
    }

    public void toggleBought(ShoppingItem item) {
        String uid = auth.getCurrentUser().getUid();
        shoppingRepo.setBought(uid, item.id, !item.bought);
    }

    public void addItem(ShoppingItem item) {
        shoppingRepo.addItem(auth.getCurrentUser().getUid(), item);
    }

    public void deleteItem(ShoppingItem item) {
        shoppingRepo.deleteItem(auth.getCurrentUser().getUid(), item.id);
    }

    public void clearBought() {
        List<ShoppingItem> items = shoppingItems.getValue();
        if (items != null) shoppingRepo.clearBoughtItems(auth.getCurrentUser().getUid(), items);
    }

    public void autoPopulate() {
        String uid = auth.getCurrentUser().getUid();
        List<PantryItem> pantry = pantryItems.getValue();
        List<ShoppingItem> shopping = shoppingItems.getValue();
        if (pantry != null && shopping != null)
            shoppingRepo.autoPopulate(uid, pantry, shopping);
    }

    public double runningTotal() {
        List<ShoppingItem> items = shoppingItems.getValue();
        if (items == null) return 0;
        return items.stream().filter(i -> !i.bought).mapToDouble(i -> i.estimatedCost).sum();
    }

    public void assignMember(ShoppingItem item, String memberName) {
        shoppingRepo.assignMember(auth.getCurrentUser().getUid(), item.id, memberName);
    }

    @Override
    protected void onCleared() {
        shoppingRepo.removeListener();
        pantryRepo.removeListener();
        super.onCleared();
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────
public class ShoppingScreen {

    @Composable
    public static void ShoppingScreen() {
        ShoppingViewModel vm = hiltViewModel();
        State<List<ShoppingItem>> items = vm.shoppingItems.observeAsState(List.of());
        HapticFeedback haptic = LocalHapticFeedback.current;

        MutableState<Boolean> showAddSheet = remember {
            mutableStateOf(false)
        }
        MutableState<ShoppingItem> assignTarget = remember {
            mutableStateOf(null)
        }

        Map<String, List<ShoppingItem>> grouped = items.getValue().stream()
                .collect(Collectors.groupingBy(i -> i.category));

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = {Text("Shopping List")},
                                actions = {
                                        Components.RunningTotal(vm.runningTotal());
        Spacer(Modifier.width(8.dp));
        IconButton(onClick = vm::clearBought) {
            Icon(Icons.Filled.DeleteSweep, "Clear bought");
        }
                            }
                    )
                },
        floatingActionButton = {
                FloatingActionButton(
                        onClick = () -> showAddSheet.setValue(true),
                        containerColor = MaterialTheme.colorScheme.secondary
                ){Icon(Icons.Filled.Add, "Add"); }
                }
        ){
            padding ->
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Auto-populate button
                OutlinedButton(
                        onClick = vm::autoPopulate,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp));
                    Spacer(Modifier.width(6.dp));
                    Text("Auto-populate from pantry");
                }

                if (items.getValue().isEmpty()) {
                    Components.EmptyState("🛒", "Your list is empty",
                            "Add an item", () -> showAddSheet.setValue(true));
                } else {
                    LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (Map.Entry<String, List<ShoppingItem>> entry : grouped.entrySet()) {
                            stickyHeader {
                                Components.SectionHeader(entry.getKey());
                            }
                            items(entry.getValue(), key = {it.id}) {
                                item ->
                                        ShoppingItemRow(
                                                item = item,
                                                onToggle = () -> {
                                                    vm.toggleBought(item);
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress);
                                                },
                                                onLongPress = () -> assignTarget.setValue(item)
                                        );
                            }
                        }
                    }
                }
            }
        }

        // Add item sheet
        if (showAddSheet.getValue()) {
            AddShoppingItemSheet(
                    onDismiss = () -> showAddSheet.setValue(false),
                    onSave = item -> {
                        vm.addItem(item);
                        showAddSheet.setValue(false);
                    }
            );
        }

        // Assign member sheet
        if (assignTarget.getValue() != null) {
            AssignMemberSheet(
                    members = vm.members.observeAsState(List.of()).getValue(),
                    onDismiss = () -> assignTarget.setValue(null),
                    onAssign = name -> {
                        vm.assignMember(assignTarget.getValue(), name);
                        assignTarget.setValue(null);
                    }
            );
        }
    }

    @Composable
    private static void ShoppingItemRow(ShoppingItem item, Runnable onToggle,
                                        Runnable onLongPress) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                onClick = onLongPress::run
        ) {
            Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(checked = item.bought, onCheckedChange = {_ -> onToggle.run()});
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = item.bought ? TextDecoration.LineThrough : TextDecoration.None,
                            color = item.bought ? MaterialTheme.colorScheme.onSurfaceVariant
                                    : MaterialTheme.colorScheme.onSurface
                    );
                    if (item.assignedTo != null && !item.assignedTo.isEmpty()) {
                        Text(item.assignedTo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary);
                    }
                }
                if (item.estimatedCost > 0) {
                    Text("₹" + String.format("%.0f", item.estimatedCost),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant);
                }
            }
        }
    }

    @Composable
    private static void AddShoppingItemSheet(Runnable onDismiss,
                                             java.util.function.Consumer<ShoppingItem> onSave) {
        MutableState<String> name = remember {
            mutableStateOf("")
        }
        MutableState<String> category = remember {
            mutableStateOf("Produce")
        }
        MutableState<String> quantity = remember {
            mutableStateOf("1")
        }
        MutableState<String> cost = remember {
            mutableStateOf("0")
        }

        ModalBottomSheet(onDismissRequest = onDismiss::run,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text("Add to Shopping List", style = MaterialTheme.typography.titleLarge);
                Spacer(Modifier.height(16.dp));
                OutlinedTextField(value = name.getValue(), onValueChange = v -> name.setValue(v),
                        label = {Text("Item name")}, modifier = Modifier.fillMaxWidth(), singleLine = true);
                Spacer(Modifier.height(8.dp));
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = quantity.getValue(), onValueChange = v -> quantity.setValue(v),
                            label = {Text("Qty")}, modifier = Modifier.weight(1f), singleLine = true);
                    OutlinedTextField(value = cost.getValue(), onValueChange = v -> cost.setValue(v),
                            label = {Text("Est. cost ₹")}, modifier = Modifier.weight(1f), singleLine = true);
                }
                Spacer(Modifier.height(20.dp));
                Button(onClick = () -> {
                    ShoppingItem item = new ShoppingItem();
                    item.name = name.getValue();
                    item.category = category.getValue();
                    item.quantity = Double.parseDouble(quantity.getValue().isEmpty() ? "1" : quantity.getValue());
                    item.estimatedCost = Double.parseDouble(cost.getValue().isEmpty() ? "0" : cost.getValue());
                    item.unit = "pcs";
                    onSave.accept(item);
                }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Add to List");
                }
                Spacer(Modifier.height(32.dp));
            }
        }
    }

    @Composable
    private static void AssignMemberSheet(List<Member> members, Runnable onDismiss,
                                          java.util.function.Consumer<String> onAssign) {
        ModalBottomSheet(onDismissRequest = onDismiss::run) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Assign to member", style = MaterialTheme.typography.titleMedium);
                Spacer(Modifier.height(12.dp));
                members.forEach(m -> {
                    ListItem(
                            headlineContent = {Text(m.name)},
                            leadingContent = {Text(m.avatarEmoji, fontSize = 28.sp)},
                            modifier = Modifier.clickable(() -> onAssign.accept(m.name))
                    );
                });
                Spacer(Modifier.height(24.dp));
            }
        }
    }
}
