package com.callrecorder.app.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recordings")
public class Recording {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String phoneNumber;
    private String contactName;
    private int callType; // 1 for incoming, 2 for outgoing
    private String filePath;
    private long duration;
    private long date;
    private boolean isStarred;
    private String notes;

    public Recording(long id, String phoneNumber, String contactName, int callType, 
                    String filePath, long duration, long date, boolean isStarred, String notes) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.contactName = contactName;
        this.callType = callType;
        this.filePath = filePath;
        this.duration = duration;
        this.date = date;
        this.isStarred = isStarred;
        this.notes = notes;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public int getCallType() {
        return callType;
    }

    public void setCallType(int callType) {
        this.callType = callType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean isStarred() {
        return isStarred;
    }

    public void setStarred(boolean starred) {
        isStarred = starred;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
