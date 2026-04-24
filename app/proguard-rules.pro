# Firebase Firestore — keep POJO model classes for toObjects()
-keepclassmembers class com.pantrypilot.data.model.** {
    public <init>();
    public *;
}

# Keep Hilt generated classes
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keep class com.pantrypilot.workers.** { *; }

# Receivers
-keep class com.pantrypilot.receivers.** { *; }
