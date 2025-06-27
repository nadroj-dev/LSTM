package com.otis.lstm;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddDeviceActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private DeviceAdapter deviceAdapter;
    private ListenerRegistration deviceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        // Setup RecyclerView
        RecyclerView deviceList = findViewById(R.id.deviceList);
        deviceList.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new DeviceAdapter();
        deviceList.setAdapter(deviceAdapter);

        // Fetch devices
        fetchDevices();

        // Setup dialog button
        Button openDialogBtn = findViewById(R.id.openAddDeviceDialogBtn);
        openDialogBtn.setOnClickListener(v -> showAddDeviceDialog());
    }

    private void fetchDevices() {
        String userId = mAuth.getCurrentUser().getUid();
        deviceListener = firestore.collection("Devices")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(AddDeviceActivity.this, "Failed to fetch devices", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ArrayList<Device> devices = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Device device = doc.toObject(Device.class);
                        devices.add(device);
                    }
                    deviceAdapter.setDeviceList(devices);
                });
    }

    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.dialog_add_device);

        AlertDialog dialog = builder.create();
        dialog.show();

        EditText deviceIdInput = dialog.findViewById(R.id.deviceIdInput);
        Button cancelBtn = dialog.findViewById(R.id.cancelBtn);
        Button addDeviceBtn = dialog.findViewById(R.id.addDeviceConfirmBtn);

        if (cancelBtn != null) {
            cancelBtn.setOnClickListener(v -> dialog.dismiss());
        }

        if (addDeviceBtn != null) {
            addDeviceBtn.setOnClickListener(v -> {
                String deviceId = deviceIdInput != null ? deviceIdInput.getText().toString().trim() : "";
                if (TextUtils.isEmpty(deviceId)) {
                    Toast.makeText(AddDeviceActivity.this, "Please enter a Device ID", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> device = new HashMap<>();
                device.put("deviceId", deviceId);
                device.put("userId", mAuth.getCurrentUser().getUid());
                device.put("timestamp", com.google.firebase.Timestamp.now());

                firestore.collection("Devices")
                        .add(device)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(AddDeviceActivity.this, "Device added successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AddDeviceActivity.this, "Failed to add device", Toast.LENGTH_SHORT).show();
                        });
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceListener != null) {
            deviceListener.remove();
        }
    }
}