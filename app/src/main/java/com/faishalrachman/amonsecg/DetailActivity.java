package com.faishalrachman.amonsecg;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.core.Core;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.faishalrachman.amonsecg.services.CoreService;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.faishalrachman.amonsecg.model.ItemDevice;
import com.github.mikephil.charting.charts.*;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.DeviceCallback;

public class DetailActivity extends AppCompatActivity {

    /*QUICK INFO*/
    @BindView(R.id.item_image)
    ImageView itemImage;
    @BindView(R.id.item_name)
    TextView itemName;
    @BindView(R.id.item_id)
    TextView itemId;
    @BindView(R.id.item_rate)
    TextView itemRate;

    /*GRAPH VIEW*/
//    @BindView(R.id.graphView) SparkView sparkView;
    @BindView(R.id.graphView2)
    LineChart lineChart;
//    com.github.mikephil.charting.charts.Chart


    /*DETAIL INFO*/
    @BindView(R.id.friend_full_name)
    TextView friendFullName;
    @BindView(R.id.friend_address)
    TextView friendAddress;
    @BindView(R.id.friend_phone)
    TextView friendPhone;
    @BindView(R.id.friend_gender)
    TextView friendGender;
    @BindView(R.id.friend_age)
    TextView friendAge;

    /*ALERT INFO*/
    @BindView(R.id.alert_image)
    ImageView alertImage;
    @BindView(R.id.alert_title)
    TextView alertTitle;
    @BindView(R.id.alert_detail)
    TextView alertDetail;
    @BindView(R.id.button_remove)
    View button_remove;

    CoreService coreService;
    Intent mServiceIntent;
    BroadcastReceiver updateUIReceiver;

