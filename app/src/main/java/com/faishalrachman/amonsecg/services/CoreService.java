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
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
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
import com.faishalrachman.amonsecg.algo.ECGClassificationML;
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
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
    private static ArrayList<Float> ecg_signal2;
    private static ArrayList<Float> ecg_signal3;
    private static ECGClassificationML clf;


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
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ctx = getApplicationContext();

        MappedByteBuffer modelFile = null;
        try {
            modelFile = loadModelFile(getAssets(),"model.tflite");
            Interpreter tflite = new Interpreter(modelFile,new Interpreter.Options());
            clf = new ECGClassificationML(tflite);
//            clf.classifycc();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!is_recording) {
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
            if (callback == null) {
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
                        Log.d(TAG, "messageArrived: " + message.getPayload().toString());
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                };
                mqttClient.setCallback(callback);
            }
            if (!mqttClient.isConnected()) {
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
//                    Toast.makeText(ctx, "Connecting MQTT to SERVER", Toast.LENGTH_SHORT).show();
                    sendToast("Connecting to MQTT Server");
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
//            System.exit(0);
        }
        ecg_signal = new ArrayList<>();
        ecg_signal2 = new ArrayList<>();
        ecg_signal3 = new ArrayList<>();

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
//        Log.i("EXIT", "ondestroy!");
////        if (AppSetting.isLoggedIn(ctx)) {
//        Log.d(TAG, "onDestroy: Restarting");
//        Intent broadcastIntent = new Intent(this, CoreRestarter.class);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            Log.d(TAG, "onDestroy: stopForeground");
//            stopForeground(true);
//        }
//
//            sendBroadcast(broadcastIntent);
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
                Log.d(TAG, "saveECGSignal: " + filename);
//            File file = new File(context.getFilesDir(), filename);
//                FileWriter writer = new FileWriter(folder.getPath() + "/" + ts + "-" + name + ".txt");
//                FileOutputStream fOut = openFileOutput(folder.getPath() + "/" + filename + ".txt",  MODE_APPEND);
                FileOutputStream fOut = new FileOutputStream(new File(folder.getPath() + "/" + filename + "-3lead.txt"), true);//openFileOutput(folder.getPath() + "/" + filename + ".txt",  MODE_APPEND);
//                FileOutputStream fOut2 = new FileOutputStream(new File(folder.getPath() + "/" + filename + "-ch2.txt"), true);//openFileOutput(folder.getPath() + "/" + filename + ".txt",  MODE_APPEND);
//                FileOutputStream fOut3 = new FileOutputStream(new File(folder.getPath() + "/" + filename + "-ch3.txt"), true);//openFileOutput(folder.getPath() + "/" + filename + ".txt",  MODE_APPEND);
                fOut.write(ecgData.getBytes());
//                fOut2.write(ecgData2.getBytes());
//                fOut3.write(ecgData3.getBytes());
                fOut.close();
