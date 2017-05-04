package com.marekv1.fingertouch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class StartFingertouchServiceAtBoot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences settings = context.getSharedPreferences(Constants.PREFS.PREFS_NAME, 0);
            boolean bootEnabled = settings.getBoolean("bootEnabled", false);

            if (bootEnabled) {
                Intent startIntent = new Intent(context, FingertouchService.class);
                startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                context.startService(startIntent);
            }
        }
    }
}