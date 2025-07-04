package com.example.client;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;
import androidx.work.PeriodicWorkRequest;
import androidx.work.ExistingPeriodicWorkPolicy;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeviceWorker extends Worker {
    private DatabaseReference dbRef;

    public DeviceWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        dbRef = FirebaseDatabase.getInstance().getReference("devices");
    }

    @NonNull
    @Override
    public Result doWork() {
        String deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        int batteryLevel = getBatteryLevel();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        dbRef.child(deviceId).child("battery").setValue(batteryLevel);
        dbRef.child(deviceId).child("last_seen").setValue(timestamp);

        try {
            FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
            locationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    dbRef.child(deviceId).child("location").setValue(location.getLatitude() + "," + location.getLongitude());
                }
            });
        } catch (Exception e) {
            Log.e("Location", "Failed: " + e.getMessage());
        }

        dbRef.child(deviceId).child("command").get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String cmd = snapshot.getValue(String.class);
                if ("ping".equals(cmd)) {
                    dbRef.child(deviceId).child("response").setValue("pong");
                } else if ("get_location".equals(cmd)) {
                    FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                    locationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            String loc = location.getLatitude() + "," + location.getLongitude();
                            dbRef.child(deviceId).child("response").setValue(loc);
                        }
                    });
                }
            }
        });

        return Result.success();
    }

    private int getBatteryLevel() {
        BatteryManager bm = (BatteryManager) getApplicationContext().getSystemService(Context.BATTERY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return -1;
    }
}
