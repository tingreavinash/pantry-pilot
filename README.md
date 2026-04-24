# PantryPilot 🌿

Smart home grocery management — Java + XML ViewBinding + Material 3 + Firebase

---

## Tech stack

| Layer        | Technology                                    |
|--------------|-----------------------------------------------|
| Language     | Java 17                                       |
| UI           | XML layouts + ViewBinding (no Compose)        |
| Architecture | MVVM + Repository                             |
| DI           | Hilt                                          |
| Navigation   | Jetpack Navigation Component (Fragment-based) |
| Firebase     | Auth · Firestore (offline-enabled) · FCM      |
| Local DB     | Room                                          |
| Background   | WorkManager                                   |
| Camera       | CameraX + ML Kit (barcode + OCR)              |
| Location     | Play Services Location + Geofencing           |
| Build        | AGP 8.2.2 · Gradle 8.2 · JDK 17               |
| Min SDK      | 30 (Android 11)                               |

---

## First-time setup

### 1. Add google-services.json

Place your existing Firebase project's `google-services.json` at:
```
app/google-services.json
```

The app shares collections with the React web app — no schema changes needed.

### 2. Open in Android Studio

- Open the `pantrypilot/` folder as an existing project
- Android Studio will prompt to sync Gradle — accept
- JDK: use **Android Studio default JDK (17)**
- Gradle distribution: **8.2** (set in `gradle/wrapper/gradle-wrapper.properties`)

### 3. Build and run
```
./gradlew assembleDebug
```

or press **Run ▶** in Android Studio.

---

## Project structure

```
app/src/main/java/com/pantrypilot/
├── PantryPilotApp.java          Hilt Application, notification channels
├── di/
│   └── AppModule.java           Firebase, Room, Executor Hilt bindings
├── data/
│   ├── model/                   PantryItem · ShoppingItem · Meal · Member
│   ├── firebase/                AuthRepository · PantryRepository · ShoppingRepository
│   │                            MealRepository · MemberRepository
│   └── local/                   AppDatabase · GroceryStoreDao · GroceryStoreEntity
├── ui/
│   ├── MainActivity.java        Shell: BottomNav + NavHost + deep links + offline banner
│   ├── auth/                    AuthActivity · LoginFragment · SignUpFragment · AuthViewModel
│   ├── dashboard/               DashboardFragment · DashboardViewModel · RecentItemsAdapter
│   ├── pantry/                  PantryFragment · PantryViewModel · PantryAdapter
│   │                            BarcodeScannerFragment · ReceiptScannerFragment
│   │                            ReceiptParser · ReceiptItemsAdapter
│   ├── shopping/                ShoppingFragment · ShoppingViewModel · ShoppingAdapter
│   ├── meals/                   MealPlannerFragment · MealViewModel · MealDayAdapter
│   ├── members/                 MembersFragment · MembersViewModel · MembersAdapter
│   │                            EmojiGridAdapter
│   └── settings/                SettingsFragment · SettingsViewModel · StoresAdapter
├── workers/                     ExpiryCheckWorker · LowStockWorker · ShoppingReminderWorker
└── receivers/                   GeofenceBroadcastReceiver · BootReceiver
```

---

## Firestore schema (shared with web app — unchanged)

```
households/{uid}/
  pantryItems/   → name, category, quantity, unit, minThreshold, expiryDate, createdAt
  shoppingList/  → name, category, quantity, unit, estimatedCost, bought, assignedTo, createdAt
  meals/         → day, mealName, ingredients[]
  members/       → name, avatarEmoji
  fcmTokens/     → token, platform:"android", updatedAt
```

---

## Key implementation notes

### MVVM + LiveData pattern

```java
// Repository posts to MutableLiveData via Firestore snapshot listener
repo.subscribe(uid, pantryItems);   // addSnapshotListener → postValue()

// Fragment observes
viewModel.pantryItems.observe(getViewLifecycleOwner(), items -> {
    adapter.updateItems(items);
});
```

### Offline mode

Firestore persistence is enabled in `AppModule.java`:

```java
new FirebaseFirestoreSettings.Builder()
    .setPersistenceEnabled(true)
    .setCacheSizeBytes(CACHE_SIZE_UNLIMITED)
    .build();
```

The amber offline banner in `activity_main.xml` is shown/hidden by
`ConnectivityManager.NetworkCallback` in `MainActivity`.

### Background notifications (no Cloud Functions needed)

Three `WorkManager` periodic workers run on-device:

- `ExpiryCheckWorker` — daily, alerts on items expiring within 2 days
- `LowStockWorker` — every 12 hours, alerts when 3+ items are below threshold
- `ShoppingReminderWorker` — weekly, reminds on configured day/hour

All workers use `Source.CACHE` for Firestore reads so they work fully offline.

### Barcode scanner

`BarcodeScannerFragment` uses CameraX `ImageAnalysis` + ML Kit `BarcodeScanning`.
On a successful scan it calls the Open Food Facts API via `HttpURLConnection`
on a background `ExecutorService` — no Retrofit dependency needed.

### Android 11 background location (two-step)

The spec requires background location for geofencing. Request flow:

1. Request `ACCESS_FINE_LOCATION` first (in `SettingsFragment`)
2. Only after granted, request `ACCESS_BACKGROUND_LOCATION` separately
3. Never request both in the same call (Android 11 requirement)

### Deep links from notifications

All notifications use `PendingIntent` with URI scheme `pantrypilot://tab/{tab}`.
`MainActivity.handleDeepLink()` maps the last path segment to the correct
bottom nav destination.

---

## Screens

| Screen          | Entry point                                                         |
|-----------------|---------------------------------------------------------------------|
| Login           | `AuthActivity` → `LoginFragment`                                    |
| Sign up         | `SignUpFragment`                                                    |
| Dashboard       | `DashboardFragment` — summary cards + recent activity               |
| Pantry          | `PantryFragment` — search, add/edit/delete, stock chips             |
| Shopping        | `ShoppingFragment` — checkbox, assign, auto-populate, running total |
| Meal planner    | `MealPlannerFragment` — 7-day cards, ingredient stock check         |
| Members         | `MembersFragment` — emoji grid avatar picker                        |
| Settings        | `SettingsFragment` — household name, reminder, stores, sign out     |
| Barcode scanner | `BarcodeScannerFragment` — CameraX + Open Food Facts                |
| Receipt scanner | `ReceiptScannerFragment` — ML Kit OCR + Levenshtein fuzzy match     |

---

## Notification channels

| Channel ID           | Purpose                      |
|----------------------|------------------------------|
| `expiry_alerts`      | Items expiring within 2 days |
| `stock_alerts`       | 3+ items below min threshold |
| `shopping_reminders` | Weekly grocery day reminder  |
| `geofence_alerts`    | Near a saved grocery store   |

---

## Gradle / JDK versions

| Tool                  | Version                     |
|-----------------------|-----------------------------|
| Android Gradle Plugin | 8.2.2                       |
| Gradle distribution   | 8.2                         |
| JDK                   | 17 (Android Studio default) |
| `compileSdk`          | 34                          |
| `minSdk`              | 30                          |
| `targetSdk`           | 34                          |
