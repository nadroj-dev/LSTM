package com.otis.lstm;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogsActivity extends AppCompatActivity {

    private RecyclerView logsRecyclerView;
    private LogsAdapter logsAdapter;
    private List<LogEntry> logEntries;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize RecyclerView
        logsRecyclerView = findViewById(R.id.logsRecyclerView);
        logsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        logEntries = new ArrayList<>();
        logsAdapter = new LogsAdapter(logEntries);
        logsRecyclerView.setAdapter(logsAdapter);

        // Fetch logs
        fetchLogs();
    }

    private void fetchLogs() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        firestore.collection("Logs")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        logEntries.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String eventType = document.getString("eventType");
                            String message = document.getString("message");
                            com.google.firebase.Timestamp timestamp = document.getTimestamp("timestamp");
                            Map<String, Object> details = (Map<String, Object>) document.get("details");

                            LogEntry logEntry = new LogEntry();
                            logEntry.timestamp = timestamp != null ? timestamp.toDate().getTime() : 0;
                            logEntry.eventType = eventType != null ? eventType : "Unknown";
                            logEntry.message = message != null ? message : "No message";
                            logEntry.details = details != null ? details.toString() : "{}";

                            logEntries.add(logEntry);
                        }
                        logsAdapter.notifyDataSetChanged();
                        if (logEntries.isEmpty()) {
                            Toast.makeText(LogsActivity.this, "No logs found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("LogsActivity", "Error fetching logs", task.getException());
                        Toast.makeText(LogsActivity.this, "Failed to fetch logs", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Data model for log entries
    public static class LogEntry {
        long timestamp;
        String eventType;
        String message;
        String details;
    }
}