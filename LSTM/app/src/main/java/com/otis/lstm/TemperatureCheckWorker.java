package com.otis.lstm;

import android.content.Context;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TemperatureCheckWorker extends Worker {

    private FirebaseDatabase mDatabase;
    private DatabaseReference tempRef, tempThresholdRef;

    public TemperatureCheckWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
        mDatabase = FirebaseDatabase.getInstance();
        tempRef = mDatabase.getReference("DHTTemp/currentTemp");  // Reference for current temperature
        tempThresholdRef = mDatabase.getReference("tempThreshold");  // Reference for temperature threshold
    }

    @Override
    public Result doWork() {
        // Fetch temperature and threshold data from Firebase Realtime Database
        fetchTemperatureData();

        return Result.success();
    }

    private void fetchTemperatureData() {
        // Fetch the current temperature from Firebase Realtime Database
        tempRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Double temperature = dataSnapshot.getValue(Double.class);

                if (temperature != null) {
                    // Now fetch the temperature threshold
                    fetchTemperatureThreshold(temperature);
                } else {
                    Log.e("TemperatureCheckWorker", "Failed to fetch current temperature.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("TemperatureCheckWorker", "Error reading current temperature data", databaseError.toException());
            }
        });
    }

    private void fetchTemperatureThreshold(final double currentTemperature) {
        // Fetch the temperature threshold from Firebase Realtime Database
        tempThresholdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Integer threshold = dataSnapshot.getValue(Integer.class);

                if (threshold != null) {
                    // If temperature exceeds the threshold, send notification
                    if (currentTemperature > threshold) {
                        sendTemperatureNotification(currentTemperature, threshold);
                    }
                } else {
                    Log.e("TemperatureCheckWorker", "Failed to fetch temperature threshold.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("TemperatureCheckWorker", "Error reading temperature threshold", databaseError.toException());
            }
        });
    }

    private void sendTemperatureNotification(double currentTemperature, int threshold) {
        // Create the initial message
        final String[] message = { "LSTM: Manual Mode - The current temperature in the farm exceeds " + threshold + "°C. Don't forget to set the fan!" };

        // Assuming the mode (Automatic/Manual) can be retrieved from the database
        getModeAndSendNotification(message, currentTemperature, threshold);
    }

    private void getModeAndSendNotification(final String[] message, final double currentTemperature, final int threshold) {
        DatabaseReference modeRef = mDatabase.getReference("mode");

        modeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String mode = dataSnapshot.getValue(String.class);

                if (mode != null) {
                    if (mode.equals("automatic")) {
                        message[0] = "LSTM: Automatic Mode - The temperature has exceeded the threshold. Fans are switched ON automatically.";
                    }
                }

                // Send notification with the updated message
                new NotificationHelper(getApplicationContext()).showNotification(getApplicationContext(), "Temperature Alert", message[0]);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("TemperatureCheckWorker", "Error reading mode data", databaseError.toException());
            }
        });
    }
}
