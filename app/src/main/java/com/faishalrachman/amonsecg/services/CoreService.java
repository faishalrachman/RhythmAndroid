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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.faishalrachman.amonsecg.AppSetting;
import com.faishalrachman.amonsecg.R;
import com.faishalrachman.amonsecg.algo.ECGClassification;
import com.faishalrachman.amonsecg.utils.IntentHelper;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.DeviceCallback;

public class CoreService extends Service {
    private static int ID = 1;
    private static String CHANNEL_ID = "10001";
    private Context ctx;
    private static String deviceName;
    public static Bluetooth bluetooth;
//    ArrayList<String> messageList = new ArrayList<>();
    static Notification notif;
    static String ecgData = "";
    static MqttAndroidClient mqttClient;
    static MqttCallbackExtended callback;
    static String filename;
    public static boolean is_recording = false;
    String TAG = "CoreService";
    private static ArrayList<Float> ecg_signal;
    private static ECGClassification clf = new ECGClassification();



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
        if (!is_recording){
            uploadECGSignal();
        }
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
            if (mqttClient == null)
            mqttClient = AppSetting.getMqttClient(ctx);
            if (callback == null){
                callback = new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        sendToast("Connected to MQTT Server");
                        Log.d(TAG, "connectComplete: ");
                    }

