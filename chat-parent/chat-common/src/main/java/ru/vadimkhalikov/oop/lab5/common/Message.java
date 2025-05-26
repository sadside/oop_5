package ru.vadimkhalikov.oop.lab5.common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String sender;
    private String content;
    private java.util.List<String> userList;

    public enum MessageType {
        LOGIN_REQUEST,
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        USER_MESSAGE,
        SERVER_MESSAGE,
        USER_LIST_REQUEST,
        USER_LIST_RESPONSE,
        USER_JOINED,
        USER_LEFT,
        LOGOUT_REQUEST
    }

    public Message() {
        // Для JSON десериализации
    }

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, String content) {
        this.type = type;
        this.content = content;
    }

    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public java.util.List<String> getUserList() {
        return userList;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public void setUserList(java.util.List<String> userList) {
        this.userList = userList;
    }

    @Override
    public String toString() {
        return "Message{" +
               "type=" + type +
               ", sender='" + sender + '\'' +
               ", content='" + content + '\'' +
               ", userList=" + userList +
               '}';
    }
} 