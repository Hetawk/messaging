package com.andgroupco.messaging.models;

import java.util.Date;

public class Message {
    private long id;
    private String content;
    private String recipient;
    private String recipientType; // EMAIL or SMS
    private Date sentDate;
    private String status; // SENT, FAILED, PENDING
    private int sendCount;

    public Message() {
        this.sendCount = 0;
        this.status = "PENDING";
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(String recipientType) {
        this.recipientType = recipientType;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSendCount() {
        return sendCount;
    }

    public void setSendCount(int sendCount) {
        this.sendCount = sendCount;
    }

    // Helper method to increment send count
    public void incrementSendCount() {
        this.sendCount++;
    }
}
