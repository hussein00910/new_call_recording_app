package com.callrecorder.app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.callrecorder.app.models.Recording;

@Database(entities = {Recording.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "call_recorder_database";
    private static volatile AppDatabase INSTANCE;
    
    public abstract RecordingDao recordingDao();
    
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
