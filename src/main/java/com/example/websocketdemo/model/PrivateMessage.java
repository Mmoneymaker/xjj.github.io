package com.example.websocketdemo.model;


/**
 * Created by rajeevkumarsingh on 24/07/17.
 */
public class PrivateMessage{

    private String content;
    private String sender;
    private String receiver;


    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}

