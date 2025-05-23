package com.callrecorder.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.callrecorder.app.models.Recording;

import java.util.List;

@Dao
public interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY date DESC")
    LiveData<List<Recording>> getAllRecordings();
    
    @Query("SELECT * FROM recordings WHERE isStarred = 1 ORDER BY date DESC")
    LiveData<List<Recording>> getStarredRecordings();
    
    @Query("SELECT * FROM recordings WHERE callType = :callType ORDER BY date DESC")
    LiveData<List<Recording>> getRecordingsByType(int callType);
    
    @Query("SELECT * FROM recordings WHERE contactName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' ORDER BY date DESC")
    LiveData<List<Recording>> searchRecordings(String query);
    
    @Insert
    long insert(Recording recording);
    
    @Update
    void update(Recording recording);
    
    @Delete
    void delete(Recording recording);
    
    @Query("DELETE FROM recordings WHERE id = :id")
    void deleteById(long id);
    
    @Query("SELECT * FROM recordings WHERE id = :id")
    Recording getRecordingById(long id);
}
