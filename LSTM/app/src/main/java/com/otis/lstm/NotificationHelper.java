package com.otis.lstm;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "temperature_alert_channel";
    private Context mContext;

    // Constructor that accepts Context to interact with system services
    public NotificationHelper(Context context) {
        this.mContext = context;
    }

    // Method to show notification
    public void showNotification(Context context, String title, String message) {
        // Create an explicit intent for the NotificationDetailActivity
        Intent intent = new Intent(context, NotificationDetailActivity.class);
        intent.putExtra("notification_message", message); // Pass the message to the activity

        // Create a PendingIntent to trigger the activity when the notification is tapped
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Create a Notification Manager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Create the notification channel if running on Android Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Temperature Alerts";
            String description = "Notifications for temperature alerts";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)  // Use an appropriate icon for your notification
                .setAutoCancel(true)  // Dismiss the notification when tapped
                .setContentIntent(pendingIntent)  // Set the PendingIntent
                .build();

        // If running on Android 13 or higher, check for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, request permission
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
                return;  // Exit early, don't show the notification until permission is granted
            }
        }

        // Show the notification if permission is granted
        notificationManager.notify(1, notification);  // ID for the notification
    }
}