    @OnClick(R.id.button_sms)
    void onSmsClick() {
        if (phoneEmergencyNumber != null)
            AppSetting.makeASms(DetailActivity.this, phoneEmergencyNumber);
        else Toast.makeText(this, getString(R.string.no_phone_num), Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.button_call)
    void onCallClick() {
        if (phoneEmergencyNumber != null)
            AppSetting.makeACall(DetailActivity.this, phoneEmergencyNumber);
        else Toast.makeText(this, getString(R.string.no_phone_num), Toast.LENGTH_SHORT).show();
    }
//    @OnClick(R.id.button_remove) void onRemoveClick(){
//        AppSetting.showProgressDialog(DetailActivity.this, "Removing friend");
//        AndroidNetworking.post(AppSetting.getHttpAddress(DetailActivity.this)
//                +"/{user}/{username}/data/remove")
//                .addPathParameter("user", "patient")
//                .addPathParameter("username", my_username)
//                .addBodyParameter("username", username)
//                .setPriority(Priority.MEDIUM).build()
//                .getAsJSONObject(new JSONObjectRequestListener() {
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        AppSetting.dismissProgressDialog();
//                        Toast.makeText(DetailActivity.this, getString(R.string.friend_delete_success), Toast.LENGTH_SHORT).show();
//                        finish();
//                    }
//
//                    @Override
//                    public void onError(ANError anError) {
//                        AppSetting.dismissProgressDialog();
//                        Log.i("Detail", "onError: "+anError.getErrorBody());
//                    }
//                });
//    }

    private MqttAndroidClient mqttClient;
    private static String TAG = "DetailActivity";
    private List<String> subscribedTopic = new ArrayList<>();
    private List<Float> ecgData = new ArrayList<>();
    private List<Float> ecgAllData = new ArrayList<>();
    //DATA BUAT MPANDROIDCHART
    private List<Entry> ecgEntry = new ArrayList<>();
    private LineDataSet dataset;
    private LineData linedata;
    String topicPrefix = "";
    //END

    private String phoneEmergencyNumber = "";
    private String fullname;

    private MediaPlayer mediaPlayer;
    private boolean ringtoneIsIdle = true;
    Bluetooth bluetooth;
    String deviceId;


    boolean isNerima = false;
    int fps_counter = 0;
    MyTimerTask ttask = new MyTimerTask();
    Timer t = new Timer();

    void setupMqttCallBack() {
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Toast.makeText(getApplicationContext(), "Connection Lost", Toast.LENGTH_SHORT).show();
                System.out.println("Connection was lost!");
//                t.cancel();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
//                System.out.println("Message Arrived!: " + topic + ": " + new String(message.getPayload()));


                String[] splitedTopic = topic.split("/");
                System.out.println(splitedTopic);
                switch (splitedTopic[2]) {//awalnya 2
                    case "bpm":
                        itemRate.setText(String.format(Locale.US, "%.0f", Float.parseFloat(new String(message.getPayload()))));
                        break;
//                    case "ecg"://awalnya visual
//                        fps_counter += 1;
//                        String[] data = new String(message.getPayload()).split(":");
//                        for (int i = 1; i < data.length; i++) {
//                            ecgAllData.add(Float.parseFloat(data[i]));
//                        }
//                        isNerima = true;
//                        lineChart.invalidate();
//                        break;
                    case "n":
                        String alertString = new String(message.getPayload());
                        switch (alertString.toLowerCase()) {
                            case "normal":
                                alertTitle.setTextColor(getResources().getColor(R.color.colorGreen));
                                alertTitle.setText("Condition: Normal");
                                alertImage.setImageResource(R.drawable.ic_error_green);
                                break;
                            case "pvc":
                                alertTitle.setTextColor(getResources().getColor(R.color.colorRed));
                                alertTitle.setText("Condition: Premature Ventricular Contraction Detected");
                                alertImage.setImageResource(R.drawable.ic_error_red);
                                soundOnDrop();
                                break;
                            case "vf":
                                alertTitle.setTextColor(getResources().getColor(R.color.colorRed));
                                alertTitle.setText("Condition: Ventricular Fibrillation");
                                alertImage.setImageResource(R.drawable.ic_error_red);
                                soundOnDrop();
                                break;
                            case "heartblock":
                                alertTitle.setTextColor(getResources().getColor(R.color.colorRed));
                                alertTitle.setText("Condition: Heartblock Detected");
                                alertImage.setImageResource(R.drawable.ic_error_red);
                                soundOnDrop();
                                break;
                            case "af":
                                alertTitle.setTextColor(getResources().getColor(R.color.colorRed));
                                alertTitle.setText("Condition: Atrial Fibrilation Detected");
                                alertImage.setImageResource(R.drawable.ic_error_red);
                                soundOnDrop();
                                break;

                        }
                        break;
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i("MQTT", "Delivery Complete!");
            }
        });
    }

    void setupDetail() {
        AppSetting.AccountInfo info = AppSetting.getSavedAccount(DetailActivity.this);


        AppSetting.showProgressDialog(DetailActivity.this, "Retrieving data");


        AndroidNetworking.get(AppSetting.getHttpAddress(DetailActivity.this)
                + getString(R.string.login_url))
                .addQueryParameter("email", info.username)
                .addQueryParameter("password", info.password)
                .setPriority(Priority.MEDIUM).build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppSetting.dismissProgressDialog();
                        try {
                            ItemDevice device = ItemDevice.jsonToDevice(response
                                    .getJSONArray("data_pasien").getJSONObject(0));

                            System.out.println(response.toString());

                            friendFullName.setText(device.getName());
                            fullname = device.full_name;
                            friendAddress.setText(device.address);
                            friendPhone.setText(device.phone);
//                            phoneNumber = device.phone; /*setup phone*/
                            friendPhone.setText(device.emergencyPhone);
                            phoneEmergencyNumber = device.emergencyPhone; /*setup phone*/
                            friendGender.setText(device.isMale() ? "Male" : "Female");
                            friendAge.setText(device.age);

                            itemName.setText(device.getName());
                            itemId.setText(String.format(Locale.US, "%s: %s", getString(R.string.device_id),
                                    device.deviceId));
//                            itemId.setText(String.format(Locale.US, "%s: %s", getString(R.string.device_id),
//                                    "ECG001"));

//                            setupChart();
                            boolean se = isMyServiceRunning(CoreService.class);
                            if (!se) {
                                Log.d(TAG, "onResponse: Serpis "+se);
                                startCoreService(device.deviceId);
                            }
//                            connectBluetooth(device.deviceId);
//                            setupMqtt(device.deviceId);
                            setupCondition(device.isMale());

                        } catch (JSONException ex) {
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        AppSetting.dismissProgressDialog();
                        Log.i("Detail", "onError: " + anError.getErrorBody());
                        setupChart();
//                        connectBluetooth("ECG001");
                        Toast.makeText(DetailActivity.this, "Gagal", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    void setupCondition(boolean isMale) {
        if (isMale) {
            itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.boy0));
//            switch (condition){
//                case 0: itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.boy0));
//                    break;
//                case 1: itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.boy1));
//                    break;
//                case 2: itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.boy2));
//                    break;
//            }

        } else {
            itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.girl0));
//            switch (condition){
//                case 0: itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.girl0));
//                    break;
//                case 1: itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.girl1));
//                    break;
//                case 2: itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.boy2));
//                    break;
//            }
        }
    }

    void setupChart() {
        ecgEntry.add(new Entry(0, 0));

        dataset = new LineDataSet(ecgEntry, "ECG Data");
        dataset.setColor(getResources().getColor(R.color.colorPrimary));
        dataset.setLineWidth(2);
        dataset.setDrawCircles(false);
        dataset.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        dataset.setDrawValues(false);
        dataset.setFillColor(R.color.colorPrimary);
        dataset.setValueTextColor(R.color.colorPrimaryDark);

        linedata = new LineData(dataset);
        lineChart.setData(linedata);
        lineChart.setDrawMarkers(false);
        lineChart.setDrawBorders(true);
        lineChart.setBorderWidth(0.001f);
        lineChart.setVisibleXRangeMaximum(150);
        lineChart.setVisibleXRangeMinimum(0);
        Description desc = new Description();
        desc.setText("ECG Data");
        lineChart.setDescription(desc);
        lineChart.isAutoScaleMinMaxEnabled();
//        lineChart.setScaleMinima(800,1000);

        float min = -2f;
        float max = 3f;
        int count = 10;
        YAxis leftAxis = lineChart.getAxisLeft();
//                        leftAxis.setAxisLineWidth(0.01f);
        leftAxis.setAxisMinimum(min);
        leftAxis.setAxisMaximum(max);
//                        leftAxis.setGranularity(0.5f);
        leftAxis.setGridLineWidth(1);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisLineColor(R.color.colorGreen);
        leftAxis.setLabelCount(count, true);
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setAxisMaximum(max);
        rightAxis.setAxisMinimum(min);
        rightAxis.setDrawLabels(false);
        rightAxis.setGridLineWidth(1);
        rightAxis.setLabelCount(count, true);
//                        rightAxis.setGranularity(0.5f);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setGranularity(1);
        xAxis.setDrawLabels(false);
        xAxis.setLabelCount(40, true);
        xAxis.setDrawGridLines(true);
        Log.d("Spacing", "Top = " + String.valueOf(rightAxis.getSpaceTop()) + "Bottom = " + String.valueOf(rightAxis.getSpaceBottom()));
        t.scheduleAtFixedRate(ttask, 17, 10);
    }

    void setupMqtt(String deviceId) {
        /*MQTT RELATED*/
//        subscribedTopic.add("rhythm/"+deviceId+"/visual");
        Log.d(TAG, "setupMqtt: DEVICE=" + deviceId);

        this.topicPrefix = AppSetting.getTopic(DetailActivity.this);
        String bpm = topicPrefix + "/bpm";
        String notification = topicPrefix + "/n";
        String ecg = topicPrefix + "/ecg";
        System.out.println(bpm);
        subscribedTopic.add(bpm);
        subscribedTopic.add(notification);
//        subscribedTopic.add(ecg);
        if (mqttClient == null) {
            mqttClient = AppSetting.getMqttClient(DetailActivity.this);
            try {
                Log.i("MQTT", "SETUP");
                System.out.println("Setup Mqtt");
                System.out.println();
                Toast.makeText(getApplicationContext(), "Setup MQTT", Toast.LENGTH_SHORT).show();
                mqttClient.connect(DetailActivity.this, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {

                        Log.i("MQTT", "Connected");
                        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();

                        resumeMqtt();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        System.out.println("gagalkonek");
//                        System.out.println(asyncActionToken.getResponse().toString());
                    }
                });
            } catch (MqttException ex) {
                Log.e("MqttSetup", "can't connect");
                ex.printStackTrace();
            }
        }

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // media player
        this.mediaPlayer = MediaPlayer.create(getApplicationContext(), sound);
        if (this.mediaPlayer != null) {
            this.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    ringtoneIsIdle = true;
                }
            });
            this.mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    ringtoneIsIdle = true;
                    return true;
                }
            });
        }
    }

    private void resumeMqtt() {
        setupMqttCallBack();
        System.out.println("detail resume subs " + subscribedTopic.size());
        for (String topic : subscribedTopic) {
            System.out.println("detail resume subs " + topic);
            try {
                if (mqttClient != null) {
                    mqttClient.subscribe(topic, 0);
                    Log.i("MQTT", "SUBSCRIBE");
                } else
                    Log.i("MQTT", "SUBSCRIBE NULL");
                //  System.out.println("MQTT is NULL");
//                Toast.makeText(getApplicationContext(),"MQTT NULL",Toast.LENGTH_SHORT).show();
            } catch (MqttException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Start");
        if (!ttask.is_running)
            setupChart();
    }

    @Override
    protected void onDestroy() {
//        if (bluetooth.isConnected())
//            bluetooth.disconnect();
//        Log.i("Detail", "onDestroy: ");
//        for (String topic : subscribedTopic) {
//            Log.i("Detail", "onDestroy: " + topic);
//            try {
//                mqttClient.unsubscribe(topic);
//            } catch (MqttException ex) {
//                // do nothing
//                // un-subscribe failed
//            }
//        }
//
//        if (mqttClient != null) {
//            mqttClient.unregisterResources();
//            mqttClient.close();
//        }
        Log.d(TAG, "onDestroy: Stop SErvice");
        stopService(mServiceIntent);
        unregisterReceiver(updateUIReceiver);
        super.onDestroy();
    }

    int Z = 0;

    public class MyTimerTask extends TimerTask {
        public boolean is_running = false;
        @Override
        public void run() {
            is_running = true;
//        Toast.makeText(getApplicationContext(),"Ganteng",Toast.LENGTH_SHORT).show();
            if (ecgAllData.size() > 3) {
                float data = ecgAllData.get(0);
                linedata.addEntry(new Entry(Z, data), 0);//INSERT LAST
                Z++;
                ecgAllData.remove(0);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        lineChart.setData(linedata);
                        if (Z > 200) {
//                            dataset.removeFirst();
//                            lineChart.setVisibleXRangeMaximum(200);
                            lineChart.moveViewToX(Z - 200);
//                            lineChart.centerViewTo(Z-200,0, YAxis.AxisDependency.RIGHT);
//                            dataset.removeEntry(0);

                        }
                        lineChart.notifyDataSetChanged();
                        lineChart.invalidate();
                    }
                });

            }
        }
    }

    private void soundOnDrop() {
        if (ringtoneIsIdle) {
            ringtoneIsIdle = false;

            mediaPlayer.start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction("service.to.activity.transfer");
        updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null){
                    String ecg = intent.getStringExtra("signal");
                    String[] data = ecg.split(":");
                    for (int i = 1; i < data.length; i++) {
                        ecgAllData.add(Float.parseFloat(data[i]));
                    }
                }
            }
        };
        registerReceiver(updateUIReceiver,filter);

        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                Log.d(TAG, "onPermissionsChecked: "+report.toString());
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

            }
        }).check();

