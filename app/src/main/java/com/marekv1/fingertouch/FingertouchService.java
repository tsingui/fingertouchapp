package com.marekv1.fingertouch;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.service.quicksettings.TileService;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.marekv1.fingertouch.CustomTile.FingertouchTileService;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;

public class FingertouchService extends Service implements Handler.Callback {
    private SharedPreferences settings;

    private PowerManager pm;
    private SpassFingerprint mSpassFingerprint;
    private Context mContext;
    private String eventStatusName;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationCompat.Action playAction;
    private NotificationCompat.Action pauseAction;

    private boolean onReadyIdentify = false;
    private boolean isServiceEnabled = false;
    private boolean isServicePaused = false;

    private Handler mHandler;
    private static final int MSG_AUTH = 1000;
    private static final int MSG_CANCEL = 1003;

    private int delay = 100;

    private boolean hasRegisteredFinger = false;

    private final BroadcastReceiver mPassReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                mHandler.sendEmptyMessageDelayed(MSG_AUTH, delay);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mHandler.sendEmptyMessageDelayed(MSG_CANCEL, delay);
            }
        }
    };

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mPassReceiver, filter);
    }

    private void unregisterBroadcastReceiver() {
        try {
            if (mContext != null) {
                mContext.unregisterReceiver(mPassReceiver);
            }
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    private final SpassFingerprint.IdentifyListener mIdentifyListener = new SpassFingerprint.IdentifyListener() {
        @Override
        public void onFinished(int eventStatus) {
//            eventStatusName = getEventStatusName(eventStatus);
//            Log.i(Constants.PREFS.LOG_TAG, "identify finished : reason =" + eventStatusName);
/*
            if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS) {
//                Log.i(Constants.PREFS.LOG_TAG, "onFinished() : Identify authentification Success");
            } else if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS) {
//                Log.i(Constants.PREFS.LOG_TAG, "onFinished() : Password authentification Success");
            } else if (eventStatus == SpassFingerprint.STATUS_OPERATION_DENIED) {
//                Log.i(Constants.PREFS.LOG_TAG, "onFinished() : Authentification is blocked because of fingerprint service internally.");
            } else if (eventStatus == SpassFingerprint.STATUS_USER_CANCELLED) {
//                Log.i(Constants.PREFS.LOG_TAG, "onFinished() : User cancel this identify.");
            } else if (eventStatus == SpassFingerprint.STATUS_TIMEOUT_FAILED) {
//                Log.i(Constants.PREFS.LOG_TAG, "onFinished() : The time for identify is finished.");
            } else if (eventStatus == SpassFingerprint.STATUS_QUALITY_FAILED) {
//                Log.i(Constants.PREFS.LOG_TAG, "onFinished() : Authentification Fail for identify.");
//                FingerprintGuideText = mSpassFingerprint.getGuideForPoorQuality();
//                Toast.makeText(mContext, FingerprintGuideText, Toast.LENGTH_SHORT).show();
            } else {
//                Log.i(Constants.PREFS.LOG_TAG, "onFinished() : Authentification Fail for identify, reason = " + eventStatusName);
            }
*/
            onReadyIdentify = false;
            mHandler.sendEmptyMessageDelayed(MSG_AUTH, delay);
        }

        @Override
        public void onReady() {
//            Log.i(Constants.PREFS.LOG_TAG, "identify state is ready");
        }

        @Override
        public void onStarted() {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.HOME");
            intent.setFlags(268435456);
            mContext.startActivity(intent);
            mContext.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));

            try {
                mSpassFingerprint.cancelIdentify();
            } catch (IllegalStateException ise) {
//                Log.i(Constants.PREFS.LOG_TAG, ise.getMessage());
            }
//            Log.i(Constants.PREFS.LOG_TAG, "User touched fingerprint sensor");
        }

        @Override
        public void onCompleted() {
//            Log.i(Constants.PREFS.LOG_TAG, "the identify is completed");
        }
    };

    private static String getEventStatusName(int eventStatus) {
        switch (eventStatus) {
            case SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS:
                return "STATUS_AUTHENTIFICATION_SUCCESS";
            case SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS:
                return "STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS";
            case SpassFingerprint.STATUS_TIMEOUT_FAILED:
                return "STATUS_TIMEOUT";
            case SpassFingerprint.STATUS_SENSOR_FAILED:
                return "STATUS_SENSOR_ERROR";
            case SpassFingerprint.STATUS_USER_CANCELLED:
                return "STATUS_USER_CANCELLED";
            case SpassFingerprint.STATUS_QUALITY_FAILED:
                return "STATUS_QUALITY_FAILED";
            case SpassFingerprint.STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE:
                return "STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE";
            case SpassFingerprint.STATUS_BUTTON_PRESSED:
                return "STATUS_BUTTON_PRESSED";
            case SpassFingerprint.STATUS_OPERATION_DENIED:
                return "STATUS_OPERATION_DENIED";
            case SpassFingerprint.STATUS_AUTHENTIFICATION_FAILED:
            default:
                return "STATUS_AUTHENTIFICATION_FAILED";
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_AUTH:
                startIdentify();
                break;
            case MSG_CANCEL:
                cancelIdentify();
                break;
        }
        return true;
    }

    private void startIdentify() {
        if (isServiceEnabled && !isServicePaused && !onReadyIdentify && pm.isInteractive()) {
            try {
                onReadyIdentify = true;
                if (mSpassFingerprint != null) {
                    mSpassFingerprint.startIdentify(mIdentifyListener);
                }
            } catch (IllegalStateException e) {
                onReadyIdentify = false;
//                Log.i(Constants.PREFS.LOG_TAG, "Exception: " + e);
                mHandler.sendEmptyMessageDelayed(MSG_AUTH, 1000);
            }
        }
    }

    private void cancelIdentify() {
        if (onReadyIdentify) {
            try {
                if (mSpassFingerprint != null) {
                    mSpassFingerprint.cancelIdentify();
                }
//                Log.i(Constants.PREFS.LOG_TAG, "cancelIdentify is called");
            } catch (IllegalStateException ise) {
//                Log.i(Constants.PREFS.LOG_TAG, ise.getMessage());
            }
            onReadyIdentify = false;
        }
    }

    private String getServiceStatus() {
        String serviceStatus;
        if (isServiceEnabled && !isServicePaused) {
            serviceStatus = "running";
        } else if (isServiceEnabled) {
            serviceStatus = "paused";
        } else {
            serviceStatus = "disabled";
        }
        return serviceStatus;
    }

    private void notifyQStile() {
        if (settings == null) {
            settings = this.getSharedPreferences(Constants.PREFS.PREFS_NAME, 0);
        }
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("serviceStatus",getServiceStatus());
        editor.commit();
        TileService.requestListeningState(this,
                new ComponentName(this,FingertouchTileService.class));
    }

    private void updateDelay() {
        if (settings == null) {
            settings = this.getSharedPreferences(Constants.PREFS.PREFS_NAME, 0);
        }
        String selected = settings.getString("delaySelected", "0");
        String[] delayArray = getResources().getStringArray(R.array.delay_arrays);
        delay = Integer.parseInt(delayArray[Integer.parseInt(selected)]);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.UPDATE_STATUS)) {
            if (settings == null) {
                settings = this.getSharedPreferences(Constants.PREFS.PREFS_NAME, 0);
            }
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("serviceStatus",getServiceStatus());
            editor.commit();

        } else if (isServiceEnabled && intent.getAction().equals(Constants.ACTION.UPDATE_DELAY)) {
            updateDelay();

        } else if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION) && !isServiceEnabled) {
//            Log.i(Constants.PREFS.LOG_TAG, "Received Start Fingertouch Intent ");

            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mContext = this;
            mHandler = new Handler(this);
            Spass mSpass = new Spass();

            try {
                mSpass.initialize(FingertouchService.this);
            } catch (SsdkUnsupportedException e) {
                Log.i(Constants.PREFS.LOG_TAG, "Exception: " + e);
                Toast.makeText(mContext, "Fingerprint Service is not supported on this device.",
                        Toast.LENGTH_SHORT).show();
                stopSelf();
                return START_NOT_STICKY;
            } catch (UnsupportedOperationException e) {
                Log.i(Constants.PREFS.LOG_TAG, "Fingerprint Service is not supported in the device");
                Toast.makeText(mContext, "Fingerprint Service is not supported on this device.",
                        Toast.LENGTH_SHORT).show();
                stopSelf();
                return START_NOT_STICKY;
            }
            boolean isFeatureEnabled_fingerprint = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);

            if (isFeatureEnabled_fingerprint) {
                mSpassFingerprint = new SpassFingerprint(FingertouchService.this);
                Log.i(Constants.PREFS.LOG_TAG, "Fingerprint Service is supported in the device.");
                Log.i(Constants.PREFS.LOG_TAG, "SDK version : " + mSpass.getVersionName());
            } else {
                Log.i(Constants.PREFS.LOG_TAG, "Fingerprint Service is not supported in the device.");
                Toast.makeText(mContext, "Fingerprint Service is not supported on this device.",
                        Toast.LENGTH_SHORT).show();
                stopSelf();
                return START_NOT_STICKY;
            }

            try {
                hasRegisteredFinger = mSpassFingerprint.hasRegisteredFinger();
            } catch (UnsupportedOperationException e) {
                Log.i(Constants.PREFS.LOG_TAG, "Fingerprint Service is not supported in the device.");
                Toast.makeText(mContext, "Fingerprint Service is not supported on this device.",
                        Toast.LENGTH_SHORT).show();
                stopSelf();
                return START_NOT_STICKY;
            }
            if (!hasRegisteredFinger) {
                Log.i(Constants.PREFS.LOG_TAG, "Please register at least one fingerprint in the device");
                Toast.makeText(mContext, "Please register at least one fingerprint in the device",
                        Toast.LENGTH_LONG).show();
                stopSelf();
                return START_NOT_STICKY;
            }

            registerBroadcastReceiver();
            updateDelay();

            isServiceEnabled = true;
            isServicePaused = false;
            mHandler.sendEmptyMessage(MSG_AUTH);



            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Intent playIntent = new Intent(this, FingertouchService.class);
            playIntent.setAction(Constants.ACTION.START_ACTION);
            PendingIntent pplayIntent = PendingIntent.getService(this, 0,
                    playIntent, 0);
            playAction = new NotificationCompat.Action(
                    android.R.drawable.ic_media_play, "Resume", pplayIntent);

            Intent pauseIntent = new Intent(this, FingertouchService.class);
            pauseIntent.setAction(Constants.ACTION.PAUSE_ACTION);
            PendingIntent ppauseIntent = PendingIntent.getService(this, 0,
                    pauseIntent, 0);
            pauseAction = new NotificationCompat.Action(
                    android.R.drawable.ic_media_pause, "Pause", ppauseIntent);

            Intent disableIntent = new Intent(this, FingertouchService.class);
            disableIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
            PendingIntent pdisableIntent = PendingIntent.getService(this, 0,
                    disableIntent, 0);

            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle("Fingertouch Service")
                    .setTicker("Fingertouch Service")
                    .setContentText("service is running")
                    .setSmallIcon(R.drawable.fingerprint_black)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(pauseAction)
                    .addAction(android.R.drawable.ic_notification_clear_all, "DISABLE", pdisableIntent);

            mNotificationManager.notify(
                    Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                    mNotificationBuilder.build());
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                    mNotificationBuilder.build());
            notifyQStile();

        } else if (intent.getAction().equals(Constants.ACTION.START_ACTION) && isServiceEnabled
                    && isServicePaused) {
            isServicePaused = false;
            mHandler.sendEmptyMessage(MSG_AUTH);
            mNotificationBuilder.setContentText("service is running")
                    .mActions.set(0,pauseAction);
            mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                    mNotificationBuilder.build());
            notifyQStile();

        } else if (intent.getAction().equals(Constants.ACTION.PAUSE_ACTION) && isServiceEnabled
                    && !isServicePaused) {
            isServicePaused = true;
            mHandler.sendEmptyMessage(MSG_CANCEL);
            mNotificationBuilder.setContentText("service is paused")
                    .mActions.set(0,playAction);
            mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                    mNotificationBuilder.build());
            notifyQStile();

        } else if (intent.getAction().equals(
                Constants.ACTION.STOPFOREGROUND_ACTION) && isServiceEnabled) {
//            Log.i(Constants.PREFS.LOG_TAG, "Received Stop Fingertouch Intent");

            isServiceEnabled = false;
            isServicePaused = false;
            mHandler.sendEmptyMessage(MSG_CANCEL);
            notifyQStile();

            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        Log.i(Constants.PREFS.LOG_TAG, "In onDestroy");
        unregisterBroadcastReceiver();
        isServiceEnabled = false;
        isServicePaused = false;
        notifyQStile();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }
}
