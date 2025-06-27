package com.otis.lstm;

import com.google.firebase.Timestamp;

public class Device {
    private String deviceId;
    private String userId;
    private Timestamp timestamp;

    public Device() {
    }

    public Device(String deviceId, String userId, Timestamp timestamp) {
        this.deviceId = deviceId;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}