//        setupBluetooth();


        /*DETAIL INFORMATION*/
        setupDetail();
    }

    void startCoreService(String deviceId) {
        AppSetting.setBluetoothDeviceName(getApplicationContext(),deviceId);
        Toast.makeText(this, deviceId, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "startCoreService: HEHEHEHEHEHE");
        Log.d(TAG, "startCoreService: "+deviceId);
        coreService = new CoreService(getApplicationContext());
        mServiceIntent = new Intent(getApplicationContext(), coreService.getClass());
        if (!isMyServiceRunning(coreService.getClass())) {
            Log.d(TAG, "startCoreService: Starting Intent Service");
            startService(mServiceIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.action_notif:
//                startActivity(new Intent(DetailActivity.this, NotificationActivity.class));
//                break;
            case R.id.action_mode:
                startActivity(new Intent(DetailActivity.this, DetailActivityOffline.class));
                finish();
                break;
            case R.id.action_about:
                startActivity(new Intent(DetailActivity.this, AboutActivity.class));
                break;
            case R.id.action_logout:
                if (isMyServiceRunning(CoreService.class))
                    stopService(mServiceIntent);
                unregisterReceiver(updateUIReceiver);
                Toast.makeText(this, "Logout berhasil, Silahkan jalankan kembali aplikasi ini", Toast.LENGTH_SHORT).show();
                AppSetting.setLogin(DetailActivity.this, AppSetting.LOGGED_OUT);
//                System.exit(0);
//                startActivity(new Intent(DetailActivity.this, LoginActivity.class));
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    void reconnect() {
        connectBluetooth(this.deviceId);
    }

    void setupBluetooth() {
        bluetooth = new Bluetooth(getApplicationContext());
        bluetooth.setDeviceCallback(new DeviceCallback() {
            @Override
            public void onDeviceConnected(BluetoothDevice device) {
                Log.d(TAG, "onDeviceConnected: " + device.getName());
                bluetooth.send("Connected");
                AppSetting.dismissProgressDialog();
//                setupChart();
            }

            @Override
            public void onDeviceDisconnected(BluetoothDevice device, String message) {
                Log.d(TAG, "onDeviceDisconnected: " + message);
                reconnect();
            }

            @Override
            public void onMessage(String message) {
                String[] data = message.split(":");
                for (int i = 1; i < data.length; i++) {
                    ecgAllData.add(Float.parseFloat(data[i]));
                }
                if (mqttClient != null && mqttClient.isConnected()) {
                    try {

                        mqttClient.publish(topicPrefix + "/ecg", message.getBytes(), 0, true);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                fps_counter += 1;
                                isNerima = true;
                                lineChart.invalidate();
                            }
                        }
                );
                Log.d(TAG, "onMessage: " + message);
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
    }

    void connectBluetooth(String name) {
        this.deviceId = name;
        AppSetting.setBluetoothDeviceName(getApplicationContext(),name);
        bluetooth.onStart();
        if (bluetooth.isEnabled()) {
            Log.d(TAG, "connectBluetooth: Connecting to" + name);
            bluetooth.connectToName(name);
        } else {
            bluetooth.showEnableDialog(this);
        }
    }


    @Override
    protected void onStop() {
        super.onStop(); //bluetooth.onStop();
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }
}
