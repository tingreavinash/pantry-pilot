# PantryPilot 🌿

Smart home grocery management for multi-household use.
Java · Jetpack Compose · Material You · Firebase · MVVM

---

## Prerequisites

| Tool                  | Version                      |
|-----------------------|------------------------------|
| Android Studio        | Hedgehog (2023.1.1) or newer |
| JDK                   | 17                           |
| Android Gradle Plugin | 8.2.2                        |
| Min SDK               | 30 (Android 11)              |
| Target SDK            | 34                           |

---

## Setup

### 1. Firebase

This app shares an existing Firebase project (React web app + Android).

1. Open [Firebase Console](https://console.firebase.google.com)
2. Select your existing project → **Add app** → Android
3. Package name: `com.pantrypilot`
4. Download `google-services.json`
5. Place it at: `app/google-services.json`

> ⚠️ Do NOT commit `google-services.json` to version control.

### 2. Firestore security rules

The Android app uses the same collections as the web app.
Ensure your rules allow authenticated users to read/write their household:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /households/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### 3. Firestore offline persistence

Configured automatically in `PantryPilotApp.java`:

```java
FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
    .setPersistenceEnabled(true)
    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
    .build();
```

### 4. Lora font (optional, recommended)

Download Lora from [Google Fonts](https://fonts.google.com/specimen/Lora):

- `lora_regular.ttf`
- `lora_semibold.ttf`
- `lora_bold.ttf`

Place in `app/src/main/res/font/`

Then update `Theme.java` to enable the Lora font family for display/headline styles.

### 5. Build + run

```bash
./gradlew assembleDebug
# or open in Android Studio and hit Run
```

---

## Architecture

```
UI Layer (Jetpack Compose)
    │  observeAsState()
    ▼
ViewModel (Hilt, LiveData)
    │  calls
    ▼
Repository (Firebase / Room)
    │  addSnapshotListener() / Dao
    ▼
Firebase Firestore / Room DB
```

### Async pattern

- All Firestore reads use `addSnapshotListener()` → `liveData.postValue()`
- ViewModels expose `MutableLiveData<List<T>>`
- Composables collect via `observeAsState()`
- No Kotlin coroutines — pure Java Executors + Firebase Tasks API

---

## Firestore Data Model

```
households/{uid}/
  pantryItems/   → name, category, quantity, unit, minThreshold, expiryDate, createdAt
  shoppingList/  → name, category, quantity, unit, estimatedCost, bought, assignedTo, createdAt
  meals/         → day, mealName, ingredients[]
  members/       → name, avatarEmoji
  fcmTokens/     → token, platform, updatedAt
```

> Schema is identical to the React web app — no changes made.

---

## Features

| Feature               | Status | Notes                                             |
|-----------------------|--------|---------------------------------------------------|
| Auth (email/password) | ✅      | Shared Firebase project                           |
| Dashboard             | ✅      | 4 summary cards + recent activity                 |
| Pantry CRUD           | ✅      | Swipe-to-delete, search, category filter          |
| Shopping list         | ✅      | Auto-populate, assign members, running total      |
| Meal planner          | ✅      | 7-day view, ingredient stock check                |
| Members               | ✅      | Emoji avatar picker, assignment view              |
| Settings              | ✅      | Household name, reminder, stores, sign out        |
| Barcode scanner       | ✅      | CameraX + ML Kit + Open Food Facts API            |
| Receipt OCR           | ✅      | ML Kit Text Recognition + Levenshtein fuzzy match |
| Offline mode          | ✅      | Firestore persistence + amber banner              |
| Push notifications    | ✅      | WorkManager (local, no Cloud Functions needed)    |
| Geofencing            | ✅      | Room DB for stores, GeofencingClient              |
| Material You          | ✅      | Forest green primary, amber secondary             |

---

## Build Phases

| Phase | Contents                                 |
|-------|------------------------------------------|
| 1     | Auth + MVVM + Dashboard + Pantry         |
| 2     | Shopping + Meals + Members + Settings    |
| 3     | Barcode Scanner + Open Food Facts        |
| 4     | Offline persistence banner + WorkManager |
| 5     | Receipt OCR + ReceiptParser              |
| 6     | Geofencing + Room + BootReceiver         |

---

## Deep Links

All notifications deep-link to the relevant tab:

```
pantrypilot://tab/Dashboard
pantrypilot://tab/Pantry
pantrypilot://tab/Shopping
pantrypilot://tab/Meals
pantrypilot://tab/Members
```

---

## Android 11 Permission Notes

**Background location** must be requested in two separate steps:

1. Request `ACCESS_FINE_LOCATION` first
2. Only after granted → request `ACCESS_BACKGROUND_LOCATION`
3. Never request both in the same `requestPermissions()` call

**Scoped storage**: Receipt photo picking uses `ActivityResultContracts.GetContent("image/*")` — no
`READ_EXTERNAL_STORAGE` needed on API 30+.

---

## License

Private / internal use. Firebase project owned by the household admin account.
