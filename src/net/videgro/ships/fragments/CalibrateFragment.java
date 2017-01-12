package net.videgro.ships.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import net.videgro.ships.Analytics;
import net.videgro.ships.R;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.Utils;
import net.videgro.ships.dialogs.ImagePopup.ImagePopupListener;
import net.videgro.ships.fragments.internal.FragmentUtils;
import net.videgro.ships.fragments.internal.OpenDeviceResult;
import net.videgro.ships.listeners.CalibrateListener;
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
	
	public static final CalibrateFragment newInstance() {
		return new CalibrateFragment();
	}

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final View rootView = inflater.inflate(R.layout.fragment_calibrate, container, false);
		setHasOptionsMenu(true);

		logTextView = (TextView) rootView.findViewById(R.id.textView1);
		logTextView.setText("");

		calibrateProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar1);
		
		// Percentage
		calibrateProgressBar.setMax(100);

		startStopCalibrateButtonNormal = (ToggleButton) rootView.findViewById(R.id.startStopCalibrateNormalButton);
		startStopCalibrateButtonNormal.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					startNormalCalibrating();
				} else {
					stopNormalCalibrating();
				}
			}
		});
		
		startStopCalibrateButtonThorough = (ToggleButton) rootView.findViewById(R.id.startStopCalibrateThoroughButton);
		startStopCalibrateButtonThorough.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					startThoroughCalibrating();
				} else {
					stopThoroughCalibrating();
				}
			}
		});
		
		setHasOptionsMenu(true);
		Utils.loadAd(rootView);
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		final int ppm = SettingsUtils.parseFromPreferencesRtlSdrPpm(getActivity());
		if (SettingsUtils.isValidPpm(ppm)) {
			Log.d(TAG,"Valid PPM available, no need to calibrate. Switch to Show Map fragment.");
			switchToShowMapFragment();
		}
		Analytics.logScreenView(getActivity(), TAG);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult requestCode: " + requestCode + ", resultCode: " + resultCode);

		switch (requestCode) {
			case REQ_CODE_START_RTLSDR:
				final OpenDeviceResult startRtlSdrResult = FragmentUtils.parseOpenCloseDeviceActivityResult(data);
				logStatus(startRtlSdrResult.toString());
				
				if (resultCode == Activity.RESULT_OK) {
					FragmentUtils.rtlSdrRunning=true;
					if (calibrateTask!=null){						
						calibrateTask.onDeviceOpened();
					} else {
						Log.e(TAG,"Started RTL-SDR - Calibrate task not set.");
					}
				} else {	
					resetGuiToInitialState();
					Utils.showPopup(IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR,getActivity(),this,"Failed to start RTL-SDR","<p style='text-decoration: underline'>"+startRtlSdrResult.toString()+"</p>",R.drawable.thumbs_down_circle);
				}
				break;
			case REQ_CODE_STOP_RTLSDR:
				logStatus(FragmentUtils.parseOpenCloseDeviceActivityResult(data).toString());
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
		startCalibrateTask(CalibrateTask.ScanType.NORMAL);
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
	
	private void switchToShowMapFragment(){	
		getFragmentManager().beginTransaction().replace(R.id.container, ShowMapFragment.newInstance()).commit();
	}

	private void logStatus(final String status){
		Utils.logStatus(getActivity(),logTextView,status);
	}
	
	
	/************************** LISTENER IMPLEMENTATIONS ******************/
	
	/**** START CalibrateListener ****/
	
	@Override
	public void onTryPpm(boolean firstTry,int percentage,int ppm) {
		logStatus("Trying: "+ppm+", Progress: "+percentage+" %");
		calibrateProgressBar.setProgress(percentage);
		
		if (firstTry){
			FragmentUtils.startReceivingAisFromAntenna(this, REQ_CODE_START_RTLSDR,ppm);
		} else {
			FragmentUtils.changeRtlSdrPpm(this, REQ_CODE_START_RTLSDR,ppm);	
		}		
		
		// Will continue at onActivityResult (REQ_CODE_START_RTLSDR)
	}
	
	@Override
	public void onCalibrateReady(final int ppm){
		SettingsUtils.setToPreferencesPpm(getActivity(),ppm);
		Analytics.logEvent(getActivity(), TAG, "onCalibrateReady",""+ppm);
		Utils.showPopup(IMAGE_POPUP_ID_CALIBRATE_READY,getActivity(),this,getString(R.string.popup_found_ppm_title),getString(R.string.popup_found_ppm_message)+" "+ppm,R.drawable.thumbs_up_circle);		
	}	
	
	@Override
	public void onCalibrateFailed() {
		logStatus("Not possible to determine PPM.");
		Analytics.logEvent(getActivity(), TAG, "onCalibrateFailed","");
		Utils.showPopup(IMAGE_POPUP_ID_CALIBRATE_FAILED,getActivity(),this,getString(R.string.popup_not_found_ppm_title),getString(R.string.popup_not_found_ppm_message),R.drawable.thumbs_down_circle);
	}
	
	@Override
	public void onCalibrateCancelled() {
		logStatus("Calibration cancelled.");
		Analytics.logEvent(getActivity(), TAG, "onCalibrateCancelled","");
		Utils.showPopup(IMAGE_POPUP_ID_CALIBRATE_FAILED,getActivity(),this,getString(R.string.popup_calibration_cancelled_title),getString(R.string.popup_calibration_cancelled_message),R.drawable.warning_icon);		
	}	
	
	/**** END CalibrateListener ****/


	/**** START ImagePopupListener ****/
	
	@Override
	public void onImagePopupDispose(int id) {
		switch (id){
			case IMAGE_POPUP_ID_CALIBRATE_READY:
				switchToShowMapFragment();
			break;
			case IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR:
				// TODO: Currently all errors are fatal, because we can't stop and restart the RTL-SDR dongle correctly				
				FragmentUtils.stopApplication(this);
			break;
			case IMAGE_POPUP_ID_CALIBRATE_FAILED:
				// TODO: Not possible to stop RTL-SDR stick at the moment, so kill it the hard way by stopping the app
				//FragmentUtils.stopReceivingAisFromAntenna(this,REQ_CODE_STOP_RTLSDR_AIS);
				
				FragmentUtils.stopApplication(this);				
			default:
				Log.d(TAG,"onImagePopupDispose - id: "+id);
			break;		
		}
	}
	
	/**** END ImagePopupListener ****/
}
