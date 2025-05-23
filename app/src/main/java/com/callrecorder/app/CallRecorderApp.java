package com.callrecorder.app;

import android.app.Application;
import android.content.Context;

import androidx.room.Room;

import com.callrecorder.app.database.AppDatabase;
import com.callrecorder.app.utils.SettingsManager;

public class CallRecorderApp extends Application {
    private static Context appContext;
    private static AppDatabase database;
    private static SettingsManager settingsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        
        // Initialize database
        database = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "call_recorder_database")
                .build();
                
        // Initialize settings manager
        settingsManager = new SettingsManager(getApplicationContext());
    }

    public static Context getAppContext() {
        return appContext;
    }

    public static AppDatabase getDatabase() {
        return database;
    }

    public static SettingsManager getSettingsManager() {
        return settingsManager;
    }
}
