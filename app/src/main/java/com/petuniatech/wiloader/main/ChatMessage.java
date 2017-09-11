
//Code Source: https://www.codeproject.com/Tips/897826/Designing-Android-Chat-Bubble-Chat-UI
// by JoCodes, CEO Technovibe Solutions. licensed under The Code Project Open License (CPOL)

package com.petuniatech.wiloader.main;

/**
 * Created by PetuniaTech on 2017-06-28.
 */

public class ChatMessage {

    private long id;
    private boolean isMe;
    private String message;
    private Long userId;
    private String dateTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean getIsMe() {
        return isMe;
    }

    public void setMe(boolean isMe) {
        this.isMe = isMe;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getDate() {
        return dateTime;
    }

    public void setDate(String dateTime) {
        this.dateTime = dateTime;
    }
}
