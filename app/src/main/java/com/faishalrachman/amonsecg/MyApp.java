package com.faishalrachman.amonsecg;

import android.app.Application;

import com.androidnetworking.AndroidNetworking;

import org.eclipse.paho.android.service.MqttAndroidClient;

/**
 * Created by maakbar on 11/20/16.
 * Application class, global initialization area
 */

public class MyApp extends Application {
    private MqttAndroidClient mClient;

    @Override
    public void onCreate() {
        super.onCreate();
        // initialize http client
        AndroidNetworking.initialize(getApplicationContext());
        // initialize mqtt client
//        mClient = setupMqttCallBack(getApplicationContext());
    }

//    public final MqttAndroidClient getClient() {
//        return mClient;
//    }
}
