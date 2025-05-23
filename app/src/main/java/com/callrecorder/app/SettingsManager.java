package com.callrecorder.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

public class SettingsManager {
    private static final String PREFS_NAME = "call_recorder_settings";
    
    // Keys for SharedPreferences
    private static final String KEY_AUTO_RECORD = "auto_record";
    private static final String KEY_RECORDING_QUALITY = "recording_quality";
    private static final String KEY_STORAGE_PATH = "storage_path";
    private static final String KEY_PASSWORD_PROTECTION = "password_protection";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    private static final String KEY_VIBRATION = "vibration";
    private static final String KEY_SHOW_NOTIFICATION = "show_notification";
    
    // Constants for recording quality
    public static final int QUALITY_LOW = 0;
    public static final int QUALITY_MEDIUM = 1;
    public static final int QUALITY_HIGH = 2;
    
    private final SharedPreferences prefs;
    
    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public boolean isAutoRecordEnabled() {
        return prefs.getBoolean(KEY_AUTO_RECORD, false);
    }
    
    public void setAutoRecordEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_RECORD, enabled).apply();
    }
    
    public int getRecordingQuality() {
        return prefs.getInt(KEY_RECORDING_QUALITY, QUALITY_MEDIUM);
    }
    
    public void setRecordingQuality(int quality) {
        prefs.edit().putInt(KEY_RECORDING_QUALITY, quality).apply();
    }
    
    public String getStoragePath() {
        return prefs.getString(KEY_STORAGE_PATH, getDefaultStoragePath());
    }
    
    public void setStoragePath(String path) {
        prefs.edit().putString(KEY_STORAGE_PATH, path).apply();
    }
    
    public boolean isPasswordProtectionEnabled() {
        return prefs.getBoolean(KEY_PASSWORD_PROTECTION, false);
    }
    
    public void setPasswordProtectionEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PASSWORD_PROTECTION, enabled).apply();
    }
    
    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, null);
    }
    
    public void setPassword(String password) {
        prefs.edit().putString(KEY_PASSWORD, password).apply();
    }
    
    public boolean isNotificationSoundEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATION_SOUND, true);
    }
    
    public void setNotificationSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_SOUND, enabled).apply();
    }
    
    public boolean isVibrationEnabled() {
        return prefs.getBoolean(KEY_VIBRATION, true);
    }
    
    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply();
    }
    
    public boolean isShowNotificationEnabled() {
        return prefs.getBoolean(KEY_SHOW_NOTIFICATION, true);
    }
    
    public void setShowNotificationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SHOW_NOTIFICATION, enabled).apply();
    }
    
    private String getDefaultStoragePath() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), "CallRecordings");
        return directory.getAbsolutePath();
    }
    
    // Reset all settings to default values
    public void resetToDefaults() {
        prefs.edit()
                .putBoolean(KEY_AUTO_RECORD, false)
                .putInt(KEY_RECORDING_QUALITY, QUALITY_MEDIUM)
                .putString(KEY_STORAGE_PATH, getDefaultStoragePath())
                .putBoolean(KEY_PASSWORD_PROTECTION, false)
                .putString(KEY_PASSWORD, null)
                .putBoolean(KEY_NOTIFICATION_SOUND, true)
                .putBoolean(KEY_VIBRATION, true)
                .putBoolean(KEY_SHOW_NOTIFICATION, true)
                .apply();
    }
}
