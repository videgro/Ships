package net.videgro.ships.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;

import net.videgro.ships.Analytics;
import net.videgro.ships.R;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.Utils;
import net.videgro.ships.fragments.internal.FragmentUtils;
import net.videgro.ships.listeners.ImagePopupListener;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener, ImagePopupListener {
	private static final String TAG = "SettingsFragment";

	private static final int REQ_CODE_CHANGE_RTLSDR_PPM = 3201;
	private static final int IMAGE_POPUP_ID_MUST_STOP_OTHER_DEVICE=3103;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		final Preference button = (Preference) findPreference(getString(R.string.pref_rtlSdrPpmInvalidate));
		button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				useOtherRtlSdrDeviceAreYouSure();
				return true;
			}
		});
	}
	
	@Override
	public void onResume() {
		super.onResume();
		final PreferenceCategory defaultCat = (PreferenceCategory) findPreference(getString(R.string.pref_GeneralSettings));
		defaultCat.setTitle("Version: " + getAppVersion());
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
	    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	    super.onPause();
	}
	
	private void useOtherRtlSdrDeviceAreYouSure(){		
		new AlertDialog.Builder(getActivity()).setMessage(getString(R.string.popup_are_you_sure_title))
			.setPositiveButton(getString(R.string.popup_are_you_sure_yes), dialogUseOtherRtlSdrDeviceAreYouSureClickListener)
			.setNegativeButton(getString(R.string.popup_are_you_sure_no), dialogUseOtherRtlSdrDeviceAreYouSureClickListener)
			.show();		
	}

	private void useOtherRtlSdrDevice(){
		Analytics.logEvent(getActivity(),TAG, "useOtherRtlSdrDevice","");
		
		// Invalidate PPM setting
		SettingsUtils.getInstance().setToPreferencesPpm(Integer.MAX_VALUE);
		
		// Show popup and on closing popup, close application
		Utils.showPopup(IMAGE_POPUP_ID_MUST_STOP_OTHER_DEVICE,getActivity(),this,getString(R.string.popup_other_rtlsdr_device_title),getString(R.string.popup_other_rtlsdr_device_message),R.drawable.warning_icon,null);
	}

	private String getAppVersion() {
		String result = "UNDEFINED";
		if (getView()!=null && getView().getContext()!=null) {
			try {
				result = getView().getContext().getPackageManager().getPackageInfo(getView().getContext().getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
				Log.e(TAG, "getAppVersion", e);
			}
		}
		return result;
	}
	
	/************************** LISTENER IMPLEMENTATIONS ******************/

	DialogInterface.OnClickListener dialogUseOtherRtlSdrDeviceAreYouSureClickListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which){
	        case DialogInterface.BUTTON_POSITIVE:
	            // Yes button clicked
	        	useOtherRtlSdrDevice();
	            break;

	        case DialogInterface.BUTTON_NEGATIVE:
	            // No button clicked
	        	// Ignore
	            break;
	        }
	    }
	};
	
	/**** START ImagePopupListener ****/
	
	@Override
	public void onImagePopupDispose(int id) {
		switch (id) {
		case IMAGE_POPUP_ID_MUST_STOP_OTHER_DEVICE:
			// TODO: Currently not possible to stop RTL-SDR correctly, stop application for now.
			if (isAdded()) {
				final Activity activity=getActivity();
				Analytics.logEvent(activity,TAG, "stopApplication", "IMAGE_POPUP_ID_MUST_STOP_OTHER_DEVICE");
				activity.finishAffinity();
			}
			System.exit(0);
		break;
		default:
			Log.d(TAG,"onImagePopupDispose - id: "+id);
		}
	}
	
	/**** END ImagePopupListener ****/
	

	/**** START OnSharedPreferenceChangeListener ****/
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
		if (key.equals(SettingsUtils.KEY_PREF_RTL_SDR_PPM)){
			// PPM changed, must effectuate this in RTL-SDR device
			final int newPpm = SettingsUtils.getInstance().parseFromPreferencesRtlSdrPpm();
			if (SettingsUtils.isValidPpm(newPpm) && FragmentUtils.rtlSdrRunning){
				Analytics.logEvent(getActivity(),TAG, "onSharedPreferenceChanged - PPM",""+newPpm);
				FragmentUtils.changeRtlSdrPpm(this, REQ_CODE_CHANGE_RTLSDR_PPM,newPpm);
				// Will continue at onActivityResult (REQ_CODE_CHANGE_RTLSDR_PPM) (NOT IMPLEMENTED IN THIS CLASS)
			}
		}
	}
	
	/**** END OnSharedPreferenceChangeListener ****/
}