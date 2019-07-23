package com.ipat.dhakarhythmppgganteng;

/**
 * Created by ipat on 07/12/17.
 */

import android.content.*;
import android.net.wifi.*;

import java.lang.reflect.*;

public class ApManager {

    //check whether wifi hotspot on or off
    public static boolean isApOn(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        } catch (Throwable ignored) {
        }
        return false;
    }
    // toggle wifi hotspot on
    public static boolean setupWifi(String ssid, String password,Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        WifiConfiguration wificonfiguration = new WifiConfiguration();
        wificonfiguration.SSID = ssid;
        wificonfiguration.preSharedKey  = password;
        wificonfiguration.hiddenSSID = true;
        wificonfiguration.status = WifiConfiguration.Status.ENABLED;
        wificonfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wificonfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wificonfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wificonfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wificonfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wificonfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        try {
            if (!isApOn(context)) {
                Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method.invoke(wifimanager, wificonfiguration, !isApOn(context));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public static boolean wifiOff(Context context){
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        if (isApOn(context)) {
            wifimanager.setWifiEnabled(false);
            return true;
        }
        return false;
    }
} // end of class
