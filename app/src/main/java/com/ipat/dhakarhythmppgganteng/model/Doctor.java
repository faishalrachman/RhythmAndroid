package com.ipat.dhakarhythmppgganteng.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by maakbar on 8/12/17.
 */

public class Doctor {
    public String name;
    public String email;
    public String id;

    public Doctor(String name, String email, String id) {
        this.name = name;
        this.email = email;
        this.id = id;
    }

    public static Doctor jsonToDoctor(JSONObject anObject){
        try {
            Doctor doctor = new Doctor(anObject.getString("nama_dokter"), anObject.getString("email"),
                    anObject.getString("id_dokter"));

            return doctor;
        }catch (JSONException ex){
            ex.printStackTrace();
            return new Doctor("", "", "");
        }
    }
}
