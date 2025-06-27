package com.otis.lstm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.LogViewHolder> {

    private final List<LogsActivity.LogEntry> logs;

    public LogsAdapter(List<LogsActivity.LogEntry> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogsActivity.LogEntry log = logs.get(position);

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        holder.timestampTextView.setText(sdf.format(log.timestamp));

        holder.eventTypeTextView.setText(log.eventType);
        holder.messageTextView.setText(log.message);

        // Format details map for readability
        String details = log.details;
        StringBuilder formattedDetails = new StringBuilder();
        if (details != null && details.startsWith("{") && details.endsWith("}")) {
            details = details.substring(1, details.length() - 1); // Remove braces
            String[] pairs = details.split(", ");
            for (String pair : pairs) {
                String[] parts = pair.split("=");
                if (parts.length == 2) {
                    formattedDetails.append(parts[0]).append(": ").append(parts[1]).append("\n");
                } else {
                    formattedDetails.append(pair).append("\n");
                }
            }
        } else {
            formattedDetails.append(details != null ? details : "");
        }
        holder.detailsTextView.setText(formattedDetails.toString().trim());
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView timestampTextView;
        TextView eventTypeTextView;
        TextView messageTextView;
        TextView detailsTextView;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            eventTypeTextView = itemView.findViewById(R.id.eventTypeTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            detailsTextView = itemView.findViewById(R.id.detailsTextView);
        }
    }
}