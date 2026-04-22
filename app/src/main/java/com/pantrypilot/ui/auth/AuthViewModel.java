package com.pantrypilot.ui.auth;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.pantrypilot.data.firebase.AuthRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AuthViewModel extends ViewModel {

    public final MutableLiveData<FirebaseUser> currentUser = new MutableLiveData<>();
    public final MutableLiveData<String> error = new MutableLiveData<>();
    public final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final AuthRepository authRepo;

    @Inject
    public AuthViewModel(AuthRepository authRepo) {
        this.authRepo = authRepo;
        authRepo.observeAuthState(currentUser);
    }

    public void login(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            error.setValue("Email and password are required");
            return;
        }
        loading.setValue(true);
        authRepo.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                loading.postValue(false);
            }

            @Override
            public void onError(String msg) {
                loading.postValue(false);
                error.postValue(msg);
            }
        });
    }

    public void signUp(String householdName, String email, String password, String confirm) {
        if (householdName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            error.setValue("All fields are required");
            return;
        }
        if (!password.equals(confirm)) {
            error.setValue("Passwords do not match");
            return;
        }
        if (password.length() < 6) {
            error.setValue("Password must be at least 6 characters");
            return;
        }
        loading.setValue(true);
        authRepo.signUp(householdName, email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                loading.postValue(false);
            }

            @Override
            public void onError(String msg) {
                loading.postValue(false);
                error.postValue(msg);
            }
        });
    }

    public void signOut() {
        authRepo.signOut();
    }

    public boolean isLoggedIn() {
        return authRepo.getCurrentUser() != null;
    }
}
