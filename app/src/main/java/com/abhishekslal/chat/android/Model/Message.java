package com.abhishekslal.chat.android.Model;

public class Message {
    private String message, type, chat_type, chat_id;
    private long time;
    private boolean seen;
    private String from;

    // default constructor
    public Message() {
    }

    // constructor
    public Message(String message, String type, String chat_type, String chat_id, long time, boolean seen, String from) {
        this.message = message;
        this.type = type;
        this.chat_type = chat_type;
        this.chat_id = chat_id;
        this.time = time;
        this.seen = seen;
        this.from = from;
    }

    // getter & setter
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getChat_type() {
        return chat_type;
    }

    public void setChat_type(String chat_type) {
        this.chat_type = chat_type;
    }

    public String getChat_id() {
        return chat_id;
    }

    public void setChat_id(String chat_id) {
        this.chat_id = chat_id;
    }
}
