package com.faishalrachman.amonsecg;

import android.content.Intent;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.faishalrachman.amonsecg.model.Classifier;
import com.robinhood.spark.SparkAdapter;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DetailActivityOffline extends AppCompatActivity {

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

    @OnClick(R.id.button_sms)
    void onSmsClick() {
        if (phoneEmergencyNumber != null)
            AppSetting.makeASms(DetailActivityOffline.this, phoneEmergencyNumber);
        else Toast.makeText(this, getString(R.string.no_phone_num), Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.button_call)
    void onCallClick() {
        if (phoneEmergencyNumber != null)
            AppSetting.makeACall(DetailActivityOffline.this, phoneEmergencyNumber);
        else Toast.makeText(this, getString(R.string.no_phone_num), Toast.LENGTH_SHORT).show();
    }

    private MqttAndroidClient mqttClient;

    private List<String> subscribedTopic = new ArrayList<>();
    private List<Float> ecgData = new ArrayList<>();
    private ArrayList<Double> ecgData200 = new ArrayList<>();
    private List<Float> ecgAllData = new ArrayList<>();
    //DATA BUAT MPANDROIDCHART
    private List<Entry> ecgEntry = new ArrayList<>();
    private LineDataSet dataset;
    private LineData linedata;
    //END
    private MyAdapter ecgAdapter = new MyAdapter();
    private Classifier cl = new Classifier();
    private String phoneEmergencyNumber = "";
    private String fullname;

    private MediaPlayer mediaPlayer;
    private boolean ringtoneIsIdle = true;


    boolean isNerima = false;
    int fps_counter = 0;
    TimerTask ttask = new MyTimerTask();
    Timer t = new Timer();

    void setupMqttCallBack() {
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Toast.makeText(getApplicationContext(), "Connection Lost", Toast.LENGTH_SHORT).show();
                System.out.println("Connection was lost!");
                t.cancel();
            }
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String[] splitedTopic = topic.split("/");
                System.out.println(splitedTopic);
                switch (splitedTopic[2]) {//awalnya 2
                    case "bpm":
                        itemRate.setText(String.format(Locale.US, "%.0f", Float.parseFloat(new String(message.getPayload()))));
                        break;
                    case "ecg"://awalnya visual
                        fps_counter += 1;
                        String[] data = new String(message.getPayload()).split(":");
                        for (int i = 1; i < data.length; i++) {
                            ecgAllData.add(Float.parseFloat(data[i]));
                            ecgData200.add(Double.parseDouble(data[i]));
                        }
                        if (ecgData200.size() >= 1200 && cl.get_rr_list(ecgData200).size() > 5){
                            classify(ecgData200);
                            ecgData200.clear();
                        }
                        isNerima = true;
                        lineChart.invalidate();
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
        AppSetting.AccountInfo info = AppSetting.getSavedAccount(DetailActivityOffline.this);
        AppSetting.showProgressDialog(DetailActivityOffline.this, "Retrieving data");
        setupMqtt("ECG_OFFLINE");
        setupCondition(true);
    }

    void setupCondition(boolean isMale) {
        if (isMale) {
            itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.boy0));

        } else {
            itemImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.girl0));
        }
    }

    void classify(ArrayList<Double> ecgData){
        int[] hasil = cl.beatClassifier(cl.get_rr_list(ecgData));
        String hasilakhir = "";
        if (hasil[0] > 0 && hasil[1] == 0 && hasil[2] == 0 && hasil[3] == 0){
            hasilakhir = "normal";
        }
        if (hasil[2] > hasil[1] && hasil[2] > 0){
            hasilakhir = "vf";
        }
        if (hasil[1] > hasil[2] && hasil[1] > 0){
            hasilakhir = "pvc";
        }
        if (hasil[3] > 0){
            hasilakhir = "heartblock";
        }
        switch (hasilakhir) {
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
        }
    }
    void setupMqtt(String deviceId) {

//        MQTT SERVER
        OfflineMode om = new OfflineMode();
        om.turnOnWifi(DetailActivityOffline.this);
        om.runMQTTServer();
//        MQTT RELATED
        String topicPrefix = AppSetting.getTopic(DetailActivityOffline.this);
        String bpm = topicPrefix + "/bpm";
        String notification = topicPrefix + "/n";
        String ecg = topicPrefix + "/ecg";
        System.out.println(bpm);
        subscribedTopic.add(bpm);
        subscribedTopic.add(notification);
        subscribedTopic.add(ecg);
        if (mqttClient == null) {
            mqttClient = AppSetting.getMqttOffline(DetailActivityOffline.this);
            try {
                Log.i("MQTT", "SETUP");
                System.out.println("Setup Mqtt");
                System.out.println();
                Toast.makeText(getApplicationContext(), "Setup MQTT", Toast.LENGTH_SHORT).show();
                mqttClient.connect(DetailActivityOffline.this, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        AppSetting.dismissProgressDialog();
                        Log.i("MQTT", "Connected");
                        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
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
                        lineChart.setDrawBorders(false);
                        t.scheduleAtFixedRate(ttask, 10, 10);
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
            } catch (MqttException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.i("Detail", "onDestroy: ");
        for (String topic : subscribedTopic) {
            Log.i("Detail", "onDestroy: " + topic);
            try {
                mqttClient.unsubscribe(topic);
            } catch (MqttException ex) {
                // do nothing
                // un-subscribe failed
            }
        }

        if (mqttClient != null) {
            mqttClient.unregisterResources();
            mqttClient.close();
        }
        super.onDestroy();
    }

    private class MyAdapter extends SparkAdapter {

        @Override
        public RectF getDataBounds() {
            final int count = getCount();

            float minY = -3f;
            float maxY = 3f;
            float minX = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            for (int i = 0; i < count; i++) {
                final float x = getX(i);
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);

                final float y = getY(i);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }

            return new RectF(minX, minY, maxX, maxY);
        }

        @Override
        public int getCount() {
            return ecgData.size();
        }

        @Override
        public Object getItem(int index) {
            return ecgData.get(index);
        }

        @Override
        public float getY(int index) {
            return ecgData.get(index);
        }
    }

    int Z = 0;

    public class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            if (ecgAllData.size() > 3) {
                linedata.addEntry(new Entry(Z, ecgAllData.get(0)), 0);//INSERT LAST
                Z++;
                    ecgAllData.remove(0);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        lineChart.setData(linedata);
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

        /*DETAIL INFORMATION*/
        setupDetail();
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
            case R.id.action_about:
                startActivity(new Intent(DetailActivityOffline.this, AboutActivity.class));
                break;
            case R.id.action_logout:
                AppSetting.setLogin(DetailActivityOffline.this, AppSetting.LOGGED_OUT);
            case R.id.action_mode:
                startActivity(new Intent(DetailActivityOffline.this, DetailActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
