package com.pantrypilot.di;

import android.content.Context;

import androidx.room.Room;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pantrypilot.data.local.AppDatabase;
import com.pantrypilot.data.local.GroceryStoreDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Provides
    @Singleton
    public FirebaseFirestore provideFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);
        return db;
    }

    @Provides
    @Singleton
    public FirebaseMessaging provideMessaging() {
        return FirebaseMessaging.getInstance();
    }

    @Provides
    @Singleton
    public AppDatabase provideRoomDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "pantrypilot_db")
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    @Singleton
    public GroceryStoreDao provideGroceryStoreDao(AppDatabase db) {
        return db.groceryStoreDao();
    }

    @Provides
    @Singleton
    public ExecutorService provideExecutorService() {
        return Executors.newFixedThreadPool(4);
    }
}
