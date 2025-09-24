package com.example.chatbasico.models;

public class Message {
    private String text;
    private String sender;
    private long timestamp;

    // Constructor vacío necesario para Firebase
    public Message() {}

    public Message(String text, String sender, long timestamp) {
        this.text = text;
        this.sender = sender;
        this.timestamp = timestamp;
    }

    // Getters y Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
