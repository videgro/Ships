package net.videgro.ships.fragments.internal;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import net.videgro.ships.Analytics;
import net.videgro.ships.R;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.activities.OpenDeviceActivity;

public final class FragmentUtils {
	private static final String TAG="FragmentUtils";
	private static final int NMEA_UDP_PORT=10110;
	
	/**
	 * Shared across fragments
	 */
	public static boolean rtlSdrRunning=false;
	
	private FragmentUtils(){
		// Utility class, no public constructor
	}

	public static void startReceivingAisFromAntenna(final Fragment fragment,final int reqCode,final int ppm) {
		final String tag="startReceivingAisFromAntenna - ";
		Log.d(TAG,tag);
		final String aisMessagesDestinationHost = SettingsUtils.parseFromPreferencesAisMessagesDestinationHost(fragment.getActivity());
		final String arguments = "-p " + ppm + " -P "+NMEA_UDP_PORT+" -h " + aisMessagesDestinationHost + " -R -x -S 60 -n";
		final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(fragment.getString(R.string.intent_filter_schema) + "://" + arguments));
		fragment.startActivityForResult(intent, reqCode);
	}
	
	public static void changeRtlSdrPpm(final Fragment fragment,final int reqCode,final int ppm) {
		final String tag="changeRtlSdrPpm - ";
		Log.d(TAG,tag);
		final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(fragment.getString(R.string.intent_filter_schema) + "://"));
		
		// Request to change PPM instead of (re)starting RTL-SDR
		intent.putExtra(OpenDeviceActivity.EXTRA_CHANGE_PPM,ppm);

		fragment.startActivityForResult(intent, reqCode);	
	}
	
	public static void stopReceivingAisFromAntenna(final Fragment fragment,final int reqCode){
		final String tag="stopReceivingAisFromAntenna - ";		
		Log.d(TAG,tag);
		final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(fragment.getString(R.string.intent_filter_schema) + "://"));
		intent.putExtra(OpenDeviceActivity.EXTRA_DISCONNECT, Boolean.TRUE);
		fragment.startActivityForResult(intent, reqCode);
	}
	
	public static OpenDeviceResult parseOpenCloseDeviceActivityResult(final Intent data){
		OpenDeviceResult result=null;
		if (data!=null){
			result=new OpenDeviceResult(data.getStringExtra(OpenDeviceActivity.EXTRA_RESULT_MESSAGE),data.getStringExtra(OpenDeviceActivity.EXTRA_RESULT_DEVICE_DESCRIPTION),data.getIntExtra(OpenDeviceActivity.EXTRA_RESULT_ERROR_REASON,-1));
		}
		
		return result;
	}
	
	public static void stopApplication(final Fragment fragment){
		final String tag="stopApplication - ";
		final int waitTime=5000;
		Analytics.logEvent(fragment.getActivity(), TAG, "stopApplication","");
		fragment.getActivity().finish();
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			Log.e(TAG,tag,e);			
		}
		System.exit(0);
	}
}
