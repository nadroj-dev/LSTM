package com.otis.lstm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<Device> deviceList;

    public DeviceAdapter() {
        this.deviceList = new ArrayList<>();
    }

    public void setDeviceList(List<Device> deviceList) {
        this.deviceList = deviceList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.deviceIdTextView.setText(device.getDeviceId());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = device.getTimestamp() != null ? sdf.format(device.getTimestamp().toDate()) : "N/A";
        holder.timestampTextView.setText(timestamp);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceIdTextView;
        TextView timestampTextView;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceIdTextView = itemView.findViewById(R.id.deviceIdTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }
}