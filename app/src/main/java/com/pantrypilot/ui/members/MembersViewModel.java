package com.pantrypilot.ui.members;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.pantrypilot.data.firebase.MemberRepository;
import com.pantrypilot.data.firebase.ShoppingRepository;
import com.pantrypilot.data.model.Member;
import com.pantrypilot.data.model.ShoppingItem;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MembersViewModel extends ViewModel {

    public final MutableLiveData<List<Member>> members = new MutableLiveData<>();
    public final MutableLiveData<List<ShoppingItem>> shoppingItems = new MutableLiveData<>();
    private final MemberRepository memberRepo;
    private final ShoppingRepository shoppingRepo;
    private final FirebaseAuth auth;

    @Inject
    public MembersViewModel(MemberRepository mr, ShoppingRepository sr, FirebaseAuth auth) {
        this.memberRepo = mr;
        this.shoppingRepo = sr;
        this.auth = auth;
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (!uid.isEmpty()) {
            memberRepo.subscribeMembers(uid, members);
            shoppingRepo.subscribeShoppingItems(uid, shoppingItems);
        }
    }

    private String uid() {
        return auth.getCurrentUser().getUid();
    }

    public void addMember(Member member) {
        memberRepo.addMember(uid(), member);
    }

    public void deleteMember(Member member) {
        memberRepo.deleteMember(uid(), member.id);
    }

    public int assignedCount(String memberName) {
        List<ShoppingItem> items = shoppingItems.getValue();
        if (items == null) return 0;
        int c = 0;
        for (ShoppingItem i : items) if (memberName.equals(i.assignedTo) && !i.bought) c++;
        return c;
    }

    public List<ShoppingItem> assignedItems(String memberName) {
        List<ShoppingItem> items = shoppingItems.getValue();
        List<ShoppingItem> result = new ArrayList<>();
        if (items == null) return result;
        for (ShoppingItem i : items) if (memberName.equals(i.assignedTo)) result.add(i);
        return result;
    }

    @Override
    protected void onCleared() {
        memberRepo.removeListener();
        shoppingRepo.removeListener();
        super.onCleared();
    }
}
