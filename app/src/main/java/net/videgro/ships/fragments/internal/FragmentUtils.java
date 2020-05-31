package net.videgro.ships.fragments.internal;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import net.videgro.ships.R;
import net.videgro.ships.activities.OpenDeviceActivity;
import net.videgro.ships.services.NmeaClientService;

public final class FragmentUtils {
	private static final String TAG="FragmentUtils";

	/**
	 * Shared across fragments
	 */
	public static boolean rtlSdrRunning=false;

	private FragmentUtils(){
		// Utility class, no public constructor
	}

	private static String createArgumentsToReceiveAis(final int ppm){
		return  "-p " + ppm + " -P " + NmeaClientService.NMEA_UDP_PORT + " -h " + NmeaClientService.NMEA_UDP_HOST + " -R -x -S 60 -n";
	}

	public static boolean startReceivingAisFromAntenna(final Fragment fragment,final int reqCode,final int ppm) {
		final String tag="startReceivingAisFromAntenna (Fragment) - ";
		Log.d(TAG,tag);
		boolean result=false;
		if (fragment!=null && fragment.isAdded()) {
			final Intent intent = createOpenDeviceIntent(fragment.getActivity(), createArgumentsToReceiveAis(ppm));
			fragment.startActivityForResult(intent, reqCode);
			result = true;
		} else {
			Log.w(TAG,tag+"Fragment is null or not added to its activity.");
		}
		return result;
	}

	public static boolean startReceivingAisFromAntenna(final Activity activity,final int reqCode,final int ppm) {
		final String tag="startReceivingAisFromAntenna (Activity) - ";
		Log.d(TAG,tag);
		final Intent intent = createOpenDeviceIntent(activity, createArgumentsToReceiveAis(ppm));
		activity.startActivityForResult(intent, reqCode);
		return true;
	}

	public static boolean changeRtlSdrPpm(final Fragment fragment,final int reqCode,final int ppm) {
		final String tag="changeRtlSdrPpm - ";
		Log.d(TAG,tag);
		boolean result=false;
		if (fragment!=null && fragment.isAdded()) {
			final Intent intent=createOpenDeviceIntent(fragment.getActivity(),null);

			// Request to change PPM instead of (re)starting RTL-SDR
			intent.putExtra(OpenDeviceActivity.EXTRA_CHANGE_PPM,ppm);

			fragment.startActivityForResult(intent, reqCode);
			result=true;
		} else {
			Log.w(TAG,tag+"Fragment is null or not added to its activity.");
		}
		return result;
	}

	public static boolean stopReceivingAisFromAntenna(final Fragment fragment,final int reqCode){
		final String tag="stopReceivingAisFromAntenna - ";
		Log.d(TAG,tag);
		boolean result=false;
		if (fragment!=null && fragment.isAdded()) {
			final Intent intent=createOpenDeviceIntent(fragment.getActivity(),null);

			intent.putExtra(OpenDeviceActivity.EXTRA_DISCONNECT, Boolean.TRUE);
			fragment.startActivityForResult(intent, reqCode);
			result=true;
		} else {
			Log.w(TAG,tag+"Fragment is null or not added to its activity.");
		}
		return result;
	}

	private static OpenDeviceResult parseOpenCloseDeviceActivityResult(final Intent data){
		OpenDeviceResult result;
		if (data!=null){
			result=new OpenDeviceResult(data.getStringExtra(OpenDeviceActivity.EXTRA_RESULT_MESSAGE),data.getStringExtra(OpenDeviceActivity.EXTRA_RESULT_DEVICE_DESCRIPTION),data.getIntExtra(OpenDeviceActivity.EXTRA_RESULT_ERROR_REASON,-1));
		} else {
            result=new OpenDeviceResult("Unknown reason","Unknown device",OpenDeviceActivity.ERROR_REASON_MISC);
        }

		return result;
	}

	public static String parseOpenCloseDeviceActivityResultAsString(final Intent data){
		return parseOpenCloseDeviceActivityResult(data).toString();
	}

	public static void returnFromFragment(final Fragment fragment){
		final Activity activity=fragment.getActivity();
		if (activity!=null){
			final FragmentManager fragmentManager=activity.getFragmentManager();
			if (fragmentManager!=null) {
				fragmentManager.popBackStack();
			}
		}
	}

	private static Intent createOpenDeviceIntent(final Context context,final String arguments){
		final Intent result = new Intent(Intent.ACTION_VIEW);

		final String filterSchema = context.getString(R.string.opendevice_intent_filter_schema);
		final Uri intentData = Uri.parse(filterSchema+ "://" + (arguments==null ? "" : arguments));
		result.setData(intentData);

		return result;
	}
}
