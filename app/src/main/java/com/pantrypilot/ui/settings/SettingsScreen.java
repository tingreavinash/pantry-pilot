package com.pantrypilot.ui.settings;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.layout.PaddingValues;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.ButtonDefaults;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.State;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.platform.LocalContext;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pantrypilot.BuildConfig;
import com.pantrypilot.data.local.AppDatabase;
import com.pantrypilot.data.local.GroceryStoreEntity;
import com.pantrypilot.workers.ShoppingReminderWorker;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

// ─── ViewModel ────────────────────────────────────────────────────────────────
@HiltViewModel
class SettingsViewModel extends ViewModel {

    public final MutableLiveData<String> householdName = new MutableLiveData<>("");
    public final MutableLiveData<List<GroceryStoreEntity>> stores = new MutableLiveData<>(List.of());
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final AppDatabase roomDb;

    @Inject
    SettingsViewModel(FirebaseAuth auth, FirebaseFirestore db, AppDatabase roomDb) {
        this.auth = auth;
        this.db = db;
        this.roomDb = roomDb;
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (!uid.isEmpty()) {
            db.collection("households").document(uid).get()
                    .addOnSuccessListener(snap -> {
                        String name = snap.getString("householdName");
                        if (name != null) householdName.postValue(name);
                    });
        }
        roomDb.groceryStoreDao().getAllStores().observeForever(s -> stores.postValue(s));
    }

    public void saveHouseholdName(String name) {
        String uid = auth.getCurrentUser().getUid();
        db.collection("households").document(uid).update("householdName", name);
        householdName.setValue(name);
    }

    public void addStore(GroceryStoreEntity store) {
        new Thread(() -> roomDb.groceryStoreDao().insert(store)).start();
    }

    public void deleteStore(GroceryStoreEntity store) {
        new Thread(() -> roomDb.groceryStoreDao().deleteById(store.id)).start();
    }

    public void signOut(Runnable onDone) {
        auth.signOut();
        onDone.run();
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────
public class SettingsScreen {

    @Composable
    public static void SettingsScreen(androidx.navigation.NavController navController) {
        SettingsViewModel vm = hiltViewModel();
        android.content.Context context = LocalContext.current;
        State<String> householdName = vm.householdName.observeAsState("");
        State<List<GroceryStoreEntity>> stores = vm.stores.observeAsState(List.of());

        MutableState<String> editName = remember {
            mutableStateOf(householdName.getValue())
        }
        MutableState<Boolean> showAddStore = remember {
            mutableStateOf(false)
        }

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = {Text("Settings"); },
        navigationIcon = {
                IconButton(onClick = navController::popBackStack){
                Icon(Icons.Filled.ArrowBack, "Back");
                                }
                            }
                    )
                }
        ){
            padding ->
                    LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                // Household name
                item {
                    Text("Household", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary);
                    Spacer(Modifier.height(6.dp));
                    Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                                value = editName.getValue(),
                                onValueChange = v -> editName.setValue(v),
                                label = {Text("Household name"); },
                        singleLine = true,
                                modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = () -> vm.saveHouseholdName(editName.getValue())) {
                            Icon(Icons.Filled.Save, "Save");
                        }
                    }
                }

                // Shopping reminder
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp));
                    Text("Shopping Reminder", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary);
                    Spacer(Modifier.height(6.dp));
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    int savedDay = prefs.getInt(ShoppingReminderWorker.KEY_DAY, 6);  // Saturday
                    int savedHour = prefs.getInt(ShoppingReminderWorker.KEY_HOUR, 9);
                    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
                    Text("Reminder day: " + days.get(savedDay),
                            style = MaterialTheme.typography.bodyMedium);
                    Text("Time: " + String.format("%02d:00", savedHour),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant);
                }

                // Grocery stores
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp));
                    Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                        Text("My Grocery Stores",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary);
                        IconButton(onClick = () -> showAddStore.setValue(true)) {
                            Icon(Icons.Filled.AddLocation, "Add store");
                        }
                    }
                }

                items(stores.getValue()) {
                    store ->
                            ListItem(
                                    headlineContent = {Text(store.name); },
                    supportingContent = {Text(store.lat + ", " + store.lng,
                            style = MaterialTheme.typography.bodySmall); },
                    leadingContent = {Text("📍", fontSize = 24.sp); },
                    trailingContent = {
                            IconButton(onClick = () -> vm.deleteStore(store)){
                            Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error);
                                }
                            }
                    )
                }

                // Sign out
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp));
                    Button(
                            onClick = () -> vm.signOut(() -> navController.navigate("login") {
                        popUpTo(0) {
                            inclusive = true;
                        }
                    }),
                    modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                            )
                    ){
                        Icon(Icons.Filled.Logout, null, modifier = Modifier.size(18.dp));
                        Spacer(Modifier.width(8.dp));
                        Text("Sign Out");
                    }
                }

                // App version
                item {
                    Spacer(Modifier.height(8.dp));
                    Text("PantryPilot v" + BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth());
                }
            }
        }

        // Add store sheet
        if (showAddStore.getValue()) {
            AddStoreSheet(
                    onDismiss = () -> showAddStore.setValue(false),
                    onSave = store -> {
                        vm.addStore(store);
                        showAddStore.setValue(false);
                    }
            );
        }
    }

    @Composable
    private static void AddStoreSheet(Runnable onDismiss,
                                      java.util.function.Consumer<GroceryStoreEntity> onSave) {
        MutableState<String> name = remember {
            mutableStateOf("")
        }
        MutableState<String> lat = remember {
            mutableStateOf("")
        }
        MutableState<String> lng = remember {
            mutableStateOf("")
        }
        MutableState<String> radius = remember {
            mutableStateOf("200")
        }

        ModalBottomSheet(onDismissRequest = onDismiss::run,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text("Add Grocery Store", style = MaterialTheme.typography.titleLarge);
                Spacer(Modifier.height(16.dp));
                OutlinedTextField(value = name.getValue(), onValueChange = v -> name.setValue(v),
                        label = {Text("Store name"); },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp));
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = lat.getValue(), onValueChange = v -> lat.setValue(v),
                            label = {Text("Latitude"); },
                    modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = lng.getValue(), onValueChange = v -> lng.setValue(v),
                            label = {Text("Longitude"); },
                    modifier = Modifier.weight(1f), singleLine = true)
                }
                Spacer(Modifier.height(8.dp));
                OutlinedTextField(value = radius.getValue(), onValueChange = v -> radius.setValue(v),
                        label = {Text("Radius (metres)"); },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(6.dp));
                Text("Tip: find coordinates by searching your store in Google Maps → share → copy link",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant);
                Spacer(Modifier.height(20.dp));
                Button(onClick = () -> {
                    GroceryStoreEntity store = new GroceryStoreEntity();
                    store.name = name.getValue();
                    store.lat = Double.parseDouble(lat.getValue().isEmpty() ? "0" : lat.getValue());
                    store.lng = Double.parseDouble(lng.getValue().isEmpty() ? "0" : lng.getValue());
                    store.radiusMeters = Float.parseFloat(radius.getValue().isEmpty() ? "200" : radius.getValue());
                    onSave.accept(store);
                }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Save Store");
                }
                Spacer(Modifier.height(32.dp));
            }
        }
    }
}
