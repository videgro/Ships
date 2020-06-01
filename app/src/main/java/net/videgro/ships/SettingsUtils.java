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

	private static final String KEY_PREF_OWN_IP = "pref_ownIp";

    private static final String KEY_PREF_INTERNAL_CALIBRATION_FAILED = "pref_internalIsCalibrationFailed";
	private static final String KEY_PREF_INTERNAL_USE_ONLY_EXTERNAL_SOURCES = "pref_useOnlyExternalSources";

    private static final String KEY_PREF_SHIP_SCALE_FACTOR = "pref_shipScaleFactor";
    private static final String KEY_PREF_MAX_AGE = "pref_maxAge";
    private static final String KEY_PREF_OWN_LOCATION_ICON = "pref_ownLocationIcon";

	private static final String KEY_PREF_LOGGING_VERBOSE = "pref_loggingVerbose";
	private static final String KEY_PREF_MAP_ZOOM_TO_EXTENT = "pref_mapZoomToExtent";
    private static final String KEY_PREF_MAP_DISABLE_SOUND = "pref_mapDisableSound";
	private static final String KEY_PREF_MAP_CACHE_ZOOM_LOWER_LEVELS = "pref_mapFetchLowerZoomLevels";
    private static final String KEY_PREF_MAP_CACHE_DISK_USAGE_MAX = "pref_mapCacheMaxDiskUsage";

    /* AR */
	private static final String KEY_PREF_AR_DISTANCE_MAX = "pref_arMaxDistance";
	private static final String KEY_PREF_AR_AGE_MAX = "pref_arMaxAge";

	private static final String KEY_PREF_AIS_MESSAGES_CLIENT_PORT = "pref_aisMessagesClientPort";

	private static final String KEY_PREF_REPEAT_FROM_INTERNAL = "pref_repeatFromInternal";
    private static final String KEY_PREF_REPEAT_FROM_EXTERNAL = "pref_repeatFromExternal";
	private static final String KEY_PREF_REPEAT_FROM_CLOUD = "pref_repeatFromCloud";

    private static final String KEY_PREF_REPEAT_TO_CLOUD = "pref_repeatToCloud";
	private static final String KEY_PREF_AIS_MESSAGES_DESTINATION_HOST_1 = "pref_aisMessagesDestinationHost1";
	private static final String KEY_PREF_AIS_MESSAGES_DESTINATION_PORT_1 = "pref_aisMessagesDestinationPort1";
	private static final String KEY_PREF_AIS_MESSAGES_DESTINATION_HOST_2 = "pref_aisMessagesDestinationHost2";
	private static final String KEY_PREF_AIS_MESSAGES_DESTINATION_PORT_2 = "pref_aisMessagesDestinationPort2";

	private static final boolean DEFAULT_LOGGING_VERBOSE = false;
    private static final boolean DEFAULT_INTERNAL_CALIBRATION_FAILED = false;
	private static final boolean DEFAULT_INTERNAL_USE_ONLY_EXTERNAL_SOURCES = false;

    private static final boolean DEFAULT_MAP_ZOOM_TO_EXTENT = true;
    private static final boolean DEFAULT_MAP_DISABLE_SOUND = false;
	private static final int DEFAULT_MAP_CACHE_DISK_USAGE_MAX = 5;
	private static final boolean DEFAULT_MAP_CACHE_ZOOM_LOWER_LEVELS = true;
	private static final int DEFAULT_AIS_MESSAGES_CLIENT_PORT = 10111;
	private static final String DEFAULT_AIS_MESSAGES_DESTINATION_HOST_1 = "127.0.0.1";
	private static final int DEFAULT_AIS_MESSAGES_DESTINATION_PORT_1 = 10110;
	private static final String DEFAULT_AIS_MESSAGES_DESTINATION_HOST_2 = "5.9.207.224";
	private static final int DEFAULT_AIS_MESSAGES_DESTINATION_PORT_2 = 8098;

	private static final int DEFAULT_RTL_SDR_PPM = Integer.MAX_VALUE;

    private static final int RTL_SDR_PPM_VALID_OFFSET = 1000;
    private static final int DEFAULT_SHIP_SCALE_FACTOR = 5;
    private static final int DEFAULT_MAX_AGE = 20;

	private static final boolean DEFAULT_REPEAT_FROM_INTERNAL = false;
    private static final boolean DEFAULT_REPEAT_FROM_EXTERNAL = false;
	private static final boolean DEFAULT_REPEAT_FROM_CLOUD = false;
	private static final boolean DEFAULT_REPEAT_TO_CLOUD = false;

	/* Same as  res/values/strings.xml pref_ownLocationIcon_default */
    private static final String DEFAULT_OWN_LOCATION_ICON = "antenna.png";

    /* AR (defaults) */
	private static final int DEFAULT_AR_DISTANCE_MAX = 2000;
	private static final int DEFAULT_AR_AGE_MAX = 10;

    private static SettingsUtils instance=null;

    private SharedPreferences sharedPreferences=null;

    private SettingsUtils() {
		// Singleton, no public constructor
	}

	public static SettingsUtils getInstance(){
        if (instance==null){
            instance=new SettingsUtils();
        }
        return instance;
    }

	public void init(final Context context){
		if (sharedPreferences==null) {
			if (context==null){
				throw new IllegalArgumentException("No context set.");
			}
			sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		}
	}

	private void validateSharedPreferences(){
		if (sharedPreferences==null){
			throw new IllegalArgumentException("No SharedPreferences set. Please init class before using it.");
		}
	}

	public int parseFromPreferencesRtlSdrPpm() {
		final String tag="parseFromPreferencesRtlSdrPpm - ";
		validateSharedPreferences();

		int result = DEFAULT_RTL_SDR_PPM;

		try {
			result = Integer.parseInt(sharedPreferences.getString(KEY_PREF_RTL_SDR_PPM, Integer.toString(DEFAULT_RTL_SDR_PPM)));
		} catch (ClassCastException | NumberFormatException e) {
			Log.e(TAG,tag, e);
		}

		return result;
	}

	public static boolean isValidPpm(final int ppm) {
		return (ppm > (SettingsUtils.RTL_SDR_PPM_VALID_OFFSET * -1) && ppm < SettingsUtils.RTL_SDR_PPM_VALID_OFFSET);
	}

	public void setToPreferencesPpm(final int ppm) {
		validateSharedPreferences();

		final SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(KEY_PREF_RTL_SDR_PPM, Integer.toString(ppm));
		editor.commit();
	}

    public void setToPreferencesOwnIp(final String ip) {
        validateSharedPreferences();

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_PREF_OWN_IP,ip);
        editor.commit();
    }

	public boolean parseFromPreferencesInternalIsCalibrationFailed() {
		validateSharedPreferences();
		return sharedPreferences.getBoolean(KEY_PREF_INTERNAL_CALIBRATION_FAILED, DEFAULT_INTERNAL_CALIBRATION_FAILED);
	}

    public void setToPreferencesInternalIsCalibrationFailed(final boolean failed) {
        validateSharedPreferences();

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_PREF_INTERNAL_CALIBRATION_FAILED,failed);
        editor.commit();
    }

	public boolean parseFromPreferencesInternalUseOnlyExternalSources() {
		validateSharedPreferences();
		return sharedPreferences.getBoolean(KEY_PREF_INTERNAL_USE_ONLY_EXTERNAL_SOURCES, DEFAULT_INTERNAL_USE_ONLY_EXTERNAL_SOURCES);
	}

	public void setToPreferencesInternalUseOnlyExternalSources(final boolean useOnlyExternalSources) {
		validateSharedPreferences();

		final SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(KEY_PREF_INTERNAL_USE_ONLY_EXTERNAL_SOURCES,useOnlyExternalSources);
		editor.commit();
	}

	public boolean parseFromPreferencesLoggingVerbose() {
		validateSharedPreferences();
		return sharedPreferences.getBoolean(KEY_PREF_LOGGING_VERBOSE, DEFAULT_LOGGING_VERBOSE);
	}

	public boolean parseFromPreferencesMapZoomToExtent() {
		validateSharedPreferences();
        return sharedPreferences.getBoolean(KEY_PREF_MAP_ZOOM_TO_EXTENT, DEFAULT_MAP_ZOOM_TO_EXTENT);
	}

    public boolean parseFromPreferencesMapDisableSound() {
        validateSharedPreferences();
        return sharedPreferences.getBoolean(KEY_PREF_MAP_DISABLE_SOUND, DEFAULT_MAP_DISABLE_SOUND);
    }

	public void setToPreferencesRepeatFromInternal(final boolean repeat) {
		validateSharedPreferences();

		final SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(KEY_PREF_REPEAT_FROM_INTERNAL,repeat);
		editor.commit();
	}

    public boolean parseFromPreferencesRepeatFromInternal() {
        validateSharedPreferences();
        return sharedPreferences.getBoolean(KEY_PREF_REPEAT_FROM_INTERNAL, DEFAULT_REPEAT_FROM_INTERNAL);
    }

	public boolean parseFromPreferencesRepeatFromExternal() {
		validateSharedPreferences();
		return sharedPreferences.getBoolean(KEY_PREF_REPEAT_FROM_EXTERNAL, DEFAULT_REPEAT_FROM_EXTERNAL);
	}

	public boolean parseFromPreferencesRepeatFromCloud() {
		validateSharedPreferences();
		return sharedPreferences.getBoolean(KEY_PREF_REPEAT_FROM_CLOUD, DEFAULT_REPEAT_FROM_CLOUD);
	}

	public boolean parseFromPreferencesRepeatToCloud() {
		validateSharedPreferences();
		return sharedPreferences.getBoolean(KEY_PREF_REPEAT_TO_CLOUD, DEFAULT_REPEAT_TO_CLOUD);
	}

	public void setToPreferencesRepeatToCloud(final boolean repeat) {
		validateSharedPreferences();

		final SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(KEY_PREF_REPEAT_TO_CLOUD,repeat);
		editor.commit();
	}

	public boolean parseFromPreferencesMapCacheLowerZoomlevels() {
		validateSharedPreferences();
		return sharedPreferences.getBoolean(KEY_PREF_MAP_CACHE_ZOOM_LOWER_LEVELS, DEFAULT_MAP_CACHE_ZOOM_LOWER_LEVELS);
	}

	public long parseFromPreferencesMapCacheDiskUsageMax() {
		final String tag="parseFromPreferencesMapCacheDiskUsageMax";
		validateSharedPreferences();

		int result = DEFAULT_MAP_CACHE_DISK_USAGE_MAX;

        try {
            result = Integer.parseInt(sharedPreferences.getString(KEY_PREF_MAP_CACHE_DISK_USAGE_MAX, Integer.toString(DEFAULT_MAP_CACHE_DISK_USAGE_MAX)));
        } catch (ClassCastException | NumberFormatException e) {
            Log.e(TAG,tag,e);
        }

		// Return in bytes
		return result * 1024 * 1024L;
	}

	public int parseFromPreferencesAisMessagesClientPort() {
		final String tag="parseFromPreferencesAisMessagesClientPort - ";
		validateSharedPreferences();

		int result = DEFAULT_AIS_MESSAGES_CLIENT_PORT;
		try {
			result = Integer.parseInt(sharedPreferences.getString(KEY_PREF_AIS_MESSAGES_CLIENT_PORT, Integer.toString(DEFAULT_AIS_MESSAGES_CLIENT_PORT)));
		} catch (ClassCastException | NumberFormatException e) {
			Log.e(TAG,tag,e);
		}
		return result;
	}

	public String parseFromPreferencesAisMessagesDestinationHost1() {
		validateSharedPreferences();
		return sharedPreferences.getString(KEY_PREF_AIS_MESSAGES_DESTINATION_HOST_1, DEFAULT_AIS_MESSAGES_DESTINATION_HOST_1);
	}

	public int parseFromPreferencesAisMessagesDestinationPort1() {
		final String tag="parseFromPreferencesAisMessagesDestinationPort1 - ";
		validateSharedPreferences();

		int result = DEFAULT_AIS_MESSAGES_DESTINATION_PORT_1;
        try {
            result = Integer.parseInt(sharedPreferences.getString(KEY_PREF_AIS_MESSAGES_DESTINATION_PORT_1, Integer.toString(DEFAULT_AIS_MESSAGES_DESTINATION_PORT_1)));
        } catch (ClassCastException | NumberFormatException e) {
            Log.e(TAG,tag,e);
		}
		return result;
	}

	public String parseFromPreferencesAisMessagesDestinationHost2() {
		validateSharedPreferences();
		return sharedPreferences.getString(KEY_PREF_AIS_MESSAGES_DESTINATION_HOST_2, DEFAULT_AIS_MESSAGES_DESTINATION_HOST_2);
	}

	public int parseFromPreferencesAisMessagesDestinationPort2() {
		final String tag="parseFromPreferencesAisMessagesDestinationPort2 - ";
		validateSharedPreferences();

		int result = DEFAULT_AIS_MESSAGES_DESTINATION_PORT_2;
		try {
			result = Integer.parseInt(sharedPreferences.getString(KEY_PREF_AIS_MESSAGES_DESTINATION_PORT_2, Integer.toString(DEFAULT_AIS_MESSAGES_DESTINATION_PORT_2)));
		} catch (ClassCastException | NumberFormatException e) {
			Log.e(TAG,tag,e);
		}
		return result;
	}

	public int parseFromPreferencesShipScaleFactor() {
		final String tag="parseFromPreferencesShipScaleFactor - ";
		validateSharedPreferences();

		int result = DEFAULT_SHIP_SCALE_FACTOR;

        try {
            result = Integer.parseInt(sharedPreferences.getString(KEY_PREF_SHIP_SCALE_FACTOR, Integer.toString(DEFAULT_SHIP_SCALE_FACTOR)));
        } catch (ClassCastException | NumberFormatException e) {
            Log.e(TAG,tag, e);
        }

		return result;
	}

	public int parseFromPreferencesMaxAge() {
		final String tag="parseFromPreferencesMaxAge - ";
		validateSharedPreferences();

		int result = DEFAULT_MAX_AGE;

		try {
			result = Integer.parseInt(sharedPreferences.getString(KEY_PREF_MAX_AGE, Integer.toString(DEFAULT_MAX_AGE)));
		} catch (ClassCastException | NumberFormatException e) {
			Log.e(TAG,tag, e);
		}

		return result;
	}

    public String parseFromPreferencesOwnLocationIcon() {
		validateSharedPreferences();
		return sharedPreferences.getString(KEY_PREF_OWN_LOCATION_ICON, DEFAULT_OWN_LOCATION_ICON);
    }

	public int parseFromPreferencesArMaxAge() {
		final String tag="parseFromPreferencesArMaxAge - ";
		validateSharedPreferences();

		int result = DEFAULT_AR_AGE_MAX;

		try {
			result = Integer.parseInt(sharedPreferences.getString(KEY_PREF_AR_AGE_MAX, Integer.toString(DEFAULT_AR_AGE_MAX)));
		} catch (ClassCastException | NumberFormatException e) {
			Log.e(TAG,tag, e);
		}

		return result;
	}

	public int parseFromPreferencesArMaxDistance() {
		final String tag="parseFromPreferencesArMaxDistance - ";
		validateSharedPreferences();

		int result = DEFAULT_AR_DISTANCE_MAX;

		try {
			result = Integer.parseInt(sharedPreferences.getString(KEY_PREF_AR_DISTANCE_MAX, Integer.toString(DEFAULT_AR_DISTANCE_MAX)));
		} catch (ClassCastException | NumberFormatException e) {
			Log.e(TAG,tag, e);
		}

		return result;
	}
}
