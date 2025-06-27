package com.otis.lstm;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class FanDurationWorker extends Worker {

    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    public FanDurationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        String[] fanNames = getInputData().getStringArray("fanNames");
        if (fanNames == null) {
            Log.e("FanDurationWorker", "No fan names provided");
            return Result.failure();
        }

        for (String fanName : fanNames) {
            // For simplicity, assume duration is passed or calculated elsewhere
            // In a real implementation, you'd need to access MainActivity's fanStates
            // This example logs a placeholder duration; replace with actual logic
            long durationSeconds = 0; // Placeholder: Replace with actual duration calculation

            // Get the date for the log (midnight of the current day)
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Timestamp logDate = new Timestamp(calendar.getTime());

            // Log to Firestore
            Map<String, Object> log = new HashMap<>();
            log.put("fanName", fanName);
            log.put("durationSeconds", durationSeconds);
            log.put("date", logDate);
            log.put("userId", mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "unknown");

            firestore.collection("FanLogs")
                    .add(log)
                    .addOnSuccessListener(documentReference -> Log.d("FanDurationWorker", "Logged duration for " + fanName + ": " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.e("FanDurationWorker", "Failed to log duration for " + fanName, e));
        }

        return Result.success();
    }
}