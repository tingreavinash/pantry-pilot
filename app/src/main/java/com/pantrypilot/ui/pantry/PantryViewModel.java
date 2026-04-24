package com.pantrypilot.ui.pantry;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.pantrypilot.data.firebase.PantryRepository;
import com.pantrypilot.data.model.PantryItem;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PantryViewModel extends ViewModel {

    public final MutableLiveData<List<PantryItem>> pantryItems = new MutableLiveData<>(new ArrayList<>());
    private final PantryRepository repo;
    private final FirebaseAuth auth;
    public final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    @Inject
    public PantryViewModel(PantryRepository repo, FirebaseAuth auth) {
        this.repo = repo;
        this.auth = auth;
        String uid = uid();
        if (uid != null) repo.subscribe(uid, pantryItems);
    }

    public void addItem(PantryItem item) {
        String uid = uid();
        if (uid == null) return;
        repo.addItem(uid, item,
                () -> toastMessage.postValue("Item added"),
                () -> toastMessage.postValue("Failed to add item"));
    }

    public void updateItem(PantryItem item) {
        String uid = uid();
        if (uid == null) return;
        repo.updateItem(uid, item,
                () -> toastMessage.postValue("Item updated"),
                () -> toastMessage.postValue("Update failed"));
    }

    public void deleteItem(PantryItem item) {
        String uid = uid();
        if (uid == null) return;
        repo.deleteItem(uid, item.id, () -> toastMessage.postValue("Deleted"));
    }

    private String uid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    @Override
    protected void onCleared() {
        repo.removeListener();
        super.onCleared();
    }
}
