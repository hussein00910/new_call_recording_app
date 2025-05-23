package com.callrecorder.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.callrecorder.app.CallRecorderApp;
import com.callrecorder.app.R;
import com.callrecorder.app.activities.MainActivity;
import com.callrecorder.app.database.AppDatabase;
import com.callrecorder.app.models.Recording;
import com.callrecorder.app.utils.ContactUtils;
import com.callrecorder.app.utils.SettingsManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CallRecorderService extends Service {
    private static final String TAG = "CallRecorderService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "call_recorder_channel";

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String outputFile;
    private long startTime;
    private String phoneNumber;
    private int callType; // 1 for incoming, 2 for outgoing
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private SettingsManager settingsManager;
    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        settingsManager = CallRecorderApp.getSettingsManager();
        database = CallRecorderApp.getDatabase();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Extract phone number and call type from intent if available
        if (intent != null) {
            phoneNumber = intent.getStringExtra("phone_number");
            callType = intent.getIntExtra("call_type", 0);
        }

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification("تطبيق تسجيل المكالمات قيد التشغيل"));

        // Setup phone state listener
        setupPhoneStateListener();

        return START_STICKY;
    }

    private void setupPhoneStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Call ended, stop recording
                        if (isRecording) {
                            stopRecording();
                        }
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        // Call started, begin recording
                        if (!isRecording) {
                            if (phoneNumber == null) {
                                phoneNumber = incomingNumber;
                            }
                            if (callType == 0) {
                                // If call type wasn't set, try to determine it
                                // This is a simplification, might not be accurate in all cases
                                callType = (incomingNumber != null && !incomingNumber.isEmpty()) ? 1 : 2;
                            }
                            startRecording();
                        }
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        // Phone is ringing, prepare for recording
                        if (phoneNumber == null) {
                            phoneNumber = incomingNumber;
                        }
                        if (callType == 0) {
                            callType = 1; // Incoming call
                        }
                        break;
                }
            }
        };

        // Register the phone state listener
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void startRecording() {
        if (isRecording) return;

        try {
            // Create output directory if it doesn't exist
            File directory = new File(settingsManager.getStoragePath());
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Create output file
            outputFile = createOutputFile();

            // Configure MediaRecorder based on quality settings
            mediaRecorder = new MediaRecorder();
            
            // Try to use VOICE_COMMUNICATION source first (better for calls)
            try {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            } catch (Exception e) {
                // Fallback to MIC if VOICE_COMMUNICATION is not available
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                
                // Enable speaker if using microphone
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                audioManager.setSpeakerphoneOn(true);
            }

            // Set output format and encoder based on quality setting
            int quality = settingsManager.getRecordingQuality();
            switch (quality) {
                case SettingsManager.QUALITY_HIGH:
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mediaRecorder.setAudioSamplingRate(44100);
                    mediaRecorder.setAudioEncodingBitRate(192000);
                    break;
                case SettingsManager.QUALITY_LOW:
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    break;
                case SettingsManager.QUALITY_MEDIUM:
                default:
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mediaRecorder.setAudioSamplingRate(22050);
                    mediaRecorder.setAudioEncodingBitRate(96000);
                    break;
            }

            mediaRecorder.setOutputFile(outputFile);
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            startTime = System.currentTimeMillis();
            isRecording = true;
            
            // Update notification
            updateNotification("جاري تسجيل المكالمة...");
            
        } catch (IOException e) {
            Log.e(TAG, "Error starting recording", e);
            // Try alternative recording method
            startAlternativeRecording();
        }
    }

    private void startAlternativeRecording() {
        try {
            // Enable speaker
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setSpeakerphoneOn(true);
            
            // Create output file
            if (outputFile == null) {
                outputFile = createOutputFile();
            }
            
            // Configure MediaRecorder with microphone source
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            
            // Use lower quality settings for alternative method
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(outputFile);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            startTime = System.currentTimeMillis();
            isRecording = true;
            
            // Update notification
            updateNotification("جاري تسجيل المكالمة (وضع بديل)...");
            
        } catch (IOException e) {
            Log.e(TAG, "Error starting alternative recording", e);
            isRecording = false;
        }
    }

    private void stopRecording() {
        if (!isRecording) return;

        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            
            // Calculate duration
            long duration = System.currentTimeMillis() - startTime;
            
            // Turn off speaker if it was enabled
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setSpeakerphoneOn(false);
            
            // Save recording info to database
            saveRecordingToDatabase(duration);
            
            isRecording = false;
            updateNotification("تم حفظ التسجيل");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
        }
    }

    private String createOutputFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName;
        
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            // Use contact name if available
            String contactName = ContactUtils.getContactName(this, phoneNumber);
            if (contactName != null && !contactName.isEmpty()) {
                fileName = contactName + "_" + timestamp;
            } else {
                fileName = phoneNumber + "_" + timestamp;
            }
        } else {
            fileName = "Unknown_" + timestamp;
        }
        
        // Add call type indicator
        fileName += (callType == 1) ? "_incoming" : "_outgoing";
        
        // Get file extension based on quality setting
        String extension;
        int quality = settingsManager.getRecordingQuality();
        switch (quality) {
            case SettingsManager.QUALITY_HIGH:
                extension = ".aac";
                break;
            case SettingsManager.QUALITY_LOW:
                extension = ".3gp";
                break;
            case SettingsManager.QUALITY_MEDIUM:
            default:
                extension = ".mp4";
                break;
        }
        
        return settingsManager.getStoragePath() + File.separator + fileName + extension;
    }

    private void saveRecordingToDatabase(final long duration) {
        // Get contact name if available
        String contactName = null;
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            contactName = ContactUtils.getContactName(this, phoneNumber);
        }
        
        // Create recording object
        final Recording recording = new Recording(
                0, // ID will be auto-generated
                phoneNumber,
                contactName,
                callType,
                outputFile,
                duration,
                System.currentTimeMillis(),
                false,
                null
        );
        
        // Insert recording into database
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                database.recordingDao().insert(recording);
            }
        });
    }

    private Notification createNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("تسجيل المكالمات")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createNotification(text));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "خدمة تسجيل المكالمات",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("إشعارات خدمة تسجيل المكالمات");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (isRecording) {
            stopRecording();
        }
        
        if (telephonyManager != null && phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
