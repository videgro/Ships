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
	public static final String CATEGORY_AR="AR";
	public static final String CATEGORY_AR_ERRORS="AR Errors";

	private Analytics(){
		// Utility class, no public constructor
	}

	private static String cleanup(final String raw){
		// letters, digits or _ (underscores)
		// Maximum length 40
		final int maxLength=40;
		String result=raw.replaceAll("[^a-zA-Z0-9]", "_");
		if (result.length()>maxLength){
			result=result.substring(0,maxLength-1);
		}
		return result;
	}

	public static synchronized void logEvent(final Context context,final String category,final String action,final String label) {
		MyFirebaseAnalytics.logEvent(context, category, cleanup(action), cleanup(label));
	}

	public static synchronized void logEvent(final Context context,final String category,final String action,final String label,final long value){
		MyFirebaseAnalytics.logEvent(context, category, cleanup(action), cleanup(label), value);
	}
}
