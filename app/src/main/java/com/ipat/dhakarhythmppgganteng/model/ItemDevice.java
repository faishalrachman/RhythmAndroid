package com.ipat.dhakarhythmppgganteng.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

/**
 * Created by maakbar on 11/8/16.
 */

public class ItemDevice extends Item{
    public String full_name;
    public String email;
    public String idPasient;
    public String deviceId;
    public String address;
    public String phone;
    public String emergencyPhone;
    public String age;
    public String doctorId;
    private boolean isMale;

    private int condition;
    private float rate;

    public ItemDevice(String name, int itemType) {
        super(name, itemType);
    }

    public ItemDevice(String name, String full_name, int itemType, String deviceId, boolean isMale) {
        super(name, itemType);
        this.deviceId = deviceId;
        this.full_name = full_name;
        this.isMale = isMale;

        // later change from mqtt data
        Random random = new Random();
        this.rate = random.nextInt(120 /*max*/ - 60 /*min*/ +1) + 70 /*min*/;
        this.condition = random.nextInt(2);  // from 0 to 2 [3 class]
    }

    public boolean isMale() {
        return isMale;
    }

    public int getCondition() {
        return condition;
    }

    public void setCondition(int condition) {
        this.condition = condition;
    }

    public float getRate() {
        return rate;
    }

    public void setRate(float rate) {
        this.rate = rate;
    }

    public void setRateRandom() {
        // later change from mqtt data
        Random random = new Random();
        this.rate = random.nextInt(120 /*max*/ - 70 /*min*/ +1) + 70 /*min*/;
        this.condition = random.nextInt(2);  // from 0 to 2 [3 class]
    }

    public static ItemDevice jsonToDevice(JSONObject anObject){
        try{
            ItemDevice device = new ItemDevice(anObject.getString("nama_pasien"), 0);
            device.address = anObject.getString("alamat");
            device.phone = anObject.getString("phone");
            device.emergencyPhone = anObject.getString("emergency_phone");
            device.deviceId = anObject.getString("device_id");
            device.isMale = anObject.getString("jenis_kelamin").equals("Laki-laki");
            device.age = anObject.getString("usia");
            return device;
        }catch (JSONException ex){
            ex.printStackTrace();
        }
        return null;
    }
}
