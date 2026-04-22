package com.pantrypilot.data.firebase;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AuthRepository {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseMessaging messaging;

    @Inject
    public AuthRepository(FirebaseAuth auth, FirebaseFirestore db, FirebaseMessaging messaging) {
        this.auth = auth;
        this.db = db;
        this.messaging = messaging;
    }

    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    registerFcmToken(result.getUser().getUid());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void signUp(String householdName, String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    Map<String, Object> householdDoc = new HashMap<>();
                    householdDoc.put("householdName", householdName);
                    householdDoc.put("ownerEmail", email);
                    householdDoc.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    db.collection("households").document(uid)
                            .set(householdDoc)
                            .addOnSuccessListener(v -> {
                                registerFcmToken(uid);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void signOut() {
        auth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void observeAuthState(MutableLiveData<FirebaseUser> liveData) {
        auth.addAuthStateListener(fa -> liveData.postValue(fa.getCurrentUser()));
    }

    private void registerFcmToken(String uid) {
        messaging.getToken().addOnSuccessListener(token -> {
            Map<String, Object> tokenDoc = new HashMap<>();
            tokenDoc.put("token", token);
            tokenDoc.put("platform", "android");
            tokenDoc.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            db.collection("households").document(uid)
                    .collection("fcmTokens").document(token)
                    .set(tokenDoc);
        });
    }

    public interface AuthCallback {
        void onSuccess();

        void onError(String message);
    }
}
