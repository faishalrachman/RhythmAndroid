package com.ipat.dhakarhythmppgganteng;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.text_email)
    TextInputLayout textEmail;
    @BindView(R.id.text_user_password)
    TextInputLayout textUserPass;

    @OnClick(R.id.button_login)
    void onLoginClick() {
        if (TextUtils.isEmpty(textEmail.getEditText().getText().toString())) {
            textUserPass.setError(null);
            textEmail.setError(getString(R.string.error_form));
        } else if (TextUtils.isEmpty(textUserPass.getEditText().getText().toString())) {
            textEmail.setError(null);
            textUserPass.setError(getString(R.string.error_form));
        } else {
            textEmail.setError(null);
            textUserPass.setError(null);

            AppSetting.showProgressDialog(LoginActivity.this, "Logging in");

            AndroidNetworking.get(AppSetting.getHttpAddress(LoginActivity.this)
                    + getString(R.string.login_url))
                    .addQueryParameter("email", textEmail.getEditText().getText().toString())
                    .addQueryParameter("password", textUserPass.getEditText().getText().toString())
                    .setPriority(Priority.MEDIUM).build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // do anything with response
                            AppSetting.dismissProgressDialog();
                            Log.i("LOGIN", "onResponse: " + response.toString());
                            try {
                                if (!response.getString("sukses").equals("3")) {
                                    AppSetting.setLogin(LoginActivity.this, AppSetting.LOGGED_IN);
                                    AppSetting.saveAccount(LoginActivity.this, textEmail.getEditText().getText().toString(),
                                            textUserPass.getEditText().getText().toString());
                                    moveToDetailActivity();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Email / Password Salah",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException ex) {
                                ex.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(ANError error) {
                            // handle error
                            AppSetting.dismissProgressDialog();
                            try {
                                JSONObject response = new JSONObject(error.getErrorBody());
                                System.out.println(response.toString());
//                                if (response.getString("info").equals("username")){
//                                    textEmail.setError("Username doesn't exist");
//                                }else {
//                                    textUserPass.setError("Wrong password");
//                                }
                            } catch (JSONException ex) {
                                ex.printStackTrace();
                            }
                            // handle error
                            Log.i("LOGIN", "onError: " + error.getErrorCode());
                            Log.i("LOGIN", "onError: " + error.getErrorDetail());
                            Log.i("LOGIN", "onError: " + error.getErrorBody());
                        }
                    });
        }

    }

    @OnClick(R.id.button_register)
    void onRegisterClick() {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
    }


    @OnClick(R.id.logo_jantung)
    void setupIp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle(getString(R.string.setup_ip));
        SharedPreferences pref = getApplicationContext().getSharedPreferences("JANTUNG PREF", Context.MODE_PRIVATE);
        // Set up the input
        final TextInputEditText inputIp = new TextInputEditText(this);
        inputIp.setInputType(InputType.TYPE_CLASS_TEXT);
//        inputIp.setHint(R.string.example_ip);
        inputIp.setHint("IP Server Web");
        inputIp.setText(pref.getString("ip", ""));
        final TextInputEditText inputPort = new TextInputEditText(this);
        inputPort.setInputType(InputType.TYPE_CLASS_PHONE);
//        inputPort.setHint(R.string.example_port);
        inputPort.setHint("Port Mosquitto");
        inputPort.setText(pref.getString("port", ""));
        final TextInputEditText inputTopic = new TextInputEditText(this);
        inputTopic.setInputType(InputType.TYPE_CLASS_TEXT);
//        inputTopic.setHint(R.string.example_topic);
        inputTopic.setHint("Topik (rhythm/ECG001)");
        inputTopic.setText(pref.getString("topic", ""));
        final TextInputEditText mqttserver = new TextInputEditText(this);
        mqttserver.setInputType(InputType.TYPE_CLASS_TEXT);
        mqttserver.setText(pref.getString("mqttserver", ""));
//        mqttserver.setHint(R.string.example_ip);
        mqttserver.setHint("IP MQTT");

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(inputIp);
        linearLayout.addView(inputPort);
        linearLayout.addView(mqttserver);
        linearLayout.addView(inputTopic);

        builder.setView(linearLayout);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // save a new ip and port
                if (!TextUtils.isEmpty(inputIp.getText().toString()) && !TextUtils.isEmpty(inputPort.getText().toString()))
                    AppSetting.saveIp(LoginActivity.this, inputIp.getText().toString(), inputPort.getText().toString(), mqttserver.getText().toString(), inputTopic.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    void moveToDetailActivity() {
        startActivity(new Intent(LoginActivity.this, DetailActivity.class));
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSetting.isLoggedIn(LoginActivity.this)) {
            setContentView(R.layout.activity_login);
            ButterKnife.bind(this);
            setTitle(getString(R.string.login));
        } else {
            moveToDetailActivity();
        }
    }
}
