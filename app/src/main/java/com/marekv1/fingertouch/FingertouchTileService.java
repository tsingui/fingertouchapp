package com.marekv1.fingertouch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;


public class FingertouchTileService extends TileService {
    public static final String PREFS_NAME = "fingertouchSettings";
    private SharedPreferences settings;
    private String status;

    @Override
    public void onCreate() {
        super.onCreate();
        settings=this.getSharedPreferences(PREFS_NAME, 0);
    }

    //Called when the user adds this tile to Quick Settings.
    @Override
    public void onTileAdded() {
        settings = getSharedPreferences(PREFS_NAME, 0);
        status = settings.getString("serviceStatus", "unknown");
        if (status.equals("running")) {
            this.getQsTile().setState(Tile.STATE_ACTIVE);
        } else if (status.equals("paused") || status.equals("disabled")) {
            this.getQsTile().setState(Tile.STATE_INACTIVE);
        } else {
            this.getQsTile().setState(Tile.STATE_UNAVAILABLE);
        }
        this.getQsTile().updateTile();
    }

    //Called each time tile is visible
    @Override
    public void onStartListening() {
        settings = getSharedPreferences(PREFS_NAME, 0);
        status = settings.getString("serviceStatus", "unknown");
        if (status.equals("running")) {
            this.getQsTile().setState(Tile.STATE_ACTIVE);
        } else if (status.equals("paused") || status.equals("disabled")) {
            this.getQsTile().setState(Tile.STATE_INACTIVE);
        } else {
            this.getQsTile().setState(Tile.STATE_UNAVAILABLE);
        }
        this.getQsTile().updateTile();
    }

    //Called when the user clicks on this tile.
    @Override
    public void onClick () {
        if (!isLocked()) { //Checks if the lock screen is showing.

            Intent baseIntent = new Intent(this, FingertouchService.class);
            baseIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
            startService(baseIntent);

            settings = getSharedPreferences(PREFS_NAME, 0);
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

