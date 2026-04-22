package com.pantrypilot.ui.dashboard;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.layout.PaddingValues;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.CardDefaults;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.State;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Color;
import androidx.navigation.NavController;

import com.pantrypilot.data.model.PantryItem;
import com.pantrypilot.ui.common.Components;
import com.pantrypilot.ui.common.ConnectivityObserver;
import com.pantrypilot.ui.common.Navigation;

import java.util.List;

public class DashboardScreen {

    @Composable
    public static void Screen(NavController navController) {
        DashboardViewModel vm = hiltViewModel();
        ConnectivityObserver connectivity = hiltViewModel(); // injected separately in production
        State<List<PantryItem>> pantry = vm.pantryItems.observeAsState(List.of());
        State<String> householdName = vm.householdName.observeAsState("My Home");
        // Note: pass ConnectivityObserver.isOffline via shared ViewModel in production
        boolean isOffline = false; // placeholder; wire to ConnectivityObserver.isOffline

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = {Text(householdName.getValue())},
                                actions = {
                                        IconButton(
                                                onClick = () -> navController.navigate(Navigation.ROUTE_SETTINGS)
                                        ){
                                        Icon(Icons.Filled.MoreVert, "Settings");
                                }
                            }
                    )
                }
        ){
            padding ->
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Offline banner
                Components.OfflineBanner(isOffline);

                LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary cards
                    item {
                        Text("Overview",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp));
                        LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(180.dp)
                        ) {
                            item {
                                SummaryCard("Total Items",
                                        String.valueOf(vm.totalItems()),
                                        MaterialTheme.colorScheme.primaryContainer,
                                        () -> navController.navigate(Navigation.ROUTE_PANTRY));
                            }
                            item {
                                SummaryCard("Low / Out",
                                        String.valueOf(vm.lowStockCount()),
                                        new Color(0xFFFFEDD5L),
                                        () -> navController.navigate(Navigation.ROUTE_PANTRY));
                            }
                            item {
                                SummaryCard("Expiring Soon",
                                        String.valueOf(vm.expiringCount()),
                                        new Color(0xFFFEF3C7L),
                                        () -> navController.navigate(Navigation.ROUTE_PANTRY));
                            }
                            item {
                                SummaryCard("Shopping List",
                                        String.valueOf(vm.shoppingCount()),
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        () -> navController.navigate(Navigation.ROUTE_SHOPPING));
                            }
                        }
                    }

                    // Recent activity header
                    item {
                        Text("Recent activity",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp));
                    }

                    // Recent items — last 5
                    if (pantry.getValue().isEmpty()) {
                        item {
                            Components.EmptyState(
                                    "🥦", "Your pantry is empty",
                                    "Add first item",
                                    () -> navController.navigate(Navigation.ROUTE_PANTRY));
                        }
                    } else {
                        int limit = Math.min(5, pantry.getValue().size());
                        items(limit) {
                            idx ->
                                    RecentItemRow(pantry.getValue().get(idx));
                        }
                    }
                }
            }
        }
    }

    @Composable
    private static void SummaryCard(String label, String value, Color bg, Runnable onClick) {
        Card(
                onClick = onClick::run,
                colors = CardDefaults.cardColors(containerColor = bg),
                modifier = Modifier.fillMaxWidth().aspectRatio(1.2f)
        ) {
            Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant);
                Text(value,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold);
            }
        }
    }

    @Composable
    private static void RecentItemRow(PantryItem item) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(item.name, style = MaterialTheme.typography.bodyMedium);
                    Text(item.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant);
                }
                Components.StockChip(item.getStockStatus());
            }
        }
    }
}
