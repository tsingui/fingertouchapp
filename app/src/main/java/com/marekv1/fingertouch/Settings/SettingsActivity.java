package com.marekv1.fingertouch.Settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import com.marekv1.fingertouch.Constants;
import com.marekv1.fingertouch.R;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (settings == null) {
            settings = this.getSharedPreferences(Constants.PREFS.PREFS_NAME, 0);
        }

        Switch bootSwitch = (Switch) findViewById(R.id.bootSwitch);
        boolean bootEnabled = settings.getBoolean("bootEnabled", false);
        if (bootEnabled) {
            bootSwitch.setChecked(true);
        } else {
            bootSwitch.setChecked(false);
        }

        bootSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked){
                    settings.edit().putBoolean("bootEnabled",true).apply();
                }else{
                    settings.edit().putBoolean("bootEnabled",false).apply();
                }
            }
        });

        String delay = settings.getString("delaySelected", "0");

        Spinner spinner1 = (Spinner) findViewById(R.id.spinner1);
        spinner1.setSelection(Integer.parseInt(delay));
        spinner1.setOnItemSelectedListener(new CustomOnItemSelectedListener());

        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } catch (Exception ignored) {
        }
    }

}
