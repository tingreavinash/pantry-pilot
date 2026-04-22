package com.pantrypilot.ui.common;

import androidx.compose.runtime.Composable;
import androidx.compose.runtime.State;
import androidx.compose.ui.Modifier;
import androidx.navigation.NavHost;
import androidx.navigation.NavHostController;

import com.google.firebase.auth.FirebaseUser;
import com.pantrypilot.ui.auth.AuthViewModel;
import com.pantrypilot.ui.auth.LoginScreen;
import com.pantrypilot.ui.auth.SignUpScreen;
import com.pantrypilot.ui.dashboard.DashboardScreen;
import com.pantrypilot.ui.meals.MealPlannerScreen;
import com.pantrypilot.ui.members.MembersScreen;
import com.pantrypilot.ui.pantry.BarcodeScannerScreen;
import com.pantrypilot.ui.pantry.PantryScreen;
import com.pantrypilot.ui.pantry.ReceiptScannerScreen;
import com.pantrypilot.ui.settings.SettingsScreen;
import com.pantrypilot.ui.shopping.ShoppingScreen;

public class Navigation {

    public static final String ROUTE_LOGIN = "login";
    public static final String ROUTE_SIGNUP = "signup";
    public static final String ROUTE_DASHBOARD = "dashboard";
    public static final String ROUTE_PANTRY = "pantry";
    public static final String ROUTE_SHOPPING = "shopping";
    public static final String ROUTE_MEALS = "meals";
    public static final String ROUTE_MEMBERS = "members";
    public static final String ROUTE_SETTINGS = "settings";
    public static final String ROUTE_BARCODE = "barcode_scanner";
    public static final String ROUTE_RECEIPT = "receipt_scanner";
    private static final NavItem[] NAV_ITEMS = {
            new NavItem("🏠", "Dashboard", ROUTE_DASHBOARD),
            new NavItem("🥦", "Pantry", ROUTE_PANTRY),
            new NavItem("🛒", "Shopping", ROUTE_SHOPPING),
            new NavItem("🍽", "Meals", ROUTE_MEALS),
            new NavItem("👥", "Members", ROUTE_MEMBERS)
    };

    @Composable
    public static void AppNavigation() {
        NavHostController navController = rememberNavController();
        AuthViewModel authVM = hiltViewModel();
        State<FirebaseUser> user = authVM.currentUser.observeAsState();

        // React to sign-in / sign-out
        LaunchedEffect(user.getValue()) {
            if (user.getValue() == null) {
                navController.navigate(ROUTE_LOGIN) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true;
                    }
                }
            }
        }

        NavHost(
                navController = navController,
                startDestination = authVM.isLoggedIn() ? ROUTE_DASHBOARD : ROUTE_LOGIN,
                enterTransition = {fadeIn(animationSpec = tween(200))},
                exitTransition = {fadeOut(animationSpec = tween(200))}
        ) {
            // Auth
            composable(ROUTE_LOGIN) {
                LoginScreen.Screen(navController, authVM);
            }
            composable(ROUTE_SIGNUP) {
                SignUpScreen.Screen(navController, authVM);
            }

            // Main tabs
            composable(ROUTE_DASHBOARD) {
                MainScaffold(navController, ROUTE_DASHBOARD) {
                    DashboardScreen.Screen(navController);
                }
            }
            composable(ROUTE_PANTRY) {
                MainScaffold(navController, ROUTE_PANTRY) {
                    PantryScreen.Screen(navController);
                }
            }
            composable(ROUTE_SHOPPING) {
                MainScaffold(navController, ROUTE_SHOPPING) {
                    ShoppingScreen.Screen();
                }
            }
            composable(ROUTE_MEALS) {
                MainScaffold(navController, ROUTE_MEALS) {
                    MealPlannerScreen.Screen();
                }
            }
            composable(ROUTE_MEMBERS) {
                MainScaffold(navController, ROUTE_MEMBERS) {
                    MembersScreen.Screen();
                }
            }

            // Secondary screens (no bottom nav)
            composable(ROUTE_SETTINGS) {
                SettingsScreen.Screen(navController);
            }
            composable(ROUTE_BARCODE) {
                BarcodeScannerScreen.Screen(navController);
            }
            composable(ROUTE_RECEIPT) {
                ReceiptScannerScreen.Screen(navController);
            }
        }
    }

    @Composable
    private static void MainScaffold(NavHostController nav, String current, Runnable content) {
        Scaffold(
                bottomBar = {BottomNav(nav, current); }
        ){
            padding ->
                    Box(modifier = Modifier.padding(padding)) {
                content.run();
            }
        }
    }

    @Composable
    private static void BottomNav(NavHostController nav, String current) {
        NavigationBar {
            for (NavItem item : NAV_ITEMS) {
                NavigationBarItem(
                        selected = current.equals(item.route()),
                        onClick = () -> nav.navigate(item.route()) {
                    popUpTo(nav.graph.findStartDestination().id) {
                        saveState = true;
                    }
                    launchSingleTop = true;
                    restoreState = true;
                },
                label = {Text(item.label())},
                        icon = {Text(item.emoji(), fontSize = 20.sp)}
                )
            }
        }
    }

    // ── Bottom navigation bar ─────────────────────────────────────────────────
    private record NavItem(String emoji, String label, String route) {
    }
}