                    @Override
                    public void connectionLost(Throwable cause) {

                        Log.d(TAG, "connectionLost: ");
                        sendToast("MQTT Connection Lost");
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        Log.d(TAG, "messageArrived: "+message.getPayload().toString());
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                };
                mqttClient.setCallback(callback);
            }
            if (!mqttClient.isConnected()){
//                try {
////                    MqttConnectOptions options = new MqttConnectOptions();
////                    options.
//                    mqttClient.connect(ctx, new IMqttActionListener() {
//                        @Override
//                        public void onSuccess(IMqttToken asyncActionToken) {
//                            Log.d(TAG, "onSuccess: MQTT Success");
//
//                            String deviceName = AppSetting.getBluetoothDeviceName(ctx);
//                            String[] topics = new String[2];
//                            topics[0] = "rhythm/"+deviceName+"/ecg";
//                            topics[1] = "rhythm/"+deviceName+"/n";
//                            int[] qos = {0,0};
//                            try {
//                                mqttClient.subscribe(topics,qos);
//                            } catch (MqttException e) {
//                                Log.d(TAG, "onSuccess: Failed Subscribe");
//                                e.printStackTrace();
//                            }
//                            Log.d(TAG, "onCreate: Subscribed");
//                        }
//
//                        @Override
//                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                            Log.d(TAG, "onFailure: MQTT Failed");
//                            Log.d(TAG, "onFailure: "+exception.getMessage());
//                        }
//                    });
//
//
//                } catch (MqttException e) {
//                    e.printStackTrace();
//                }
                MqttConnectOptions options = new MqttConnectOptions();
                options.setAutomaticReconnect(true);
                try {
                    Toast.makeText(ctx, "Connecting MQTT to SERVER", Toast.LENGTH_SHORT).show();
                    mqttClient.connect(options, ctx, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                                Log.d(TAG, "onSuccess: MQTT Success ");
                                Log.d(TAG, "onSuccess: tidak ada subscribe2an");
                                sendToast("MQTT Connected");
                                uploadECGSignal();

//                                String deviceName = AppSetting.getBluetoothDeviceName(ctx);
//                                String[] topics = new String[2];
//                                topics[0] = "rhythm/"+deviceName+"/ecg";
//                                topics[1] = "rhythm/"+deviceName+"/n";
//                                int[] qos = {0,0};
//                                try {
//                                    mqttClient.subscribe(topics,qos);
//                                } catch (MqttException e) {
//                                    Log.d(TAG, "onSuccess: Failed Subscribe");
//                                    e.printStackTrace();
//                                }
//                                Log.d(TAG, "onCreate: Subscribed");

                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        } else {
            stopForeground(true);
            System.exit(0);
        }
        ecg_signal = new ArrayList<>();

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

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Monitoring Notification", NotificationManager.IMPORTANCE_LOW);
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
//        saveECGSignal();
    }

    public void saveECGSignal() {

        filename = AppSetting.getRecordFilename(getApplicationContext());
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
//            Long tsLong = System.currentTimeMillis() / 1000;
//            String ts = tsLong.toString();
//            String name = AppSetting.getBluetoothDeviceName(ctx);

            try {
                Log.d(TAG, "saveECGSignal: "+filename);
//            File file = new File(context.getFilesDir(), filename);
//                FileWriter writer = new FileWriter(folder.getPath() + "/" + ts + "-" + name + ".txt");
//                FileOutputStream fOut = openFileOutput(folder.getPath() + "/" + filename + ".txt",  MODE_APPEND);
                FileOutputStream fOut = new FileOutputStream(new File(folder.getPath() + "/" + filename + ".txt"),true);//openFileOutput(folder.getPath() + "/" + filename + ".txt",  MODE_APPEND);
                fOut.write(ecgData.getBytes());
                fOut.close();
//                writer.write(ecgData);
//                writer.close();
                ecgData = "";
                Log.d(TAG, "saveECGSignal: SAVE SUCCESS - " + filename + ".txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!is_recording)
            uploadECGSignal();
    }
    public void uploadECGSignal(){

        File folder;
        if (Environment.getExternalStorageState() != null) {
            folder = new File(Environment.getExternalStorageDirectory() + "/data/ECGRecord");

        } else {
            folder = new File(Environment.getDataDirectory() + "/ECGRecord");
        }
        File[] datas = folder.listFiles();
        if (datas != null) {
            for (final File data : datas) {
                Log.d(TAG, "uploadECGSignal: " + data.getName());

                AndroidNetworking.upload(AppSetting.getHttpAddress(CoreService.this) + "patient/add_record")
                        .addMultipartFile("record", data)
                        .addHeaders("Authorization", AppSetting.getSession(CoreService.this))
                        .setTag("uploadTest")
                        .setPriority(Priority.HIGH)
                        .build()
                        .setUploadProgressListener(new UploadProgressListener() {
                            @Override
                            public void onProgress(long bytesUploaded, long totalBytes) {
                                Log.d(TAG, "onProgress: " + data.getName() + "-" + (bytesUploaded / totalBytes) * 100 + "%");
                            }
                        })
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                                data.delete();
                            }

                            @Override
                            public void onError(ANError error) {
                                Log.d(TAG, "onError: ERRORLUR");
                            }
                        });
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
                sendToast(device.getName() + " is connected");
                Log.d(TAG, "onDeviceConnected: " + device.getName());
                bluetooth.send("Connected");

                notif = getNotification("Bluetooth is connected");
                startForeground(ID, notif);

            }

            @Override
            public void onDeviceDisconnected(BluetoothDevice device, String message) {
                Toast.makeText(ctx, "Bluetooth is disconnected", Toast.LENGTH_SHORT).show();
                notif = getNotification("Bluetooth is disconnected");
                startForeground(ID, notif);
                reconnect();
                saveECGSignal();
                sendToast(device.getName() + " is disconnected");
            }

            @Override
            public void onMessage(String message) {
                is_recording = AppSetting.getRecordingStatus(getApplicationContext());
                Log.d(TAG, "onMessage: isRecording="+is_recording);
                if (is_recording) {
                    Long tsLong = System.currentTimeMillis() / 1000;
                    String ts = tsLong.toString();
//                    messageList.add(ts + ":" + message.replace("��", ""));
//                sendNotif();
                    Log.d(TAG, "onMessage: " + ts + ":" + message.replace("��", ""));
                    Intent local = new Intent();
                    local.setAction("service.to.activity.transfer");
                    local.putExtra("signal", message);
                    clf.parseSignal(ecg_signal, message.replace("��", ""));
                    Log.d(TAG, "onMessage: ECG_Array = " + ecg_signal.size());
                    if (ecg_signal.size() > 2000) {

                        clf.run(ecg_signal);
                        int[] count = clf.getClass_count();
                        String penyakit = "normal";
                        if (count[2] > 0) {
                            penyakit = "vf";
                        } else if (count[1] > 0.5 * count[0]) {
                            penyakit = "af";
                        }
                        int HR = clf.HR;
                        Intent classification = new Intent();
                        classification.setAction("publish.classification");
                        classification.putExtra("notification", penyakit);
                        classification.putExtra("hr", HR);

                        Log.d(TAG, "onMessage: Penyakit=" + penyakit);
                        Log.d(TAG, "onMessage: HR=" + HR);
                        saveECGSignal();
                        ecg_signal.clear();
                        ctx.sendBroadcast(classification);
                    }


                    ctx.sendBroadcast(local);
                    if (mqttClient.isConnected()) {
                        try {
                            mqttClient.publish("rhythm/" + AppSetting.getBluetoothDeviceName(ctx) + "/ecg", new MqttMessage(message.replace("��", "").getBytes()));
                        } catch (MqttException e) {
                            e.printStackTrace();
                            Log.d(TAG, "onMessage: " + e.getMessage());
                        }
                    }

                    ecgData += ts + ":" + message.replace("��", "") + "\n";
                    if (ecg_signal.size() > 2000) {
                        saveECGSignal();
                    }
                } else {
                    if (ecgData.length() > 2500){
                        saveECGSignal();
                    }
                }

            }

            @Override
            public void onError(int errorCode) {
                sendToast("Bluetooth failed to connect");
                Log.d(TAG, "onError: " + errorCode);
                reconnect();
                saveECGSignal();
            }

            @Override
            public void onConnectError(BluetoothDevice device, String message) {
                sendToast("Bluetooth failed to connect");
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
        sendToast("Start reconnecting");
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

        sendToast("Connecting to "+name);
        bluetooth.onStart();
        if (bluetooth.isEnabled()) {
            Log.d(TAG, "connectBluetooth: Connecting to" + name);
            bluetooth.connectToName(name);
        }
    }
    void sendToast(String message){
        Handler mainHandler = new Handler(getMainLooper());

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                // Do your stuff here related to UI, e.g. show toast
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
