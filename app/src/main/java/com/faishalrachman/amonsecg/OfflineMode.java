package com.faishalrachman.amonsecg;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import io.moquette.BrokerConstants;
import io.moquette.server.config.MemoryConfig;

/**
 * Created by ipat on 07/12/17.
 */

public class OfflineMode {
    private io.moquette.server.Server server;

    private ApManager ap =new ApManager();

    public void turnOnWifi(Context context){
        ap.setupWifi("ECG","12345678",context);
    }
    public void turnOffWifi(Context context){
        ap.wifiOff(context);
    }
    public boolean runMQTTServer(){
        server = new io.moquette.server.Server();
        try {
            MemoryConfig memoryConfig = new MemoryConfig(new Properties());
            memoryConfig.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + BrokerConstants.DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME);
            server.startServer(memoryConfig);
            // server.startServer();//is not working due to DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME;
            Log.d("MQTT","Server Started");
            return true;
        }
        catch (IOException e) { e.printStackTrace();
        return false;}
        catch (Exception e){ e.printStackTrace();
            return false;}
    }
    public boolean stopMQTTServer(){
            server.stopServer();
            return true;
    }

}