//                fOut2.close();
//                fOut3.close();
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

    public void uploadECGSignal() {

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
//                data.delete();
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
        Log.d(TAG, "onTaskRemoved: Dimatiin");
        // workaround for kitkat: set an alarm service to trigger service again
//        if (AppSetting.isLoggedIn(ctx)) {
//            Intent intent = new Intent(ctx, CoreService.class);
//            PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_ONE_SHOT);
//            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//            alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 5000, pendingIntent);
//        }
    }

    String ecg1 = "";
    String ecg2 = "";
    String ecg3 = "";

    void setupBluetooth() {
        Log.d(TAG, "setupBluetooth: Inisialisasi Bluetooth");

        bluetooth.setDeviceCallback(new DeviceCallback() {
            @Override
            public void onDeviceConnected(BluetoothDevice device) {
                sendToast(device.getName() + " is connected");
                Log.d(TAG, "onDeviceConnected: " + device.getName());
//                bluetooth.send("Connected");
//                bluetooth.send("S");

                Log.d(TAG, "onDeviceConnected: Send");
                notif = getNotification("Bluetooth is connected");
                startForeground(ID, notif);

            }

            @Override
            public void onDeviceDisconnected(BluetoothDevice device, String message) {
//                Toast.makeText(ctx, "Bluetooth is disconnected", Toast.LENGTH_SHORT).show();
                sendToast("Bluetooth is disconnected");
                notif = getNotification("Bluetooth is disconnected");
                startForeground(ID, notif);
                reconnect();
                saveECGSignal();
                sendToast(device.getName() + " is disconnected");
            }

            @Override
            public void onMessage(String message) {
                is_recording = AppSetting.getRecordingStatus(getApplicationContext());
//                Log.d(TAG, "onMessage: isRecording="+is_recording);
                if (is_recording) {
                    String[] pesan = message.split(",");
                    Long tsLong = System.currentTimeMillis() / 1000;
                    String ts = tsLong.toString();
//                    Log.d(TAG, "onMessage: " + message);
                    if (pesan.length >= 3) {

                        ecg1 += pesan[0] + ":";
                        ecg2 += pesan[1] + ":";
                        ecg3 += pesan[2] + ":";
                        ecg_signal.add(Float.parseFloat(pesan[0]));
                        ecg_signal2.add(Float.parseFloat(pesan[1]));
                        ecg_signal3.add(Float.parseFloat(pesan[2]));

                        if (ecg_signal.size() % 200 == 0 && ecg_signal.size() > 0) {
                            String yang_dikirim = ts + ";" + ecg1 + ";" + ecg2 + ";" + ecg3;
                            Log.d(TAG, "onMessage: " + yang_dikirim);
                            ecg1 = "";
                            ecg2 = "";
                            ecg3 = "";
                            Intent local = new Intent();
                            local.setAction("service.to.activity.transfer");
                            local.putExtra("signal", yang_dikirim);
                            ctx.sendBroadcast(local);
                            if (mqttClient.isConnected()) {
                                try {
                                    mqttClient.publish("rhythm/" + AppSetting.getBluetoothDeviceName(ctx) + "/ecg", new MqttMessage(yang_dikirim.replace("��", "").getBytes()));
                                } catch (MqttException e) {
                                    e.printStackTrace();
                                    Log.d(TAG, "onMessage: " + e.getMessage());
                                }
                            }
                            ecgData += yang_dikirim + "\n";
                        }

                        if (ecg_signal.size() > 2000) {
                            clf.run(ecg_signal);

                            int count = clf.getClass_one();
                            String penyakit = "normal";
                            switch(count){
                                case 1:
                                    penyakit = "vf";
                                    break;
                                case 2:
                                    penyakit = "vf";
                                    break;
                                case 3:
                                    penyakit = "af";
                                    break;
                            }

                            int HR = clf.HR;


                            clf.run(ecg_signal2);

                            int count2 = clf.getClass_one();
                            String penyakit2 = "normal";
                            switch(count2){
                                case 1:
                                    penyakit2 = "vf";
                                    break;
                                case 2:
                                    penyakit2 = "vf";
                                    break;
                                case 3:
                                    penyakit2 = "af";
                                    break;
                            }

                            int HR2 = clf.HR;

                            int count3 = clf.getClass_one();
                            String penyakit3 = "normal";
                            switch(count3){
                                case 1:
                                    penyakit3 = "vf";
                                    break;
                                case 2:
                                    penyakit3 = "vf";
                                    break;
                                case 3:
                                    penyakit3 = "af";
                                    break;
                            }

                            int HR3 = clf.HR;

                            String penyakitFinal = "normal";
                            if (!penyakit.equals("normal")) {
                                penyakitFinal = penyakit;
                            }
                            if (!penyakit2.equals("normal")) {
                                penyakitFinal = penyakit2;
                            }
                            if (!penyakit3.equals("normal")) {
                                penyakitFinal = penyakit3;
                            }

                            Intent classification = new Intent();
                            classification.setAction("publish.classification");
                            classification.putExtra("notification", penyakitFinal);
                            classification.putExtra("hr", (HR + HR2 + HR3) / 3);

                            Log.d(TAG, "onMessage: Penyakit=" + penyakitFinal);
                            Log.d(TAG, "onMessage: HR=" + (HR + HR2 + HR3) / 3);
                            saveECGSignal();
                            ecg_signal.clear();
                            ecg_signal2.clear();
                            ecg_signal3.clear();
                            ctx.sendBroadcast(classification);
                        }
//
//
//                    ctx.sendBroadcast(local);
//                    if (mqttClient.isConnected()) {
//                        try {
//                            mqttClient.publish("rhythm/" + AppSetting.getBluetoothDeviceName(ctx) + "/ecg", new MqttMessage(message.replace("��", "").getBytes()));
//                        } catch (MqttException e) {
//                            e.printStackTrace();
//                            Log.d(TAG, "onMessage: " + e.getMessage());
//                        }
//                    }
//
//                    ecgData += ts + ":" + message.replace("��", "") + "\n";
//                    if (ecg_signal.size() > 2000) {
//                        saveECGSignal();
//                    }
//                } else {
//                    if (ecgData.length() > 2500){
//                        saveECGSignal();
//                    }
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

        sendToast("Connecting to " + name);
        bluetooth.onStart();
        if (bluetooth.isEnabled()) {
            Log.d(TAG, "connectBluetooth: Connecting to" + name);
            bluetooth.connectToName(name);
        }
    }

    void sendToast(String message) {
        Handler mainHandler = new Handler(getMainLooper());

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                // Do your stuff here related to UI, e.g. show toast
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void stopThis() {
        System.exit(0);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
