package com.callrecorder.app.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.callrecorder.app.R;
import com.callrecorder.app.adapters.RecordingsAdapter;
import com.callrecorder.app.models.Recording;
import com.callrecorder.app.viewmodels.RecordingsViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class RecordingsFragment extends Fragment implements RecordingsAdapter.RecordingItemListener {
    private RecordingsViewModel viewModel;
    private RecordingsAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private EditText searchEditText;
    private Spinner filterSpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recordings, container, false);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_view_recordings);
        emptyView = view.findViewById(R.id.text_empty_recordings);
        searchEditText = view.findViewById(R.id.edit_text_search);
        filterSpinner = view.findViewById(R.id.spinner_filter);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(RecordingsViewModel.class);
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup search
        setupSearch();
        
        // Setup filter
        setupFilter();
        
        // Observe recordings
        observeRecordings();
    }

    private void setupRecyclerView() {
        adapter = new RecordingsAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private void setupFilter() {
        filterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                // Convert spinner position to filter type
                // 0: All, 1: Starred, 2: Incoming, 3: Outgoing
                int filterType;
                switch (position) {
                    case 1:
                        filterType = -1; // Starred
                        break;
                    case 2:
                        filterType = 1; // Incoming
                        break;
                    case 3:
                        filterType = 2; // Outgoing
                        break;
                    case 0:
                    default:
                        filterType = 0; // All
                        break;
                }
                viewModel.setFilter(filterType);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Not needed
            }
        });
    }

    private void observeRecordings() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), recordings -> {
            adapter.submitList(recordings);
            updateEmptyView(recordings);
        });
    }

    private void updateEmptyView(List<Recording> recordings) {
        if (recordings == null || recordings.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(Recording recording) {
        // Open playback activity
        // Intent intent = new Intent(requireContext(), PlaybackActivity.class);
        // intent.putExtra("recording_id", recording.getId());
        // startActivity(intent);
    }

    @Override
    public void onStarClick(Recording recording) {
        viewModel.toggleStar(recording);
    }

    @Override
    public void onDeleteClick(Recording recording) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("حذف التسجيل")
                .setMessage("هل أنت متأكد من حذف هذا التسجيل؟")
                .setPositiveButton("حذف", (dialog, which) -> viewModel.deleteRecording(recording))
                .setNegativeButton("إلغاء", null)
                .show();
    }
}
