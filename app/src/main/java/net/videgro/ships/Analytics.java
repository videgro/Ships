package net.videgro.ships;

import android.content.Context;

import net.videgro.analytics.MyFirebaseAnalytics;

public final class Analytics {
	/* Package protected */
	static final String CATEGORY_WARNINGS="Warnings";
	public static final String CATEGORY_ERRORS="Errors";
	public static final String CATEGORY_STATISTICS="Statistics";
	public static final String CATEGORY_ANDROID_DEVICE="Device (Android)";
	public static final String CATEGORY_RTLSDR_DEVICE="Device (RTL-SDR)";
	public static final String CATEGORY_NMEA_REPEAT="NMEA Repeat";

	private Analytics(){
		// Utility class, no public constructor
	}

	public static synchronized void logEvent(final Context context,final String category,final String action,final String label) {
		MyFirebaseAnalytics.logEvent(context, category, action, label);
	}

	public static synchronized void logEvent(final Context context,final String category,final String action,final String label,final long value){
		MyFirebaseAnalytics.logEvent(context, category, action, label, value);
	}
}
