package com.example.chatbasico.models;

import java.util.List;

public class Chat {
    private String id;
    private String name;
    private List<String> userIds;
    private String lastMessage;

    // Constructor vacío requerido para Firestore
    public Chat() {}

    public Chat(String id, String name, List<String> userIds, String lastMessage) {
        this.id = id;
        this.name = name;
        this.userIds = userIds;
        this.lastMessage = lastMessage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}
