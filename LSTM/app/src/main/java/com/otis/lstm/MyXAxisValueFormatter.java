package com.otis.lstm;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MyXAxisValueFormatter extends ValueFormatter {
    private final List<Timestamp> timestamps;

    public MyXAxisValueFormatter(List<Timestamp> timestamps) {
        this.timestamps = timestamps;
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        int index = (int) value;
        if (index >= 0 && index < timestamps.size()) {
            Timestamp timestamp = timestamps.get(index);
            if (timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
                return sdf.format(timestamp.toDate());
            }
        }
        return "";
    }
}