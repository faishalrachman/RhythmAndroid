package com.faishalrachman.amonsecg;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.core.Core;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.faishalrachman.amonsecg.services.CoreService;
import com.faishalrachman.amonsecg.utils.NotificationHelper;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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
//import org.eclipse.paho.client.mqttv3.IMqttActionListener;
//import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
//import org.eclipse.paho.client.mqttv3.IMqttToken;
//import org.eclipse.paho.client.mqttv3.MqttCallback;
//import org.eclipse.paho.client.mqttv3.MqttException;
//import org.eclipse.paho.client.mqttv3.MqttMessage;
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
    @BindView(R.id.graphView3)
    LineChart lineChart2;
    @BindView(R.id.graphView4)
    LineChart lineChart3;
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
    @BindView(R.id.btn_toggle)
    Button btn_toggle;

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

    @OnClick(R.id.btn_toggle)
    void onClickCall() {
        if (coreService != null) {
            if (!AppSetting.getRecordingStatus(getApplicationContext())) {

                Long tsLong = System.currentTimeMillis() / 1000;
                String name = AppSetting.getBluetoothDeviceName(getApplicationContext());
                String filename = tsLong.toString() + "-" + name;
//                Log.d(TAG, "startRecording: filename="+filename);
                AppSetting.setRecordingStatus(getApplicationContext(), true);
                AppSetting.setRecordFilename(getApplicationContext(), filename);
                btn_toggle.setText("Stop");
                AppSetting.setRecordingStatus(getApplicationContext(), true);
                Log.d(TAG, "onClickCall: " + filename);
            } else {
//                coreService.stopRecording();
                btn_toggle.setText("Start");
                AppSetting.setRecordingStatus(getApplicationContext(), false);
//                coreService.saveECGSignal();
//                coreService.uploadECGSignal();
            }
//            if (CoreService.is_recording) {
//                coreService.startRecording();
//                btn_toggle.setText("Start");
//            } else {
//                CoreService.is_recording = true;
//                coreService.stopRecording();
//                btn_toggle.setText("Stop");
//            }
        } else {
            Toast.makeText(this, "Service is not initialized", Toast.LENGTH_SHORT).show();
        }
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
    private List<Float> ecgAllData2 = new ArrayList<>();
    private List<Float> ecgAllData3 = new ArrayList<>();
    //DATA BUAT MPANDROIDCHART
    private List<Entry> ecgEntry = new ArrayList<>();
    private List<Entry> ecgEntry2 = new ArrayList<>();
    private List<Entry> ecgEntry3 = new ArrayList<>();
    private LineDataSet dataset;
    private LineDataSet dataset2;
    private LineDataSet dataset3;
    private LineData linedata;
    private LineData linedata2;
    private LineData linedata3;
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

    void setupDetail() {
        AppSetting.AccountInfo info = AppSetting.getSavedAccount(DetailActivity.this);


        AppSetting.showProgressDialog(DetailActivity.this, "Retrieving data");


        AndroidNetworking.get(AppSetting.getHttpAddress(DetailActivity.this)
                + getString(R.string.ping_url))
                .addHeaders("Authorization", AppSetting.getSession(DetailActivity.this))
                .setPriority(Priority.MEDIUM).build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppSetting.dismissProgressDialog();
                        try {
//                            ItemDevice device = ItemDevice.jsonToDevice(response
//                                    .getJSONArray("data_pasien").getJSONObject(0));
                            JSONObject data = response.getJSONObject("data");

                            System.out.println(response.toString());

                            friendFullName.setText(data.getString("name"));
                            fullname = data.getString("name");
                            friendAddress.setText(data.getString("address"));
                            friendPhone.setText(data.getString("phone_number"));
//                            phoneNumber = device.phone; /*setup phone*/
                            friendPhone.setText(data.getString("phone_number"));
                            phoneEmergencyNumber = data.getString("phone_number"); /*setup phone*/
                            friendGender.setText(data.getBoolean("gender") ? "Male" : "Female");
//                            friendAge.setText(device.age);

                            itemName.setText(data.getString("name"));
                            itemId.setText(String.format(Locale.US, "%s: %s", getString(R.string.device_id),
                                    data.getString("device_id")));
//                            itemId.setText(String.format(Locale.US, "%s: %s", getString(R.string.device_id),
//                                    "ECG001"));
                            String device_id = data.getString("device_id");
                            Log.d(TAG, "onResponse: " + data.toString());
//                            setupChart();
                            boolean se = isMyServiceRunning(CoreService.class);
                            if (!se) {
                                Log.d(TAG, "onResponse: Serpis " + se);
                                startCoreService(device_id);
                            }
//                            connectBluetooth(device.deviceId);
//                            setupMqtt(device.deviceId);
                            setupCondition(data.getBoolean("gender"));

                        } catch (JSONException ex) {
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        AppSetting.dismissProgressDialog();
                        Log.i("Detail", "onError: " + anError.getErrorBody());
                        if (anError.getErrorCode() == 401) {
                            Toast.makeText(DetailActivity.this, "Session anda telah habis, silahkan login kembali", Toast.LENGTH_SHORT).show();
                            AppSetting.setLogin(DetailActivity.this, AppSetting.LOGGED_OUT);
                            finish();
                        } else {
                            Toast.makeText(DetailActivity.this, "Internet anda mati, mohon sambungkan ke internet untuk menggunakan seluruh layanan", Toast.LENGTH_SHORT).show();
//                        setupChart();
//                        connectBluetooth("ECG001");

                        }
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

    void initializeChart(LineDataSet dataset, LineChart lineChart, LineData linedata, float minScaleY, float maxScaleY) {
        dataset.setColor(getResources().getColor(R.color.colorPrimary));
        dataset.setLineWidth(2);
        dataset.setDrawCircles(false);
        dataset.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        dataset.setDrawValues(false);
        dataset.setFillColor(R.color.colorPrimary);
        dataset.setValueTextColor(R.color.colorPrimaryDark);
        lineChart.setData(linedata);
        lineChart.setDrawMarkers(false);
        lineChart.setDrawBorders(true);
        lineChart.setBorderWidth(0.001f);
        lineChart.setVisibleXRangeMaximum(150);
        lineChart.setVisibleXRangeMinimum(0);
        lineChart.setScaleEnabled(false);


        Description desc = new Description();
        desc.setText("ECG Data");
        lineChart.setDescription(desc);
        lineChart.isAutoScaleMinMaxEnabled();
//        lineChart.setScaleMinima(800,1000);

        int count = 10;
        YAxis leftAxis = lineChart.getAxisLeft();
//                        leftAxis.setAxisLineWidth(0.01f);
        leftAxis.setAxisMinimum(minScaleY);
        leftAxis.setAxisMaximum(maxScaleY);
//                        leftAxis.setGranularity(0.5f);
        leftAxis.setGridLineWidth(1);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisLineColor(R.color.colorGreen);
        leftAxis.setLabelCount(count, true);
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setAxisMaximum(maxScaleY);
        rightAxis.setAxisMinimum(minScaleY);
        rightAxis.setDrawLabels(false);
        rightAxis.setGridLineWidth(1);
        rightAxis.setLabelCount(count, true);
//                        rightAxis.setGranularity(0.5f);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setGranularity(1);
        xAxis.setDrawLabels(false);
        xAxis.setLabelCount(20, true);
        xAxis.setDrawGridLines(true);
        Log.d("Spacing", "Top = " + String.valueOf(rightAxis.getSpaceTop()) + "Bottom = " + String.valueOf(rightAxis.getSpaceBottom()));

    }

    void setupChart() {
        ecgEntry.add(new Entry(0, 0));
        ecgEntry2.add(new Entry(0, 0));
        ecgEntry3.add(new Entry(0, 0));
        dataset = new LineDataSet(ecgEntry, "ECG Data");
        linedata = new LineData(dataset);
        dataset2 = new LineDataSet(ecgEntry2, "ECG Data");
        linedata2 = new LineData(dataset2);
        dataset3 = new LineDataSet(ecgEntry3, "ECG Data");
        linedata3 = new LineData(dataset3);
        initializeChart(dataset, lineChart, linedata, -2f, 3f);
        initializeChart(dataset2, lineChart2, linedata2, -2f, 3f);
        initializeChart(dataset3, lineChart3, linedata3, -2f, 3f);
        if (!ttask.is_running)
            t.scheduleAtFixedRate(ttask, 5, 5);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();

        boolean status = AppSetting.getRecordingStatus(getApplicationContext());
        if (status) {
            btn_toggle.setText("Stop");
        } else {
            btn_toggle.setText("Start");
        }

        Log.d(TAG, "onStart: Start");
        if (!ttask.is_running)
            setupChart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Stop SErvice");
        try {
            if (mServiceIntent != null)
                stopService(mServiceIntent);
            if (updateUIReceiver != null)
                unregisterReceiver(updateUIReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    int Z = 0;

    public class MyTimerTask extends TimerTask {
        public boolean is_running = false;

        @Override
        public void run() {
            try {
                is_running = true;
//        Toast.makeText(getApplicationContext(),"Ganteng",Toast.LENGTH_SHORT).show();
                if (ecgAllData.size() > 3 && ecgAllData2.size() > 3 && ecgAllData3.size() > 3) {
                    if (ecgAllData.get(0) != null && ecgAllData2.get(0) != null && ecgAllData3.get(0) != null) {
                        try {
                            float data = ecgAllData.get(0);
                            float data2 = ecgAllData2.get(0);
                            float data3 = ecgAllData3.get(0);
                            linedata.addEntry(new Entry(Z, data), 0);//INSERT LAST
                            linedata2.addEntry(new Entry(Z, data2), 0);//INSERT LAST
                            linedata3.addEntry(new Entry(Z, data3), 0);//INSERT LAST
                            Z++;
                            ecgAllData.remove(0);
                            ecgAllData2.remove(0);
                            ecgAllData3.remove(0);
                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                        lineChart.setData(linedata);
                                if (Z > 200) {
//                            dataset.removeFirst();
                                    lineChart.setVisibleXRangeMaximum(800);
                                    lineChart.moveViewToX(Z - 200);
                                    lineChart2.setVisibleXRangeMaximum(800);
                                    lineChart2.moveViewToX(Z - 200);
                                    lineChart3.setVisibleXRangeMaximum(800);
                                    lineChart3.moveViewToX(Z - 200);
//                            lineChart.centerViewTo(Z-200,0, YAxis.AxisDependency.RIGHT);
//                            dataset.removeEntry(0);

                                }
                                lineChart.notifyDataSetChanged();
                                lineChart.invalidate();
                                lineChart2.notifyDataSetChanged();
                                lineChart2.invalidate();
                                lineChart3.notifyDataSetChanged();
                                lineChart3.invalidate();
                            }
                        });
                    }

                }
            } catch (NullPointerException e) {
                e.printStackTrace();
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
        filter.addAction("publish.classification");

        updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: " + intent.getAction());
                if (intent != null) {
                    if (intent.getAction().contains("service.to.activity.transfer")) {
                        String ecg = intent.getStringExtra("signal");
                        String[] data = ecg.split(";");
                        String time_stamp = data[0];
                        String[] signal1 = data[1].split(":");
                        String[] signal2 = data[2].split(":");
                        String[] signal3 = data[3].split(":");

                        ArrayList<Float> local = new ArrayList<>();
                        ArrayList<Float> local2 = new ArrayList<>();
                        ArrayList<Float> local3 = new ArrayList<>();
                        for (int i = 0; i < signal1.length; i++) {
//                            ecgAllData.add(Float.parseFloat(data[i]));
                            local.add(Float.parseFloat(signal1[i]));
                            local2.add(Float.parseFloat(signal2[i]));
                            local3.add(Float.parseFloat(signal3[i]));
                        }
//
                        ecgAllData.addAll(local);
                        ecgAllData2.addAll(local2);
                        ecgAllData3.addAll(local3);

                    } else if (intent.getAction().contains("publish.classification")) {
                        Log.d(TAG, "onReceive: AYANAONIEU");
                        String notif = intent.getStringExtra("notification");
                        int bpm = intent.getIntExtra("hr", 65);
                        Log.d(TAG, "onReceive: " + bpm);
                        itemRate.setText(String.valueOf(bpm));
                        NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext());
                        switch (notif.toLowerCase()) {
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
                                notificationHelper.createNotification("Rhythm", "Premature Ventricular Contraction Detected");
                                break;
                            case "vf":
                                alertTitle.setTextColor(getResources().getColor(R.color.colorRed));
                                alertTitle.setText("Condition: Ventricular Fibrillation");
                                alertImage.setImageResource(R.drawable.ic_error_red);
                                soundOnDrop();
                                notificationHelper.createNotification("Rhythm", "Ventricular Fibrillation Detected");
                                break;
                            case "heartblock":
                                alertTitle.setTextColor(getResources().getColor(R.color.colorRed));
                                alertTitle.setText("Condition: Heartblock Detected");
                                alertImage.setImageResource(R.drawable.ic_error_red);
                                soundOnDrop();
                                notificationHelper.createNotification("Rhythm", "Heartblock Detected");
                                break;
                            case "af":
                                alertTitle.setTextColor(getResources().getColor(R.color.colorRed));
                                alertTitle.setText("Condition: Atrial Fibrilation Detected");
                                alertImage.setImageResource(R.drawable.ic_error_red);
                                soundOnDrop();
                                notificationHelper.createNotification("Rhythm", "Atrial Fibrilation Detected");
                                break;
                        }
                    } else if (intent.getAction().contains("make.toast")) {
                        Log.d(TAG, "onReceive: " + intent.getAction());
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show();
                    }
                }
            }
        };
        registerReceiver(updateUIReceiver, filter);

        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                Log.d(TAG, "onPermissionsChecked: " + report.toString());
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
        AppSetting.setBluetoothDeviceName(getApplicationContext(), deviceId);
        Toast.makeText(this, deviceId, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "startCoreService: HEHEHEHEHEHE");
        Log.d(TAG, "startCoreService: " + deviceId);
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
//                finish();
                break;
            case R.id.action_about:
                startActivity(new Intent(DetailActivity.this, AboutActivity.class));
                break;
            case R.id.action_logout:
                if (AppSetting.getRecordingStatus(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), "Mohon di stop terlebih dahulu sebelum logout aplikasi", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Logout berhasil, Silahkan jalankan kembali aplikasi ini", Toast.LENGTH_SHORT).show();
                    AppSetting.setLogin(DetailActivity.this, AppSetting.LOGGED_OUT);
                    try {
                        CoreService.bluetooth.disconnect();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                System.exit(0);
//                startActivity(new Intent(DetailActivity.this, LoginActivity.class));

//                    stopService(new Intent(DetailActivity.this, CoreService.class));
                    super.onBackPressed();
                    finish();

                    coreService.stopThis();
//                    System.exit(0);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onStop() {
        super.onStop(); //bluetooth.onStop();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("isMyServiceRunning?", true + "");
                return true;
            }
        }
        Log.i("isMyServiceRunning?", false + "");
        return false;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
//        super.onBackPressed();
    }
}
