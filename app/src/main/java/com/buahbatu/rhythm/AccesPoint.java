package com.buahbatu.rhythm;

/**
 * Created by reza on 06/11/16.
 */


import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.lang.reflect.Method;


public class AccesPoint extends AppCompatActivity {
    WifiManager wifiManager;
    Button turnon;
    private String SSID = "";
    private String password = "";

    public boolean configApState(Context context) {

        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        WifiConfiguration wificonfiguration = null;
        try {

            if(isApOn(context)) {
                wifimanager.setWifiEnabled(false);
            }


            Method method = wifimanager.getClass().getMethod("setWifiApEnabled",  WifiConfiguration.class, boolean.class);
            method.invoke(wifimanager, wificonfiguration, !isApOn(context));
            return true;
        }
        catch (Exception e) {

            e.printStackTrace();
        }
        return false;
    }

    public static boolean isApOn(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        }
        catch (Throwable ignored) {}
        return false;
    }

    public boolean changeHotspotconfiguration(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
            Method getConfigMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifiManager);

            this.SSID = wifiConfig.SSID;
            this.password = wifiConfig.preSharedKey;
            Toast.makeText(context, "SSID : "+this.SSID+"\nPassword : "+this.password,
                    Toast.LENGTH_LONG).show();
            wifiConfig.SSID = "Secure";
            wifiConfig.preSharedKey = "haleboop";

            Method setConfigMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            setConfigMethod.invoke(wifiManager, wifiConfig);

            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
