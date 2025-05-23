package com.callrecorder.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.callrecorder.app.R;
import com.callrecorder.app.fragments.ContactsFragment;
import com.callrecorder.app.fragments.RecordingsFragment;
import com.callrecorder.app.fragments.SettingsFragment;
import com.callrecorder.app.services.CallRecorderService;
import com.callrecorder.app.viewmodels.RecordingsViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private RecordingsViewModel viewModel;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(RecordingsViewModel.class);

        // Setup bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

        // Check and request permissions
        if (!hasRequiredPermissions()) {
            requestPermissions();
        } else {
            // Start the service if permissions are granted
            startCallRecorderService();
        }

        // Load default fragment
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_recordings);
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    switch (item.getItemId()) {
                        case R.id.nav_recordings:
                            selectedFragment = new RecordingsFragment();
                            break;
                        case R.id.nav_contacts:
                            selectedFragment = new ContactsFragment();
                            break;
                        case R.id.nav_settings:
                            selectedFragment = new SettingsFragment();
                            break;
                    }

                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
                        return true;
                    }
                    return false;
                }
            };

    private boolean hasRequiredPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSIONS_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                // All permissions granted, start service
                startCallRecorderService();
            } else {
                // Some permissions denied, show explanation
                showPermissionsExplanationDialog();
            }
        }
    }

    private void showPermissionsExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("الأذونات مطلوبة")
                .setMessage("يحتاج تطبيق تسجيل المكالمات إلى جميع الأذونات المطلوبة للعمل بشكل صحيح. يرجى منح الأذونات للاستمرار.")
                .setPositiveButton("طلب الأذونات مرة أخرى", (dialog, which) -> requestPermissions())
                .setNegativeButton("إلغاء", (dialog, which) -> Toast.makeText(MainActivity.this, "لن يعمل التطبيق بشكل صحيح بدون الأذونات المطلوبة", Toast.LENGTH_LONG).show())
                .setCancelable(false)
                .show();
    }

    private void startCallRecorderService() {
        Intent serviceIntent = new Intent(this, CallRecorderService.class);
        startService(serviceIntent);
    }
}
