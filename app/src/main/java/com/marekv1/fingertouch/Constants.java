package com.marekv1.fingertouch;

public class Constants {
	public interface ACTION {
		String MAIN_ACTION = "com.marekv1.fingertouch.action.main";
		String START_ACTION = "com.marekv1.fingertouch.action.start";
		String PAUSE_ACTION = "com.marekv1.fingertouch.action.pause";
		String STARTFOREGROUND_ACTION = "com.marekv1.fingertouch.action.startforeground";
		String STOPFOREGROUND_ACTION = "com.marekv1.fingertouch.action.stopforeground";
		String UPDATE_STATUS = "com.marekv1.fingertouch.action.updatestatus";
        String UPDATE_SETTINGS = "com.marekv1.fingertouch.action.updatesettings";
	}

	public interface PREFS {
        String LOG_TAG = "FingertouchService";
        String PREFS_NAME = "fingertouchSettings";
    }

	public interface NOTIFICATION_ID {
		int FOREGROUND_SERVICE = 101;
	}
}
