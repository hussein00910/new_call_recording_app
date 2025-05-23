package com.callrecorder.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.callrecorder.app.R;
import com.callrecorder.app.models.Recording;
import com.callrecorder.app.utils.ContactUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RecordingsAdapter extends ListAdapter<Recording, RecordingsAdapter.RecordingViewHolder> {
    
    private final RecordingItemListener listener;
    
    public RecordingsAdapter(RecordingItemListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public RecordingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recording, parent, false);
        return new RecordingViewHolder(itemView);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecordingViewHolder holder, int position) {
        Recording recording = getItem(position);
        holder.bind(recording);
    }
    
    class RecordingViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView dateTimeTextView;
        private final TextView durationTextView;
        private final ImageView callTypeImageView;
        private final ImageButton starButton;
        private final ImageButton deleteButton;
        
        public RecordingViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.text_view_name);
            dateTimeTextView = itemView.findViewById(R.id.text_view_date_time);
            durationTextView = itemView.findViewById(R.id.text_view_duration);
            callTypeImageView = itemView.findViewById(R.id.image_view_call_type);
            starButton = itemView.findViewById(R.id.button_star);
            deleteButton = itemView.findViewById(R.id.button_delete);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(position));
                }
            });
            
            starButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onStarClick(getItem(position));
                }
            });
            
            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeleteClick(getItem(position));
                }
            });
        }
        
        public void bind(Recording recording) {
            // Set contact name or phone number
            String displayName = recording.getContactName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = recording.getPhoneNumber();
                if (displayName == null || displayName.isEmpty()) {
                    displayName = "رقم غير معروف";
                } else {
                    displayName = ContactUtils.formatPhoneNumber(displayName);
                }
            }
            nameTextView.setText(displayName);
            
            // Set date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM، hh:mm a", new Locale("ar"));
            dateTimeTextView.setText(dateFormat.format(new Date(recording.getDate())));
            
            // Set duration
            long minutes = TimeUnit.MILLISECONDS.toMinutes(recording.getDuration());
            long seconds = TimeUnit.MILLISECONDS.toSeconds(recording.getDuration()) % 60;
            durationTextView.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
            
            // Set call type icon
            int callTypeIcon = recording.getCallType() == 1 ? 
                    R.drawable.ic_call_received : R.drawable.ic_call_made;
            callTypeImageView.setImageResource(callTypeIcon);
            
            // Set star icon
            int starIcon = recording.isStarred() ? 
                    R.drawable.ic_star_filled : R.drawable.ic_star_outline;
            starButton.setImageResource(starIcon);
        }
    }
    
    public interface RecordingItemListener {
        void onItemClick(Recording recording);
        void onStarClick(Recording recording);
        void onDeleteClick(Recording recording);
    }
    
    private static final DiffUtil.ItemCallback<Recording> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<Recording>() {
                @Override
                public boolean areItemsTheSame(@NonNull Recording oldItem, @NonNull Recording newItem) {
                    return oldItem.getId() == newItem.getId();
                }
                
                @Override
                public boolean areContentsTheSame(@NonNull Recording oldItem, @NonNull Recording newItem) {
                    return oldItem.getFilePath().equals(newItem.getFilePath()) &&
                           oldItem.isStarred() == newItem.isStarred() &&
                           oldItem.getDate() == newItem.getDate();
                }
            };
}
