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

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;
import com.samsung.android.sdk.pass.SpassInvalidStateException;

public class FingertouchService extends Service implements Handler.Callback {
    private static final String LOG_TAG = "FingertouchService";
    public static final String PREFS_NAME = "fingertouchSettings";
    private SharedPreferences settings;

    private PowerManager pm;
    private SpassFingerprint mSpassFingerprint;
    private Spass mSpass;
    private Context mContext;
    private String eventStatusName;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private Intent playIntent;
    private PendingIntent pplayIntent;
    private NotificationCompat.Action playAction;
    private Intent pauseIntent;
    private PendingIntent ppauseIntent;
    private NotificationCompat.Action pauseAction;
    private Intent disableIntent;
    private PendingIntent pdisableIntent;

    private boolean onReadyIdentify = false;
    private boolean isServiceEnabled = false;
    private boolean isServicePaused = false;

    private boolean isFeatureEnabled_fingerprint = false;
    private boolean isFeatureEnabled_index = false;
    private boolean isFeatureEnabled_uniqueId = false;
    private boolean isFeatureEnabled_custom = false;
    private boolean isFeatureEnabled_backupPw = false;

    private Handler mHandler;
    private static final int MSG_AUTH = 1000;
    private static final int MSG_CANCEL = 1003;



    private BroadcastReceiver mPassReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                mHandler.sendEmptyMessageDelayed(MSG_AUTH, 100);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mHandler.sendEmptyMessageDelayed(MSG_CANCEL, 100);
            }
        }
    };

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mPassReceiver, filter);
    };

    private void unregisterBroadcastReceiver() {
        try {
            if (mContext != null) {
                mContext.unregisterReceiver(mPassReceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SpassFingerprint.IdentifyListener mIdentifyListener = new SpassFingerprint.IdentifyListener() {
        @Override
        public void onFinished(int eventStatus) {
            eventStatusName = getEventStatusName(eventStatus);
//            Log.i(LOG_TAG, "identify finished : reason =" + eventStatusName);

            if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS) {
//                Log.i(LOG_TAG, "onFinished() : Identify authentification Success");
            } else if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS) {
//                Log.i(LOG_TAG, "onFinished() : Password authentification Success");
            } else if (eventStatus == SpassFingerprint.STATUS_OPERATION_DENIED) {
//                Log.i(LOG_TAG, "onFinished() : Authentification is blocked because of fingerprint service internally.");
            } else if (eventStatus == SpassFingerprint.STATUS_USER_CANCELLED) {
//                Log.i(LOG_TAG, "onFinished() : User cancel this identify.");
            } else if (eventStatus == SpassFingerprint.STATUS_TIMEOUT_FAILED) {
//                Log.i(LOG_TAG, "onFinished() : The time for identify is finished.");
            } else if (eventStatus == SpassFingerprint.STATUS_QUALITY_FAILED) {
//                Log.i(LOG_TAG, "onFinished() : Authentification Fail for identify.");
//                FingerprintGuideText = mSpassFingerprint.getGuideForPoorQuality();
//                Toast.makeText(mContext, FingerprintGuideText, Toast.LENGTH_SHORT).show();
            } else {
//                Log.i(LOG_TAG, "onFinished() : Authentification Fail for identify, reason = " + eventStatusName);
            }

            onReadyIdentify = false;
            mHandler.sendEmptyMessageDelayed(MSG_AUTH, 100);
        }

        @Override
        public void onReady() {
//            Log.i(LOG_TAG, "identify state is ready");
        }

        @Override
        public void onStarted() {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.HOME");
            intent.setFlags(268435456);
            mContext.startActivity(intent);

            try {
                mSpassFingerprint.cancelIdentify();
            } catch (IllegalStateException ise) {
//                Log.i(LOG_TAG, ise.getMessage());
            }
//            Log.i(LOG_TAG, "User touched fingerprint sensor");
        }

        @Override
        public void onCompleted() {
//            Log.i(LOG_TAG, "the identify is completed");
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
            } catch (SpassInvalidStateException ise) {
                onReadyIdentify = false;
                if (ise.getType() == SpassInvalidStateException.STATUS_OPERATION_DENIED) {
//                    Log.i(LOG_TAG, "Exception: " + ise.getMessage());
                }
                mHandler.sendEmptyMessageDelayed(MSG_AUTH, 1000);
            } catch (IllegalStateException e) {
                onReadyIdentify = false;
//                Log.i(LOG_TAG, "Exception: " + e);
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
//                Log.i(LOG_TAG, "cancelIdentify is called");
            } catch (IllegalStateException ise) {
//                Log.i(LOG_TAG, ise.getMessage());
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
        settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("serviceStatus",getServiceStatus());
        editor.commit();
        TileService.requestListeningState(this,
                new ComponentName(this,FingertouchTileService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        settings=this.getSharedPreferences(PREFS_NAME, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION) && !isServiceEnabled) {
//            Log.i(LOG_TAG, "Received Start Fingertouch Intent ");

            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mContext = this;
            mHandler = new Handler(this);
            mSpass = new Spass();

            try {
                mSpass.initialize(FingertouchService.this);
            } catch (SsdkUnsupportedException e) {
                Log.i(LOG_TAG, "Exception: " + e);
            } catch (UnsupportedOperationException e) {
                Log.i(LOG_TAG, "Fingerprint Service is not supported in the device");
            }
            isFeatureEnabled_fingerprint = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);

            if (isFeatureEnabled_fingerprint) {
                mSpassFingerprint = new SpassFingerprint(FingertouchService.this);
                Log.i(LOG_TAG, "Fingerprint Service is supported in the device.");
                Log.i(LOG_TAG, "SDK version : " + mSpass.getVersionName());
            } else {
                Log.i(LOG_TAG, "Fingerprint Service is not supported in the device.");
                Toast.makeText(mContext, "Fingerprint Service is not supported in the device.",
                        Toast.LENGTH_SHORT).show();
                stopSelf();
                return START_NOT_STICKY;
            }

            isFeatureEnabled_index = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_FINGER_INDEX);
            isFeatureEnabled_custom = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_CUSTOMIZED_DIALOG);
            isFeatureEnabled_uniqueId = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_UNIQUE_ID);
            isFeatureEnabled_backupPw = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_AVAILABLE_PASSWORD);

            registerBroadcastReceiver();

            isServiceEnabled = true;
            isServicePaused = false;
            mHandler.sendEmptyMessage(MSG_AUTH);



            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            playIntent = new Intent(this, FingertouchService.class);
            playIntent.setAction(Constants.ACTION.START_ACTION);
            pplayIntent = PendingIntent.getService(this, 0,
                    playIntent, 0);
            playAction = new NotificationCompat.Action(
                    android.R.drawable.ic_media_play, "Resume", pplayIntent);

            pauseIntent = new Intent(this, FingertouchService.class);
            pauseIntent.setAction(Constants.ACTION.PAUSE_ACTION);
            ppauseIntent = PendingIntent.getService(this, 0,
                    pauseIntent, 0);
            pauseAction = new NotificationCompat.Action(
                    android.R.drawable.ic_media_pause, "Pause", ppauseIntent);

            disableIntent = new Intent(this, FingertouchService.class);
            disableIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
            pdisableIntent = PendingIntent.getService(this, 0,
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
//            Log.i(LOG_TAG, "Received Stop Fingertouch Intent");

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
//        Log.i(LOG_TAG, "In onDestroy");
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
