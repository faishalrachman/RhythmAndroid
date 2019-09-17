package com.faishalrachman.amonsecg.utils;

import android.content.Intent;

public class IntentHelper {

    public static Intent createIntent(String action, String name, String message){
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(name, message);
        return intent;
    }
}
