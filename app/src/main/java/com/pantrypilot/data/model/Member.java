package com.pantrypilot.data.model;

/**
 * Mirrors Firestore: households/{uid}/members/{docId}
 */
public class Member {
    public String id;
    public String name;
    public String avatarEmoji;

    public Member() {
    }
}
