package net.videgro.ships;

//import net.videgro.ships.domain.TrackingSettings;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public final class SettingsUtils {
	private static final String TAG = "SettingsUtils";

	// Declare the same values as in res/xml/preferences.xml!
	public static final String KEY_PREF_RTL_SDR_PPM = "pref_rtlSdrPpm";
//	private static final String KEY_PREF_RTL_SDR_FORCE_ROOT = "pref_rtlSdrForceRoot";

	private static final String KEY_PREF_LOGGING_VERBOSE = "pref_loggingVerbose";
	private static final String KEY_PREF_MAP_ZOOM_TO_EXTEND = "pref_mapZoomToExtend";
	private static final String KEY_PREF_MAP_CACHE_ZOOM_LOWER_LEVELS = "pref_mapFetchLowerZoomLevels";
	private static final String KEY_PREF_MAP_CACHE_DISK_USAGE_MAX = "pref_mapCacheMaxDiskUsage";
	private static final String KEY_PREF_AIS_MESSAGES_DESTINATION_HOST = "pref_aisMessagesDestinationHost";

	private static final boolean DEFAULT_LOGGING_VERBOSE = true;
	private static final boolean DEFAULT_MAP_ZOOM_TO_EXTEND = true;
	private static final int DEFAULT_MAP_CACHE_DISK_USAGE_MAX = 5;
	private static final boolean DEFAULT_MAP_CACHE_ZOOM_LOWER_LEVELS = true;
	private static final String DEFAULT_AIS_MESSAGES_DESTINATION_HOST = "127.0.0.1";
//	private static final boolean DEFAULT_RTL_SDR_FORCE_ROOT = false;
	private static final int DEFAULT_RTL_SDR_PPM = Integer.MAX_VALUE;

	private static final int RTL_SDR_PPM_VALID_OFFSET = 150;

	private SettingsUtils() {
		// Utility class, no public constructor
	}

	public static int parseFromPreferencesRtlSdrPpm(final Context context) {
		int result = DEFAULT_RTL_SDR_PPM;
		if (context != null) {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			if (sharedPref != null) {
				try {
					result = Integer.valueOf(sharedPref.getString(KEY_PREF_RTL_SDR_PPM, Integer.toString(DEFAULT_RTL_SDR_PPM)));
				} catch (ClassCastException e) {
					Log.e(TAG, "parseFromPreferencesPpm", e);
				} catch (NumberFormatException e) {
					Log.e(TAG, "parseFromPreferencesPpm", e);
				}
			}
		}
		return result;
	}

	public static boolean isValidPpm(final int ppm) {
		return (ppm > (SettingsUtils.RTL_SDR_PPM_VALID_OFFSET * -1) && ppm < SettingsUtils.RTL_SDR_PPM_VALID_OFFSET);
	}

	public static void setToPreferencesPpm(final Context context, final int ppm) {
		if (context != null) {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			if (sharedPref != null) {
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putString(KEY_PREF_RTL_SDR_PPM, Integer.toString(ppm));
				editor.commit();
			}
		}
	}

	public static boolean parseFromPreferencesLoggingVerbose(final Context context) {
		boolean result = DEFAULT_LOGGING_VERBOSE;
		if (context != null) {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			if (sharedPref != null) {
				result = sharedPref.getBoolean(KEY_PREF_LOGGING_VERBOSE, DEFAULT_LOGGING_VERBOSE);
			}
		}
		return result;
	}

	public static boolean parseFromPreferencesMapZoomToExtend(final Context context) {
		boolean result = DEFAULT_MAP_ZOOM_TO_EXTEND;
		if (context != null) {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			if (sharedPref != null) {
				result = sharedPref.getBoolean(KEY_PREF_MAP_ZOOM_TO_EXTEND, DEFAULT_MAP_ZOOM_TO_EXTEND);
			}
		}
		return result;
	}

	public static boolean parseFromPreferencesMapCacheLowerZoomlevels(final Context context) {
		boolean result = DEFAULT_MAP_CACHE_ZOOM_LOWER_LEVELS;
		if (context != null) {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			if (sharedPref != null) {
				result = sharedPref.getBoolean(KEY_PREF_MAP_CACHE_ZOOM_LOWER_LEVELS, DEFAULT_MAP_CACHE_ZOOM_LOWER_LEVELS);
			}
		}
		return result;
	}

	public static long parseFromPreferencesMapCacheDiskUsageMax(final Context context) {
		final String tag="parseFromPreferencesMapCacheDiskUsageMax";
		
		int result = DEFAULT_MAP_CACHE_DISK_USAGE_MAX;
		if (context != null) {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			if (sharedPref != null) {
				try {					
					result = Integer.valueOf(sharedPref.getString(KEY_PREF_MAP_CACHE_DISK_USAGE_MAX, Integer.toString(DEFAULT_MAP_CACHE_DISK_USAGE_MAX)));
				} catch (ClassCastException e) {
					Log.e(TAG,tag,e);
				} catch (NumberFormatException e) {
					Log.e(TAG,tag,e);
				}
			}
		}
		
		// Return in bytes
		return result * 1024 * 1024L;
	}

	public static String parseFromPreferencesAisMessagesDestinationHost(final Context context) {
		String result = DEFAULT_AIS_MESSAGES_DESTINATION_HOST;
		if (context != null) {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			if (sharedPref != null) {
				result = sharedPref.getString(KEY_PREF_AIS_MESSAGES_DESTINATION_HOST, DEFAULT_AIS_MESSAGES_DESTINATION_HOST);
			}
		}
		return result;
	}
}
