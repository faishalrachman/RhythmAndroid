package com.faishalrachman.amonsecg.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.faishalrachman.amonsecg.AppSetting;
import com.faishalrachman.amonsecg.R;
import com.faishalrachman.amonsecg.utils.NotificationHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.DeviceCallback;

public class CoreService extends Service {
    public int ID = 1;
    public String CHANNEL_ID = "10001";
    private Context ctx;
    private static String deviceName;
    public static Bluetooth bluetooth;
    NotificationHelper notificationHelper;
    ArrayList<String> messageList = new ArrayList<>();
    static Notification notif;
    static String ecgData = "";

    String TAG = "CoreService";


    public CoreService(Context appContext) {
        super();
        this.deviceName = AppSetting.getBluetoothDeviceName(appContext);
        Log.d(TAG, "CoreService: Started");
        Log.d(TAG, "CoreService: " + this.deviceName);
    }
    public CoreService() {

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        ctx = getApplicationContext();
        if (AppSetting.isLoggedIn(ctx)) {
            Log.d(TAG, "onCreate: " + AppSetting.isLoggedIn(ctx));
            Log.d(TAG, "onCreate: Bluetooth Setup");
            if (bluetooth == null)
                bluetooth = new Bluetooth(ctx);
            if (!bluetooth.isConnected()) {
                Log.d(TAG, "onCreate: Tidak Konek Anjay");
                setupBluetooth();
            }
//        this.notificationHelper = new NotificationHelper(ctx);
            notif = getNotification("Bluetooth is Connected");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "onCreate: Foreground");
                super.startForeground(ID, notif);
//            notificationHelper.sendNotification("Rhythm","Service Started");
            }
        } else {
            stopForeground(true);
            System.exit(0);
        }

    }
    private Notification getNotification(String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_yes)
                .setContentTitle("Rhythm")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_LOW);
//        Intent detail = new Intent(this, DetailActivity.class);
//        builder.setContentIntent(PendingIntent.getActivity(ctx,0,detail,PendingIntent.FLAG_UPDATE_CURRENT));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SERVICE", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);

        }
        return builder.build();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("EXIT", "ondestroy!");
//        if (AppSetting.isLoggedIn(ctx)) {
        Log.d(TAG, "onDestroy: Restarting");
        Intent broadcastIntent = new Intent(this, CoreRestarter.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "onDestroy: stopForeground");
            stopForeground(true);
        }

            sendBroadcast(broadcastIntent);
//        }
        saveECGSignal();
    }

    void saveECGSignal() {

        File folder;
        if (Environment.getExternalStorageState() != null) {
            folder = new File(Environment.getExternalStorageDirectory() + "/data/ECGRecord");

        } else {
            folder = new File(Environment.getDataDirectory() + "/ECGRecord");
        }

        if (!folder.exists()) {
            boolean mkdir = folder.mkdirs();
            Log.d(TAG, "saveECGSignal: " + mkdir);
        }


        if (ecgData.length() > 2500) {
            Log.d(TAG, "saveECGSignal: " + folder.getPath());
            Long tsLong = System.currentTimeMillis() / 1000;
            String ts = tsLong.toString();
            String name = AppSetting.getBluetoothDeviceName(ctx);

            try {
//            File file = new File(context.getFilesDir(), filename);
                FileWriter writer = new FileWriter(folder.getPath() + "/" + ts + "-" + name + ".txt");
                writer.write(ecgData);
                writer.close();
                this.ecgData = "";
                Log.d(TAG, "saveECGSignal: SAVE SUCCESS - " + ts + "-" + name + ".txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // workaround for kitkat: set an alarm service to trigger service again
        if (AppSetting.isLoggedIn(ctx)) {
            Intent intent = new Intent(ctx, CoreService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 5000, pendingIntent);
        }
    }
    void setupBluetooth() {
        Log.d(TAG, "setupBluetooth: Inisialisasi Bluetooth");

        bluetooth.setDeviceCallback(new DeviceCallback() {
            @Override
            public void onDeviceConnected(BluetoothDevice device) {
                Log.d(TAG, "onDeviceConnected: " + device.getName());
                bluetooth.send("Connected");

                notif = getNotification("Bluetooth is connected");
                startForeground(ID, notif);

            }

            @Override
            public void onDeviceDisconnected(BluetoothDevice device, String message) {
                notif = getNotification("Bluetooth is disconnected");
                startForeground(ID, notif);
                reconnect();
                saveECGSignal();
            }

            @Override
            public void onMessage(String message) {
                Long tsLong = System.currentTimeMillis() / 1000;
                String ts = tsLong.toString();
                messageList.add(ts + ":" + message.replace("��", ""));
//                sendNotif();
                Log.d(TAG, "onMessage: " + ts + ":" + message.replace("��", ""));
                Intent local = new Intent();
                local.setAction("service.to.activity.transfer");
                local.putExtra("signal", message);
                ctx.sendBroadcast(local);

                ecgData += ts + ":" + message.replace("��", "") + "\n";
                if (ecgData.length() > 1073741824) {
                    saveECGSignal();
                }

            }

            @Override
            public void onError(int errorCode) {

                Log.d(TAG, "onError: " + errorCode);
                reconnect();
                saveECGSignal();
            }

            @Override
            public void onConnectError(BluetoothDevice device, String message) {
                Log.d(TAG, "onConnectError: " + message);
                reconnect();
                saveECGSignal();
            }
        });
        reconnect();
    }
//    void sendNotif() {
//        notificationHelper = new NotificationHelper(ctx);
//        notificationHelper.createNotification("App", "app");
//    }

    void reconnect() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        notif = getNotification("Bluetooth is disconnected");
        super.startForeground(ID, notif);
        String name = AppSetting.getBluetoothDeviceName(ctx);
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
