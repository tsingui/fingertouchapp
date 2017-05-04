package com.marekv1.fingertouch.Settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.marekv1.fingertouch.Constants;
import com.marekv1.fingertouch.FingertouchService;

class CustomOnItemSelectedListener implements OnItemSelectedListener {

    private SharedPreferences settings;

    public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
        if (settings == null) {
            settings = parent.getContext().getSharedPreferences(Constants.PREFS.PREFS_NAME, 0);
        }
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("delaySelected",String.valueOf(pos));
        editor.commit();

        Intent updateDelay = new Intent(parent.getContext(), FingertouchService.class);
        updateDelay.setAction(Constants.ACTION.UPDATE_DELAY);
        parent.getContext().startService(updateDelay);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

}
