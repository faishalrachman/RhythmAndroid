package com.faishalrachman.amonsecg.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.faishalrachman.amonsecg.AppSetting;
import com.faishalrachman.amonsecg.DetailActivity;
import com.faishalrachman.amonsecg.R;
import com.faishalrachman.amonsecg.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.DeviceCallback;

public class CoreService extends Service {
    public int counter=0;
    private Context ctx;
    private static String deviceName;
    Bluetooth bluetooth;
    NotificationHelper notificationHelper;
    ArrayList<String> messageList = new ArrayList<>();
    Notification notif;

    String TAG = "CoreService";


    public CoreService(Context appContext) {
        super();

        this.ctx = appContext;
        this.notificationHelper = new NotificationHelper(appContext);
        this.deviceName = AppSetting.getBluetoothDeviceName(appContext);
        Log.d(TAG, "CoreService: Started");
        Log.d(TAG, "CoreService: "+this.deviceName);
    }
    public CoreService(){

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Bluetooth Setup");
        setupBluetooth();
        notif = new Notification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(0,notif);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("EXIT", "ondestroy!");
        Log.d(TAG, "onDestroy: Restarting");
        Intent broadcastIntent = new Intent(this, CoreRestarter.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // workaround for kitkat: set an alarm service to trigger service again
        Intent intent = new Intent(getApplicationContext(), CoreService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 5000, pendingIntent);
    }


    void setupBluetooth() {
        Log.d(TAG, "setupBluetooth: Inisialisasi Bluetooth");
        bluetooth = new Bluetooth(getApplicationContext());
        bluetooth.setDeviceCallback(new DeviceCallback() {
            @Override
            public void onDeviceConnected(BluetoothDevice device) {
                Log.d(TAG, "onDeviceConnected: " + device.getName());
                bluetooth.send("Connected");
            }

            @Override
            public void onDeviceDisconnected(BluetoothDevice device, String message) {
                reconnect();
            }

            @Override
            public void onMessage(String message) {
                Long tsLong = System.currentTimeMillis()/1000;
                String ts = tsLong.toString();
                messageList.add(ts+":"+message.replace("��",""));
//                sendNotif();
                Log.d(TAG, "onMessage: "+ts+":"+message.replace("��",""));
                Intent local = new Intent();
                local.setAction("service.to.activity.transfer");
                local.putExtra("signal", message);
                getApplicationContext().sendBroadcast(local);
            }

            @Override
            public void onError(int errorCode) {

                Log.d(TAG, "onError: " + errorCode);
                reconnect();
            }

            @Override
            public void onConnectError(BluetoothDevice device, String message) {
                Log.d(TAG, "onConnectError: " + message);
                reconnect();
            }
        });
        reconnect();
    }
    void sendNotif(){
        notificationHelper = new NotificationHelper(getApplicationContext());
        notificationHelper.createNotification("App","app");
    }
    void reconnect() {
        String name = AppSetting.getBluetoothDeviceName(getApplicationContext());
        connectBluetooth(name);
    }

    void connectBluetooth(String name) {
        bluetooth.onStart();
        if (bluetooth.isEnabled()) {
            Log.d(TAG, "connectBluetooth: Connecting to" + name);
            bluetooth.connectToName(name);
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
