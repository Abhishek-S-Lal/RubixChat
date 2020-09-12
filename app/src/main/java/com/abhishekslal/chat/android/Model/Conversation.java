package com.abhishekslal.chat.android.Model;

public class Conversation {
    private String friendId;
    private String lastMessage;

    public Conversation() {
    }

    public Conversation(String friendId, String lastMessage) {
        this.friendId = friendId;
        this.lastMessage = lastMessage;
    }

    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}
