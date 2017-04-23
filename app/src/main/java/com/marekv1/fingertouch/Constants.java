package com.marekv1.fingertouch;

public class Constants {
	public interface ACTION {
		public static String MAIN_ACTION = "com.marekv1.fingertouch.action.main";
		public static String START_ACTION = "com.marekv1.fingertouch.action.start";
		public static String PAUSE_ACTION = "com.marekv1.fingertouch.action.pause";
		public static String STARTFOREGROUND_ACTION = "com.marekv1.fingertouch.action.startforeground";
		public static String STOPFOREGROUND_ACTION = "com.marekv1.fingertouch.action.stopforeground";
	}

	public interface NOTIFICATION_ID {
		public static int FOREGROUND_SERVICE = 101;
	}
}
