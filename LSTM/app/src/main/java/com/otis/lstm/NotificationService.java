package com.otis.lstm;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class NotificationService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Code to handle background tasks, such as sending notifications
        return START_STICKY; // Or START_NOT_STICKY depending on your use case
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
