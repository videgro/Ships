package net.videgro.ships.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.fragment.app.Fragment;

import net.videgro.ships.Analytics;
import net.videgro.ships.R;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.Utils;
import net.videgro.ships.fragments.internal.FragmentUtils;
import net.videgro.ships.fragments.internal.OpenDeviceResult;
import net.videgro.ships.listeners.CalibrateListener;
import net.videgro.ships.listeners.ImagePopupListener;
import net.videgro.ships.tasks.CalibrateTask;

public class CalibrateFragment extends Fragment implements CalibrateListener, ImagePopupListener {
	private static final String TAG = "CalibrateFragment";
	
	private static final int IMAGE_POPUP_ID_CALIBRATE_READY=2101;
	private static final int IMAGE_POPUP_ID_CALIBRATE_FAILED=2102;
	private static final int IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR=2103;
	
	private static final int REQ_CODE_START_RTLSDR = 2201;
	private static final int REQ_CODE_STOP_RTLSDR = 2202;
	
	private ProgressBar calibrateProgressBar;
	private TextView logTextView;
	private ToggleButton startStopCalibrateButtonNormal;
	private ToggleButton startStopCalibrateButtonThorough;

	private CalibrateTask calibrateTask;

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final View rootView = inflater.inflate(R.layout.fragment_calibrate, container, false);
		setHasOptionsMenu(true);

		logTextView = rootView.findViewById(R.id.textView1);
		logTextView.setText("");

		calibrateProgressBar = rootView.findViewById(R.id.progressBar1);
		
		// Percentage
		calibrateProgressBar.setMax(100);

