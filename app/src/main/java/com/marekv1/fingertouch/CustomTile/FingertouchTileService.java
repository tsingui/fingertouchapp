package com.marekv1.fingertouch.CustomTile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.marekv1.fingertouch.Constants;
import com.marekv1.fingertouch.FingertouchService;


public class FingertouchTileService extends TileService {
    public static final String PREFS_NAME = "fingertouchSettings";
    private SharedPreferences settings;
    private String status;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    //Called when the user adds this tile to Quick Settings.
    @Override
    public void onTileAdded() {
        Intent updateStatus = new Intent(this, FingertouchService.class);
        startService(updateStatus.setAction(Constants.ACTION.UPDATE_STATUS));

        if (settings == null) {
            settings = this.getSharedPreferences(PREFS_NAME, 0);
        }
        status = settings.getString("serviceStatus", "unknown");
        int state;
        if (status.equals("running")) {
            state = Tile.STATE_ACTIVE;
        } else {
            state = Tile.STATE_INACTIVE;
        }
        try {
            getQsTile().setState(state);
            getQsTile().updateTile();
        } catch (Exception ignored) {
        }
    }

    //Called each time tile is visible
    @Override
    public void onStartListening() {
        if (settings == null) {
            settings = this.getSharedPreferences(PREFS_NAME, 0);
        }
        status = settings.getString("serviceStatus", "unknown");
        int state;
        if (status.equals("running")) {
            state = Tile.STATE_ACTIVE;
        } else {
            state = Tile.STATE_INACTIVE;
        }
        try {
            getQsTile().setState(state);
            getQsTile().updateTile();
        } catch (Exception ignored) {
        }
    }

    //Called when the user clicks on this tile.
    @Override
    public void onClick () {
        if (!isLocked()) { //Checks if the lock screen is showing.

            Intent baseIntent = new Intent(this, FingertouchService.class);
            baseIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
            startService(baseIntent);

            if (settings == null) {
                settings = this.getSharedPreferences(PREFS_NAME, 0);
            }
            status = settings.getString("serviceStatus", "unknown");
            if (status.equals("running")) {
                Intent intent = new Intent(this, FingertouchService.class);
                intent.setAction(Constants.ACTION.PAUSE_ACTION);
                startService(intent);
            } else if (status.equals("paused")) {
                Intent intent = new Intent(this, FingertouchService.class);
                intent.setAction(Constants.ACTION.START_ACTION);
                startService(intent);
            }
        }
    }

    //Called when tile is no longer visible
    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    //Called when the user removes this tile from Quick Settings.
    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }


}

