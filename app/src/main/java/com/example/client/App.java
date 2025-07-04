package com.example.client;

import android.app.Application;
import androidx.work.*;
import java.util.concurrent.TimeUnit;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(DeviceWorker.class, 15, TimeUnit.MINUTES).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("DeviceUpdate", ExistingPeriodicWorkPolicy.KEEP, workRequest);
    }
}