		startStopCalibrateButtonNormal = rootView.findViewById(R.id.startStopCalibrateNormalButton);
		startStopCalibrateButtonNormal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startNormalCalibrating();
            } else {
                stopNormalCalibrating();
            }
        });
		
		startStopCalibrateButtonThorough = rootView.findViewById(R.id.startStopCalibrateThoroughButton);
		startStopCalibrateButtonThorough.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startThoroughCalibrating();
            } else {
                stopThoroughCalibrating();
            }
        });

		final RelativeLayout adView = rootView.findViewById(R.id.adView);
		if (isAdded()) {
			Utils.loadAd(getActivity(),adView,getString(R.string.adUnitId_CalibrateFragment));
		}

		setHasOptionsMenu(true);

		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		final int ppm = SettingsUtils.getInstance().parseFromPreferencesRtlSdrPpm();
		if (SettingsUtils.isValidPpm(ppm) && isAdded()) {
			Log.d(TAG, "Valid PPM available, no need to calibrate. Return.");
			FragmentUtils.returnFromFragment(this);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult requestCode: " + requestCode + ", resultCode: " + resultCode);

		switch (requestCode) {
			case REQ_CODE_START_RTLSDR:
				final String startRtlSdrResultAsString=FragmentUtils.parseOpenCloseDeviceActivityResultAsString(data);
                Analytics.logEvent(getActivity(),Analytics.CATEGORY_RTLSDR_DEVICE, OpenDeviceResult.TAG, startRtlSdrResultAsString+" - "+Utils.retrieveAbi());
				logStatus(startRtlSdrResultAsString);
				
				if (resultCode == Activity.RESULT_OK) {
					FragmentUtils.rtlSdrRunning=true;
					if (calibrateTask!=null){						
						calibrateTask.onDeviceOpened();
					} else {
						Log.e(TAG,"Started RTL-SDR - Calibrate task not set.");
					}
				} else {	
					resetGuiToInitialState();
					Utils.showPopup(IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR,getActivity(),this,getString(R.string.popup_start_device_failed_title),getString(R.string.popup_start_device_failed_message)+" "+startRtlSdrResultAsString,R.drawable.thumbs_down_circle,null);
				}
				break;
			case REQ_CODE_STOP_RTLSDR:
				logStatus(FragmentUtils.parseOpenCloseDeviceActivityResultAsString(data));
				FragmentUtils.rtlSdrRunning=false;
				if (resultCode == Activity.RESULT_OK) {										
					if (calibrateTask!=null){						
						calibrateTask.onDeviceClosed();
					} else {
						Log.e(TAG,"Stopped RTL-SDR - Calibrate task not set.");
					}
				}
				break;
			default:
				Log.e(TAG, "Unexpected request code: " + requestCode);
		}
	}
	
	private void startNormalCalibrating(){		
		startStopCalibrateButtonThorough.setEnabled(false);		
		startCalibrateTask(CalibrateTask.ScanType.NORMAL);
	}
	
	private void stopNormalCalibrating(){
		stopCalibrateTask();
		startStopCalibrateButtonThorough.setEnabled(true);
	}
	
	private void startThoroughCalibrating(){		
		startStopCalibrateButtonNormal.setEnabled(false);
		startCalibrateTask(CalibrateTask.ScanType.THOROUGH);
	}
	
	private void stopThoroughCalibrating(){
		stopCalibrateTask();
		startStopCalibrateButtonNormal.setEnabled(true);
	}
		
	private void startCalibrateTask(final CalibrateTask.ScanType scanType){
		calibrateTask = new CalibrateTask(getActivity(),scanType,this);
		calibrateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}	

	private void stopCalibrateTask(){
		if (calibrateTask!=null && !calibrateTask.isCancelled()){
			calibrateTask.cancel(true);
			calibrateTask=null;
		}
	}
	
	private void resetGuiToInitialState(){
		startStopCalibrateButtonNormal.setEnabled(true);
		startStopCalibrateButtonThorough.setEnabled(true);

		startStopCalibrateButtonNormal.setChecked(false);
		startStopCalibrateButtonThorough.setChecked(false);
	}

	private void logStatus(final String status){
		Utils.logStatus(getActivity(),logTextView,status);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Not possible to share anything from this fragment
		menu.setGroupVisible(R.id.main_menu_group_show_map, false);
	}
	
	/************************** LISTENER IMPLEMENTATIONS ******************/
	
	/**** START CalibrateListener ****/
	
	@Override
	public boolean onTryPpm(boolean firstTry,int percentage,int ppm) {
		final String tag="onTryPpm - ";
		boolean result=false;
		if (isAdded()){
			logStatus("Trying: "+ppm+", Progress: "+percentage+" %");
			calibrateProgressBar.setProgress(percentage);

			if (firstTry){
				result=FragmentUtils.startReceivingAisFromAntenna(this, REQ_CODE_START_RTLSDR,ppm);
			} else {
				result=FragmentUtils.changeRtlSdrPpm(this, REQ_CODE_START_RTLSDR,ppm);
			}

			// Will continue at onActivityResult (REQ_CODE_START_RTLSDR)
		} else {
			Log.w(TAG,tag+"Fragment is not added to activity.");
		}
		
		return result;
	}
	
	@Override
	public void onCalibrateReady(final int ppm){
        Analytics.logEvent(getActivity(),TAG, "onCalibrateReady",""+ppm);

        if (isAdded()){
			SettingsUtils.getInstance().setToPreferencesPpm(ppm);
			Utils.showPopup(IMAGE_POPUP_ID_CALIBRATE_READY, getActivity(), this, getString(R.string.popup_found_ppm_title), getString(R.string.popup_found_ppm_message) + " " + ppm, R.drawable.thumbs_up_circle, null);
		}
	}
	
	@Override
	public void onCalibrateFailed() {
		logStatus("Not possible to determine PPM.");
        Analytics.logEvent(getActivity(),TAG, "onCalibrateFailed", "");
		if (isAdded()) {
            Utils.showPopup(IMAGE_POPUP_ID_CALIBRATE_FAILED, getActivity(), this, getString(R.string.popup_not_found_ppm_title), getString(R.string.popup_not_found_ppm_message), R.drawable.thumbs_down_circle, null);
		}
	}
	
	@Override
	public void onCalibrateCancelled() {
		logStatus("Calibration cancelled.");
        Analytics.logEvent(getActivity(),TAG, "onCalibrateCancelled", "");
		if (isAdded()) {
			Utils.showPopup(IMAGE_POPUP_ID_CALIBRATE_FAILED, getActivity(), this, getString(R.string.popup_calibration_cancelled_title), getString(R.string.popup_calibration_cancelled_message), R.drawable.warning_icon, null);
		}
	}	
	
	/* END CalibrateListener */


	/* START ImagePopupListener */
	
	@Override
	public void onImagePopupDispose(int id) {
		switch (id){
			case IMAGE_POPUP_ID_CALIBRATE_READY:
                // Once more 'changeRtlSdrPpm' with stored value, to trigger MainActivity.onResume()
                final int ppm = SettingsUtils.getInstance().parseFromPreferencesRtlSdrPpm();
                FragmentUtils.changeRtlSdrPpm(this, REQ_CODE_START_RTLSDR,ppm);

                break;
            case IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR:
            case IMAGE_POPUP_ID_CALIBRATE_FAILED:
                // Set to communicate to dispatcher that calibration has failed
                SettingsUtils.getInstance().setToPreferencesInternalIsCalibrationFailed(true);

                // Once more 'changeRtlSdrPpm' with stored value, to trigger MainActivity.onResume()
                FragmentUtils.changeRtlSdrPpm(this, REQ_CODE_START_RTLSDR,0);
			break;

			default:
				Log.d(TAG,"onImagePopupDispose - id: "+id);
			break;		
		}
	}
	
	/* END ImagePopupListener */
}
