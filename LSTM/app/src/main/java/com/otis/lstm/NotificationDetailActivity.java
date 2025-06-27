package com.otis.lstm;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class NotificationDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        TextView messageTextView = findViewById(R.id.messageTextView);
        Button buttonBack = findViewById(R.id.buttonBack);

        String message = getIntent().getStringExtra("notification_message");
        String notificationType = getIntent().getStringExtra("notification_type");
        String fanName = getIntent().getStringExtra("fan_name");

        if (message != null) {
            if ("fan".equals(notificationType) && fanName != null) {
                // Format fan-specific message
                String formattedMessage = String.format("%s Notification\n\n%s", fanName, message);
                messageTextView.setText(formattedMessage);
            } else if ("temperature".equals(notificationType)) {
                messageTextView.setText("Temperature Alert\n\n" + message);
            } else if ("humidity".equals(notificationType)) {
                messageTextView.setText("Humidity Alert\n\n" + message);
            } else {
                // Fallback for unrecognized types
                messageTextView.setText(message);
            }
        } else {
            messageTextView.setText("No notification details available.");
        }

        buttonBack.setOnClickListener(v -> finish());
    }
}