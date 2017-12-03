package com.buahbatu.rhythm.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.buahbatu.rhythm.AppSetting;
import com.buahbatu.rhythm.R;
import com.buahbatu.rhythm.model.Doctor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by maakbar on 8/12/17.
 */

public class DataFragment extends Fragment {
    public static final String TAG = DataFragment.class.getSimpleName();

    @BindView(R.id.text_full_name) public TextInputLayout textUserFullName;
    @BindView(R.id.text_email) public TextInputLayout textEmail;
    @BindView(R.id.text_user_password) public TextInputLayout textUserPassword;
    @BindView(R.id.text_device_id) public TextInputLayout textUserDeviceId;
    @BindView(R.id.text_address) public TextInputLayout textUserAddress;
    @BindView(R.id.spinner_gender) public Spinner spinnerUserGender;
    @BindView(R.id.text_user_phone) public TextInputLayout textUserPhone;
    @BindView(R.id.text_emergency_phone) public TextInputLayout textUserEmergencyPhone;
    @BindView(R.id.text_user_age) public TextInputLayout textUserAge;
    @BindView(R.id.spinner_doctor) public Spinner spinnerDoctor;

    List<Doctor> doctorList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data, container, false);
        TextView textView = (TextView) view.findViewById(R.id.terms_check);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(Html.fromHtml(getString(R.string.terms_check)));
        ButterKnife.bind(this, view);

        loadDoctorId(spinnerDoctor);

        return view;
    }

    void loadDoctorId(final Spinner spinnerDoctor){

        AndroidNetworking.get(AppSetting.getHttpAddress(getContext())
                +getString(R.string.doctor_data)).setPriority(Priority.MEDIUM).build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try{
                            JSONArray dataDoctor = response.getJSONArray("data_dokter");

                            List<String> spinnerArray = new ArrayList<>();
                            for (int i = 0; i < dataDoctor.length(); i++) {
                                Doctor aDoctor = Doctor.jsonToDoctor(dataDoctor.getJSONObject(i));
                                doctorList.add(aDoctor);
                                spinnerArray.add(aDoctor.id+":  "+aDoctor.name);
                            }

                            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(),
                                    android.R.layout.simple_spinner_item, spinnerArray);

                            spinnerDoctor.setAdapter(spinnerAdapter);

//                            System.out.println(dataDoctor.toString());
                        }catch (JSONException ex){
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        // handle error
                        Log.i(TAG, "onError: "+ anError.getErrorCode());
                        Log.i(TAG, "onError: "+ anError.getErrorDetail());
                        Log.i(TAG, "onError: "+ anError.getErrorBody());
                    }
                });
    }
}
