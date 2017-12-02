package net.videgro.ships.fragments.internal;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import net.videgro.ships.Analytics;
import net.videgro.ships.R;
import net.videgro.ships.activities.OpenDeviceActivity;
import net.videgro.ships.services.NmeaClientService;

public final class FragmentUtils {
	private static final String TAG="FragmentUtils";

	public static final String BUNDLE_DATA_FRAGMENT_PREVIOUS="previous_fragment";

	/**
	 * Shared across fragments
	 */
	public static boolean rtlSdrRunning=false;
	
	private FragmentUtils(){
		// Utility class, no public constructor
	}
	
	public static boolean startReceivingAisFromAntenna(final Fragment fragment,final int reqCode,final int ppm) {
		final String tag="startReceivingAisFromAntenna - ";
		Log.d(TAG,tag);
		boolean result=false;
		final String arguments = "-p " + ppm + " -P "+ NmeaClientService.NMEA_UDP_PORT+" -h " + NmeaClientService.NMEA_UDP_HOST + " -R -x -S 60 -n";
		final Intent intent=createOpenDeviceIntent(fragment,arguments);
		if (intent!=null){
			fragment.startActivityForResult(intent, reqCode);
			result=true;
		}
		return result;
	}
	
	public static boolean changeRtlSdrPpm(final Fragment fragment,final int reqCode,final int ppm) {
		final String tag="changeRtlSdrPpm - ";
		Log.d(TAG,tag);
		boolean result=false;
		final Intent intent=createOpenDeviceIntent(fragment,null);		
		if (intent!=null){
			// Request to change PPM instead of (re)starting RTL-SDR
			intent.putExtra(OpenDeviceActivity.EXTRA_CHANGE_PPM,ppm);
	
			fragment.startActivityForResult(intent, reqCode);
			result=true;
		}
		return result;
	}

	public static boolean stopReceivingAisFromAntenna(final Fragment fragment,final int reqCode){
		final String tag="stopReceivingAisFromAntenna - ";		
		Log.d(TAG,tag);
		boolean result=false;
		final Intent intent=createOpenDeviceIntent(fragment,null);
		if (intent!=null){
			intent.putExtra(OpenDeviceActivity.EXTRA_DISCONNECT, Boolean.TRUE);
			fragment.startActivityForResult(intent, reqCode);
			result=true;
		}
		return result;
	}
	
	private static OpenDeviceResult parseOpenCloseDeviceActivityResult(final Intent data){
		OpenDeviceResult result=null;
		if (data!=null){
			result=new OpenDeviceResult(data.getStringExtra(OpenDeviceActivity.EXTRA_RESULT_MESSAGE),data.getStringExtra(OpenDeviceActivity.EXTRA_RESULT_DEVICE_DESCRIPTION),data.getIntExtra(OpenDeviceActivity.EXTRA_RESULT_ERROR_REASON,-1));
		} else {
            result=new OpenDeviceResult("Unknown reason","Unknown device",OpenDeviceActivity.ERROR_REASON_MISC);
        }
		
		return result;
	}

	public static String parseOpenCloseDeviceActivityResultAsString(final Intent data){
		final OpenDeviceResult startRtlSdrResult = parseOpenCloseDeviceActivityResult(data);
		return (startRtlSdrResult!=null) ? startRtlSdrResult.toString() : "RESULT UNKNOWN";
	}
	
	public static void stopApplication(final Fragment fragment) {
		final String tag = "stopApplication - ";
		final int waitTime = 5000;

		Analytics.getInstance().logEvent(TAG, "stopApplication", "");

		if (fragment != null && fragment.getActivity() != null) {
			fragment.getActivity().finish();

			try {
				Thread.sleep(waitTime);
			} catch (InterruptedException e) {
				Log.e(TAG,tag,e);
			}
		}

		System.exit(0);
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
	
	private static Intent createOpenDeviceIntent(final Fragment fragment,final String arguments){
		final String tag="createOpenDeviceIntent - ";		
		Intent result=null;
		if (fragment!=null && fragment.isAdded()){
			result = new Intent(Intent.ACTION_VIEW);
			final String filterSchema = fragment.getString(R.string.opendevice_intent_filter_schema);
			final Uri intentData = Uri.parse(filterSchema+ "://" + (arguments==null ? "" : arguments));
			result.setData(intentData);
		} else {
			Log.w(TAG,tag+"Fragment is null or not added to its activity.");
		}
		return result;
	}
}
