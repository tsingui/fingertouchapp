package com.marekv1.fingertouch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.marekv1.fingertouch.Settings.SettingsActivity;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = (Button) findViewById(R.id.startForeground);
        Button stopButton = (Button) findViewById(R.id.stopForeground);
        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startForeground:
                Intent startIntent = new Intent(MainActivity.this, FingertouchService.class);
                startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                startService(startIntent);
                break;
            case R.id.stopForeground:
                Intent stopIntent = new Intent(MainActivity.this, FingertouchService.class);
                stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                startService(stopIntent);
                break;

            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settingsButton:
                showSettings();
                return true;

            case R.id.menu_about:
                displayAboutDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void displayAboutDialog() {
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (pInfo != null) {
            builder.setTitle(getString(R.string.about_title) + " " + pInfo.versionName);
        } else {
            builder.setTitle(getString(R.string.about_title));
        }
        builder.setMessage(getString(R.string.about_desc));

//        builder.setPositiveButton("GET PRO", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                try {
//                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.marekv1.fingertouchpro")));
//                } catch (android.content.ActivityNotFoundException anfe) {
//                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.marekv1.fingertouchpro")));
//                }
//                dialog.cancel();
//            }
//        });


//        builder.setNegativeButton("No Thanks!", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                dialog.cancel();
//            }
//        });

        builder.show();
    }
}
