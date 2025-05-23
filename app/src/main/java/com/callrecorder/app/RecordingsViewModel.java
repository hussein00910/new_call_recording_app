package com.callrecorder.app.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.callrecorder.app.database.AppDatabase;
import com.callrecorder.app.database.RecordingDao;
import com.callrecorder.app.models.Recording;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;

public class RecordingsViewModel extends AndroidViewModel {
    private final RecordingDao recordingDao;
    private final LiveData<List<Recording>> allRecordings;
    private final LiveData<List<Recording>> starredRecordings;
    private final MutableLiveData<Integer> filterType = new MutableLiveData<>();
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>();

    public RecordingsViewModel(@NonNull Application application) {
        super(application);
        recordingDao = AppDatabase.getInstance(application).recordingDao();
        allRecordings = recordingDao.getAllRecordings();
        starredRecordings = recordingDao.getStarredRecordings();
        
        // Set default filter to show all recordings
        filterType.setValue(0);
        searchQuery.setValue("");
    }

    public LiveData<List<Recording>> getAllRecordings() {
        return allRecordings;
    }

    public LiveData<List<Recording>> getStarredRecordings() {
        return starredRecordings;
    }

    public LiveData<List<Recording>> getFilteredRecordings() {
        return Transformations.switchMap(filterType, type -> {
            if (type == 0) {
                return allRecordings;
            } else if (type == -1) {
                return starredRecordings;
            } else {
                return recordingDao.getRecordingsByType(type);
            }
        });
    }

    public LiveData<List<Recording>> getSearchResults() {
        return Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.isEmpty()) {
                return getFilteredRecordings();
            } else {
                return recordingDao.searchRecordings(query);
            }
        });
    }

    public void setFilter(int type) {
        filterType.setValue(type);
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public void toggleStar(Recording recording) {
        recording.setStarred(!recording.isStarred());
        updateRecording(recording);
    }

    public void insertRecording(Recording recording) {
        Executors.newSingleThreadExecutor().execute(() -> {
            recordingDao.insert(recording);
        });
    }

    public void updateRecording(Recording recording) {
        Executors.newSingleThreadExecutor().execute(() -> {
            recordingDao.update(recording);
        });
    }

    public void deleteRecording(Recording recording) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Delete the file first
            File file = new File(recording.getFilePath());
            if (file.exists()) {
                file.delete();
            }
            
            // Then delete the database entry
            recordingDao.delete(recording);
        });
    }
}
