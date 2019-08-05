package com.faishalrachman.amonsecg.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.faishalrachman.amonsecg.AppSetting;

public class CoreRestarter extends BroadcastReceiver {
    public final static String TAG = "CoreRestarter";
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(CoreRestarter.class.getSimpleName(), "Service Stops! Oooooooooooooppppssssss!!!!");
            Log.d(TAG, "onReceive: Restart because of logged in");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "onReceive: Restarting Foreground");
                context.startForegroundService(new Intent(context, CoreService.class));
            } else {
                context.startService(new Intent(context, CoreService.class));

            }

    }
}
