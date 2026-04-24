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

    private final MemberRepository memberRepo;
    private final ShoppingRepository shoppingRepo;
    private final FirebaseAuth auth;

    public final MutableLiveData<List<Member>> members = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<ShoppingItem>> shoppingItems = new MutableLiveData<>(new ArrayList<>());

    @Inject
    public MembersViewModel(MemberRepository mr, ShoppingRepository sr, FirebaseAuth auth) {
        this.memberRepo = mr;
        this.shoppingRepo = sr;
        this.auth = auth;
        String uid = uid();
        if (uid != null) {
            memberRepo.subscribe(uid, members);
            shoppingRepo.subscribe(uid, shoppingItems);
        }
    }

    private String uid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
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

    @Override
    protected void onCleared() {
        memberRepo.removeListener();
        shoppingRepo.removeListener();
        super.onCleared();
    }
}
