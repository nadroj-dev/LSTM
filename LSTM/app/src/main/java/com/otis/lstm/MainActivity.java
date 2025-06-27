package com.otis.lstm;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private CardView cardHumidity;
    private CardView cardTemperature;
    private TextView humidityValue;
    private TextView temperatureValue, status;
    private BarChart barChart;
    private Switch toggleSwitch;
    private FirebaseAuth mAuth;
    private FirebaseDatabase realtimeDatabase;
    private FirebaseFirestore firestore;
    private DatabaseReference humidRef, tempRef, modeRef, fan1Ref, fan2Ref, fan3Ref, tempThresholdRef, humidThresholdRef;
    private Button buttonProfile;
    private ChipGroup chipGroup;
    private MediaPlayer mediaPlayer;

    private static class FanState {
        boolean isOn;
        long startTime;
        long totalOnDuration;
    }

    private final Map<String, FanState> fanStates = new HashMap<>();
    private static final String FAN_1 = "Fan 1";
    private static final String FAN_2 = "Fan 2";
    private static final String FAN_3 = "Fan 3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        realtimeDatabase = FirebaseDatabase.getInstance("https://lstm-d75ee-default-rtdb.asia-southeast1.firebasedatabase.app/");
        firestore = FirebaseFirestore.getInstance();

        buttonProfile = findViewById(R.id.buttonProfile);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        cardHumidity = findViewById(R.id.card_humidity);
        cardTemperature = findViewById(R.id.card_temperature);
        humidityValue = findViewById(R.id.humidity_value);
        temperatureValue = findViewById(R.id.temperature_value);
        barChart = findViewById(R.id.bar_chart);
        toggleSwitch = findViewById(R.id.toggle_switch);
        status = findViewById(R.id.status);

        chipGroup = findViewById(R.id.chipGroup);

        Chip chip1 = findViewById(R.id.chip1);
        Chip chip2 = findViewById(R.id.chip2);
        Chip chip3 = findViewById(R.id.chip3);

        final int defaultColor = getResources().getColor(R.color.purple_200);
        final int redColor = getResources().getColor(R.color.red);

        fanStates.put(FAN_1, new FanState());
        fanStates.put(FAN_2, new FanState());
        fanStates.put(FAN_3, new FanState());

        chip1.setOnClickListener(v -> toggleChipColor(chip1, defaultColor, redColor));
        chip2.setOnClickListener(v -> toggleChipColor(chip2, defaultColor, redColor));
        chip3.setOnClickListener(v -> toggleChipColor(chip3, defaultColor, redColor));

        humidRef = realtimeDatabase.getReference("DHTHumid/currentHumid");
        tempRef = realtimeDatabase.getReference("DHTTemp/currentTemp");
        tempThresholdRef = realtimeDatabase.getReference("tempThreshold");
        humidThresholdRef = realtimeDatabase.getReference("humidThreshold");
        modeRef = realtimeDatabase.getReference("mode");
        fan1Ref = realtimeDatabase.getReference("Fans/fan1");
        fan2Ref = realtimeDatabase.getReference("Fans/fan2");
        fan3Ref = realtimeDatabase.getReference("Fans/fan3");

        initializeFanStates();

        fetchDataFromRealtimeDatabase();

        toggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                status.setText("Automatic");
                chipGroup.setVisibility(ChipGroup.GONE);
                modeRef.setValue("automatic")
                        .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Mode set to automatic"))
                        .addOnFailureListener(e -> Log.e("MainActivity", "Failed to set mode to automatic", e));
            } else {
                status.setText("Manual");
                chipGroup.setVisibility(ChipGroup.VISIBLE);
                modeRef.setValue("manual")
                        .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Mode set to manual"))
                        .addOnFailureListener(e -> Log.e("MainActivity", "Failed to set mode to manual", e));
            }
        });

        buttonProfile.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        fetchLatestSensorData();

        startPeriodicWork();
        scheduleFanDurationLogging();
    }

    private void initializeFanStates() {
        fan1Ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Integer value = dataSnapshot.getValue(Integer.class);
                if (value != null && value == 1) {
                    FanState state = fanStates.get(FAN_1);
                    state.isOn = true;
                    state.startTime = System.currentTimeMillis();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to read Fan 1 initial state", databaseError.toException());
            }
        });

        fan2Ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Integer value = dataSnapshot.getValue(Integer.class);
                if (value != null && value == 1) {
                    FanState state = fanStates.get(FAN_2);
                    state.isOn = true;
                    state.startTime = System.currentTimeMillis();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to read Fan 2 initial state", databaseError.toException());
            }
        });

        fan3Ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Integer value = dataSnapshot.getValue(Integer.class);
                if (value != null && value == 1) {
                    FanState state = fanStates.get(FAN_3);
                    state.isOn = true;
                    state.startTime = System.currentTimeMillis();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to read Fan 3 initial state", databaseError.toException());
            }
        });
    }

    private void toggleChipColor(Chip chip, int defaultColor, int redColor) {
        int currentColor = chip.getChipBackgroundColor().getDefaultColor();
        DatabaseReference fanRefToUpdate = null;
        String fanName = "";
        FanState fanState = null;

        if (chip.getId() == R.id.chip1) {
            fanRefToUpdate = fan1Ref;
            fanName = FAN_1;
            fanState = fanStates.get(FAN_1);
        } else if (chip.getId() == R.id.chip2) {
            fanRefToUpdate = fan2Ref;
            fanName = FAN_2;
            fanState = fanStates.get(FAN_2);
        } else if (chip.getId() == R.id.chip3) {
            fanRefToUpdate = fan3Ref;
            fanName = FAN_3;
            fanState = fanStates.get(FAN_3);
        }

        final FanState finalFanState = fanState;
        final String finalFanName = fanName;

        if (currentColor == redColor) {
            chip.setChipBackgroundColorResource(R.color.purple_200);
            if (fanRefToUpdate != null && finalFanState != null) {
                fanRefToUpdate.setValue(0)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("MainActivity", finalFanName + " turned off");
                            playSound(R.raw.fan_off);
                            sendFanNotification(finalFanName, false);
                            if (finalFanState.isOn) {
                                finalFanState.isOn = false;
                                long currentTime = System.currentTimeMillis();
                                finalFanState.totalOnDuration += currentTime - finalFanState.startTime;
                                finalFanState.startTime = 0;
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("MainActivity", "Failed to turn off " + finalFanName, e);
                            Toast.makeText(MainActivity.this, "Failed to turn off " + finalFanName, Toast.LENGTH_SHORT).show();
                        });
            }
        } else {
            chip.setChipBackgroundColorResource(R.color.red);
            if (fanRefToUpdate != null && finalFanState != null) {
                fanRefToUpdate.setValue(1)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("MainActivity", finalFanName + " turned on");
                            playSound(R.raw.fan_on);
                            sendFanNotification(finalFanName, true);
                            if (!finalFanState.isOn) {
                                finalFanState.isOn = true;
                                finalFanState.startTime = System.currentTimeMillis();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("MainActivity", "Failed to turn on " + finalFanName, e);
                            Toast.makeText(MainActivity.this, "Failed to turn on " + finalFanName, Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }

    private void sendFanNotification(String fanName, boolean isOn) {
        String title = isOn ? fanName + " Turned On" : fanName + " Turned Off";
        String message = isOn ? fanName + " is now ON." : fanName + " is now OFF.";
        new NotificationHelper(MainActivity.this).showNotification(MainActivity.this, title, message);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> log = new HashMap<>();
            log.put("eventType", "Fan State Change");
            log.put("message", message);
            log.put("timestamp", Timestamp.now());
            log.put("userId", currentUser.getUid());
            log.put("details", new HashMap<String, Object>() {{
                put("fanName", fanName);
                put("state", isOn ? "ON" : "OFF");
            }});

            firestore.collection("Logs")
                    .add(log)
                    .addOnSuccessListener(documentReference -> Log.d("MainActivity", "Fan notification logged: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.e("MainActivity", "Failed to log fan notification", e));
        }
    }

    private void scheduleFanDurationLogging() {
        PeriodicWorkRequest fanDurationWorkRequest =
                new PeriodicWorkRequest.Builder(FanDurationWorker.class, 24, TimeUnit.HOURS)
                        .setInputData(new Data.Builder()
                                .putStringArray("fanNames", new String[]{FAN_1, FAN_2, FAN_3})
                                .build())
                        .build();

        WorkManager.getInstance(this).enqueue(fanDurationWorkRequest);
    }

    private void playSound(int soundResourceId) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = MediaPlayer.create(this, soundResourceId);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> mp.release());
            mediaPlayer.start();
        } else {
            Log.e("MainActivity", "Failed to create MediaPlayer for sound resource: " + soundResourceId);
        }
    }

    private void fetchDataFromRealtimeDatabase() {
        humidRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Double humidity = dataSnapshot.getValue(Double.class);
                if (humidity != null) {
                    updateHumidityCard(humidity.intValue());
                    checkHumidityThreshold(humidity.intValue());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to read humidity data.", Toast.LENGTH_SHORT).show();
            }
        });

        tempRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Double temperature = dataSnapshot.getValue(Double.class);
                if (temperature != null) {
                    updateTemperatureCard(temperature.intValue());
                    checkTemperatureThreshold(temperature.intValue());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to read temperature data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkTemperatureThreshold(int temperature) {
        tempThresholdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Integer tempThreshold = dataSnapshot.getValue(Integer.class);
                if (tempThreshold != null && temperature > tempThreshold) {
                    sendTemperatureNotification(temperature, tempThreshold);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to check temperature threshold", databaseError.toException());
            }
        });
    }

    private void checkHumidityThreshold(int humidity) {
        humidThresholdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Integer humidThreshold = dataSnapshot.getValue(Integer.class);
                if (humidThreshold != null && humidity > humidThreshold) {
                    sendHumidityNotification(humidity, humidThreshold);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to check humidity threshold", databaseError.toException());
            }
        });
    }

    private void sendTemperatureNotification(int temperature, int threshold) {
        String mode = status.getText().toString();
        String message = mode.equals("Automatic") ?
                "LSTM: Automatic Mode - The temperature has exceeded the threshold. Fans are switched ON automatically." :
                "LSTM: Manual Mode - The current temperature in the farm exceeds " + threshold + "°C. Don't forget to set the fan!";
        new NotificationHelper(MainActivity.this).showNotification(MainActivity.this, "Temperature Alert", message);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> log = new HashMap<>();
            log.put("eventType", "Temperature Alert");
            log.put("message", message);
            log.put("timestamp", Timestamp.now());
            log.put("userId", currentUser.getUid());
            log.put("details", new HashMap<String, Object>() {{
                put("temperature", temperature);
                put("threshold", threshold);
                put("mode", mode);
            }});

            firestore.collection("Logs")
                    .add(log)
                    .addOnSuccessListener(documentReference -> Log.d("MainActivity", "Temperature notification logged: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.e("MainActivity", "Failed to log temperature notification", e));
        }
    }

    private void sendHumidityNotification(int humidity, int threshold) {
        String mode = status.getText().toString();
        String message = mode.equals("Automatic") ?
                "LSTM: Automatic Mode - The humidity has exceeded the threshold. Fans are switched ON automatically." :
                "LSTM: Manual Mode - The current humidity in the farm exceeds " + threshold + "%.";
        new NotificationHelper(MainActivity.this).showNotification(MainActivity.this, "Humidity Alert", message);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> log = new HashMap<>();
            log.put("eventType", "Humidity Alert");
            log.put("message", message);
            log.put("timestamp", Timestamp.now());
            log.put("userId", currentUser.getUid());
            log.put("details", new HashMap<String, Object>() {{
                put("humidity", humidity);
                put("threshold", threshold);
                put("mode", mode);
            }});

            firestore.collection("Logs")
                    .add(log)
                    .addOnSuccessListener(documentReference -> Log.d("MainActivity", "Humidity notification logged: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.e("MainActivity", "Failed to log humidity notification", e));
        }
    }

    private void updateHumidityCard(int humidity) {
        humidityValue.setText(humidity + "%");
        cardHumidity.setCardBackgroundColor(getColorForPercentage(humidity));
    }

    private void updateTemperatureCard(int temperature) {
        temperatureValue.setText(temperature + "°C");
        cardTemperature.setCardBackgroundColor(getColorForTemperature(temperature));
    }

    private void setupBarChart(ArrayList<BarEntry> humidityEntries, ArrayList<BarEntry> temperatureEntries, ArrayList<Timestamp> timestamps) {
        BarDataSet humidityDataSet = new BarDataSet(humidityEntries, "Humidity");
        humidityDataSet.setColor(Color.BLUE);
        humidityDataSet.setValueTextColor(Color.BLACK);
        humidityDataSet.setValueTextSize(16f);

        BarDataSet temperatureDataSet = new BarDataSet(temperatureEntries, "Temperature");
        temperatureDataSet.setColor(Color.RED);
        temperatureDataSet.setValueTextColor(Color.BLACK);
        temperatureDataSet.setValueTextSize(16f);

        BarData barData = new BarData(humidityDataSet, temperatureDataSet);
        barChart.setData(barData);
        barChart.getXAxis().setValueFormatter(new MyXAxisValueFormatter(timestamps));
        barChart.getXAxis().setDrawLabels(true);
        barChart.getAxisLeft().setDrawLabels(true);
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void fetchLatestSensorData() {
        firestore.collection("sensorData")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<BarEntry> humidityEntries = new ArrayList<>();
                        ArrayList<BarEntry> temperatureEntries = new ArrayList<>();
                        ArrayList<Timestamp> timestamps = new ArrayList<>();
                        int index = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Double humidity = document.getDouble("humidity");
                            Double temperature = document.getDouble("temperature");
                            Timestamp timestamp = document.getTimestamp("timestamp");

                            if (humidity != null && temperature != null && timestamp != null) {
                                humidityEntries.add(new BarEntry(index, humidity.floatValue()));
                                temperatureEntries.add(new BarEntry(index, temperature.floatValue()));
                                timestamps.add(timestamp);
                                index++;
                            }
                        }

                        setupBarChart(humidityEntries, temperatureEntries, timestamps);
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to fetch sensor data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private int getColorForPercentage(int percentage) {
        int red = (int) (255 * (percentage / 100.0));
        int green = (int) (255 * ((100 - percentage) / 100.0));
        return Color.rgb(red, green, 255);
    }

    private int getColorForTemperature(int temperature) {
        int red = (int) (255 * ((temperature - 15) / 25.0));
        int blue = (int) (255 * (1 - (temperature - 15) / 25.0));
        return Color.rgb(red, 0, blue);
    }

    public void startPeriodicWork() {
        PeriodicWorkRequest temperatureCheckWorkRequest =
                new PeriodicWorkRequest.Builder(TemperatureCheckWorker.class, 15, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueue(temperatureCheckWorkRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. Now notifications can be shown.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. Notifications cannot be shown.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        for (String fanName : fanStates.keySet()) {
            FanState state = fanStates.get(fanName);
            if (state.isOn) {
                long currentTime = System.currentTimeMillis();
                state.totalOnDuration += currentTime - state.startTime;
                state.startTime = currentTime;
            }
        }
    }
}