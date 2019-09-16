package com.faishalrachman.amonsecg;

import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.faishalrachman.amonsecg.fragment.DataFragment;
import com.faishalrachman.amonsecg.fragment.TutorialFragment;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.relex.circleindicator.CircleIndicator;

public class RegisterActivity extends AppCompatActivity {
    public static final String TAG = RegisterActivity.class.getSimpleName();

    @BindView(R.id.view_pager) ViewPager viewPager;
    @BindView(R.id.pager_indicator) CircleIndicator pagerIndicator;
    @BindView(R.id.button_next) ImageView nextButton;

    DataFragment dataFragment;

    @OnClick(R.id.button_next) void onNextClick(){
        int position = viewPager.getCurrentItem();
        if (position < fragmentList.size()-1){
            viewPager.setCurrentItem(position+1, true);
        }else {
            TextInputLayout[] inputLayouts = {
                    dataFragment.textUserFullName,
                    dataFragment.textEmail,
                    dataFragment.textUserPassword,
                    dataFragment.textUserDeviceId,
                    dataFragment.textUserAddress,
                    dataFragment.textUserPhone,
                    dataFragment.textUserEmergencyPhone,
                    dataFragment.textUserAge
            };
            boolean anyEmpty = false;
            for (TextInputLayout inputLayout: inputLayouts) {
                inputLayout.setError(null);
                if (TextUtils.isEmpty(inputLayout.getEditText().getText())){
                    inputLayout.setError(getString(R.string.error_form));
                    anyEmpty = true;
                }
            }
            if (!anyEmpty) {
                String gender = dataFragment.spinnerUserGender.getSelectedItem().toString().equals("Male") ? "1" : "2";
                String id_doctor = dataFragment.spinnerDoctor.getSelectedItem().toString().split(":")[0];

                AppSetting.showProgressDialog(RegisterActivity.this, "Registering");
                AndroidNetworking.get(AppSetting.getHttpAddress(RegisterActivity.this)
                        +getString(R.string.register_url))
                        .addQueryParameter("nama_pasien", dataFragment.textUserFullName.getEditText().getText().toString())
                        .addQueryParameter("email", dataFragment.textEmail.getEditText().getText().toString())
                        .addQueryParameter("password", dataFragment.textUserPassword.getEditText().getText().toString())
                        .addQueryParameter("device_id", dataFragment.textUserDeviceId.getEditText().getText().toString())
                        .addQueryParameter("alamat", dataFragment.textUserAddress.getEditText().getText().toString())
                        .addQueryParameter("jenis_kelamin", gender)
                        .addQueryParameter("id_dokter", id_doctor)
                        .addQueryParameter("phone", dataFragment.textUserPhone.getEditText().getText().toString())
                        .addQueryParameter("emergency_phone", dataFragment.textUserEmergencyPhone.getEditText().getText().toString())
                        .addQueryParameter("usia", dataFragment.textUserAge.getEditText().getText().toString())
                        .setPriority(Priority.MEDIUM)
                        .build()
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                                AppSetting.dismissProgressDialog();
                                // do anything with response
                                Log.i("REGISTER", "onResponse: "+response.toString());
                                Toast.makeText(RegisterActivity.this, "Register Success", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                            @Override
                            public void onError(ANError error) {
                                // handle error
                                Log.i("REGISTER", "onError: "+ error.getErrorCode());
                                Log.i("REGISTER", "onError: "+ error.getErrorDetail());
                                Log.i("REGISTER", "onError: "+ error.getErrorBody());
                            }
                        });
            }
        }
    }

    List<Fragment> fragmentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        dataFragment = new DataFragment();

        fragmentList = new ArrayList<>();
        fragmentList.add(TutorialFragment.newInstance(R.drawable.boy0, R.string.tutor1));
        fragmentList.add(TutorialFragment.newInstance(R.drawable.boy1, R.string.tutor2));
        fragmentList.add(TutorialFragment.newInstance(R.drawable.girl1, R.string.tutor3));
        fragmentList.add(TutorialFragment.newInstance(R.drawable.girl0, R.string.tutor4));
        fragmentList.add(dataFragment);

        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragmentList.get(position);
            }

            @Override
            public int getCount() {
                return fragmentList.size();
            }
        });
        pagerIndicator.setViewPager(viewPager);

        viewPager.addOnPageChangeListener(pageChangeListener);
    }

    ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            if (position < fragmentList.size()-1)
                nextButton.setImageResource(R.drawable.ic_action_next);
            else
                nextButton.setImageResource(R.drawable.ic_action_done);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
}
