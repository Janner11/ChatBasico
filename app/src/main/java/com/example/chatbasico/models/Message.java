package com.example.chatbasico.models;

public class Message {
    private String text;
    private String senderId; // Guardamos el UID del usuario
    private long timestamp;

    // Constructor vacío necesario para Firebase
    public Message() {}

    public Message(String text, String senderId, long timestamp) {
        this.text = text;
        this.senderId = senderId;
        this.timestamp = timestamp;
    }

    // Getters y Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
