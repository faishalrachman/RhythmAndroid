package com.faishalrachman.amonsecg;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.eclipse.paho.android.service.MqttAndroidClient;

import java.util.Locale;
import java.util.Random;

public class AppSetting {
    private static String PREFERENCE_NAME = "JANTUNG PREF";

    static boolean LOGGED_IN = true;
    static boolean LOGGED_OUT = false;
    private static ProgressDialog dialog;

    public static MqttAndroidClient getMqttClient(Context context) {
        //            String clientId = MqttClient.generateClientId();
        System.out.println("mqtt address " + AppSetting.getMqttAddress(context));
        AppSetting.AccountInfo accountInfo = AppSetting.getSavedAccount(context);
        // mqtt client
        Random ran = new Random();
        MqttAndroidClient mqttClient = new MqttAndroidClient(context,
                /*MQTT SERVER ADDRESS*/
                AppSetting.getMqttAddress(context),
                /*MQTT CLIENT ID*/
                accountInfo.username + "/" + "uadshowaidhoqwwhoduqwhd" + ran.nextInt());

        System.out.println(accountInfo.username);
        return mqttClient;
    }

    static MqttAndroidClient getMqttOffline(Context context) {
        System.out.println("mqtt address : localhost");
        AppSetting.AccountInfo accountInfo = AppSetting.getSavedAccount(context);
        // mqtt client
        MqttAndroidClient mqttClient = new MqttAndroidClient(context, "tcp://localhost:1883", "ECG_OFFLINE");
        System.out.println(accountInfo.username);
        return mqttClient;
    }

    private static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(6);
        char tempChar;
        for (int i = 0; i < randomLength; i++) {
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    static void saveIp(Context context, String ip, String port, String mqttserver, String topic) {
        SharedPreferences.Editor edit = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit();
        edit.putString("ip", ip);
        edit.putString("port", port);
        edit.putString("topic", topic);
        edit.putString("mqttserver", mqttserver);
        edit.apply();
    }

    public static String getTopic(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        String topic = pref.getString("topic", "rhythm/ECG001");
        return topic;
    }

    public static String getBluetoothDeviceName(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        String topic = pref.getString("bluetoothDeviceName", "ECG001");
        return topic;
    }
    public static String getSession(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        String topic = pref.getString("session", "");
        return topic;
    }

    public static String getHttpAddress(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        String ip = pref.getString("ip", "telemedicine.co.id:3000");
        if (ip.equals(""))
            ip = context.getString(R.string.server_ip_address);
        return String.format(Locale.US, context.getString(R.string.http_url), ip);
//        return String.format(Locale.US, "http://%s/mobileapi", ip);
//        return String.format(Locale.US, "http://%s/mobileapi", ip);
    }

    static String getMqttAddress(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        String ip = pref.getString("mqttserver", "telemedicine.co.id");
        String port = pref.getString("port", "49877");
        if (ip.equals(""))
            ip = context.getString(R.string.server_ip_address);
        if (port.equals(""))
            port = context.getString(R.string.server_http_port);
//        return String.format(Locale.US, context.getString(R.string.mqtt_url), ip);
//        String ip = "telemedicinecoid-in.cloud.revoluz.io";
//        String port = "49560";
        return String.format(Locale.US, "tcp://%s:%s", ip, port);
    }

    static void showProgressDialog(Context context, String message) {
        dialog = new ProgressDialog(context, android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        dialog.setMessage(message);
        dialog.show();
    }

    static void makeACall(Activity context, String number) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
//        intent.setData(Uri.parse("tel:" + number));
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE);
        if (res == PackageManager.PERMISSION_GRANTED)
            context.startActivity(intent);
        else ActivityCompat.requestPermissions(context,
                new String[]{Manifest.permission.READ_CONTACTS}, 0 /*REQUEST CODE*/);
    }

    static void makeASms(Context context, String number) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + number));
        context.startActivity(intent);
    }

    static void dismissProgressDialog() {
        dialog.dismiss();
    }

    public static boolean isLoggedIn(Context context) {
        return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).getBoolean("isLoggedIn", false);
    }

    static void setLogin(Context context, boolean isLoggedIn) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("isLoggedIn", isLoggedIn);
        editor.apply();
    }

    static void setBluetoothDeviceName(Context context, String name) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("bluetoothDeviceName", name);
        editor.apply();
    }

    static void saveAccount(Context context, String username, String password) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("username", username);
        editor.putString("password", password);
        editor.apply();
    }
    static void saveSession(Context context, String api_key) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("session", api_key);
        editor.apply();
    }
    public static void setRecordingStatus(Context context, Boolean api_key) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("is_recording", api_key);
        editor.apply();
    }

    public static Boolean getRecordingStatus(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean("is_recording", false);
    }
    public static void setRecordFilename(Context context, String api_key) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("filename", api_key);
        editor.apply();
    }

    public static String getRecordFilename(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return pref.getString("filename", "unknown");
    }
    static AccountInfo getSavedAccount(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return new AccountInfo(pref.getString("username", ""), pref.getString("password", ""));
    }

    static class AccountInfo {
        public String full_name;
        String password;
        String username;

        AccountInfo(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
    public static String getTimestamp(){

        Long tsLong = System.currentTimeMillis() / 1000;
        String ts = tsLong.toString();
        return ts;
    }


}
