package com.pantrypilot.data.firebase;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.pantrypilot.data.model.Member;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MemberRepository {

    private final FirebaseFirestore db;
    private ListenerRegistration memberListener;

    @Inject
    public MemberRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public void subscribeMembers(String uid, MutableLiveData<List<Member>> liveData) {
        if (memberListener != null) memberListener.remove();
        memberListener = db.collection("households").document(uid)
                .collection("members")
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null) {
                        List<Member> members = snapshot.toObjects(Member.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            members.get(i).id = snapshot.getDocuments().get(i).getId();
                        }
                        liveData.postValue(members);
                    }
                });
    }

    public void removeListener() {
        if (memberListener != null) {
            memberListener.remove();
            memberListener = null;
        }
    }

    public void addMember(String uid, Member member) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", member.name);
        data.put("avatarEmoji", member.avatarEmoji);
        db.collection("households").document(uid)
                .collection("members")
                .add(data);
    }

    public void deleteMember(String uid, String memberId) {
        db.collection("households").document(uid)
                .collection("members")
                .document(memberId)
                .delete();
    }
}
