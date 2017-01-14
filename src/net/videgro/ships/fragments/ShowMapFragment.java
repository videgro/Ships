package net.videgro.ships.fragments;

import com.google.gson.Gson;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;
import net.videgro.ships.Analytics;
import net.videgro.ships.R;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.Utils;
import net.videgro.ships.dialogs.ImagePopup.ImagePopupListener;
import net.videgro.ships.fragments.internal.FragmentUtils;
import net.videgro.ships.fragments.internal.OpenDeviceResult;
import net.videgro.ships.listeners.NmeaReceivedListener;
import net.videgro.ships.listeners.OwnLocationReceivedListener;
import net.videgro.ships.nmea2ship.Nmea2Ship;
import net.videgro.ships.nmea2ship.domain.Ship;
import net.videgro.ships.services.NmeaUdpClientService;
import net.videgro.ships.services.TrackService;
import net.videgro.ships.tools.HttpCachingTileServer;

public class ShowMapFragment extends Fragment implements OwnLocationReceivedListener, NmeaReceivedListener, ImagePopupListener {
	private static final String TAG = "ShowMapFragment";

	private static final int IMAGE_POPUP_ID_CALIBRATE_WARNING=1101;
	private static final int IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR=1102;
	
	private static final int REQ_CODE_START_RTLSDR = 1201;
	private static final int REQ_CODE_STOP_RTLSDR = 1202;
	
	private WebView webView;
	private TextView logTextView;
	private Nmea2Ship nmea2Ship = new Nmea2Ship();
	private TrackService trackService;
	private NmeaUdpClientService nmeaUdpClientService;
	private ServiceConnection locationServiceConnection;
	private ServiceConnection nmeaUdpClientServiceConnection;
	private Location lastReceivedOwnLocation=null;
	private ToggleButton startStopButton;
	
	public static final ShowMapFragment newInstance() {
		return new ShowMapFragment();
	}

	private ShowMapFragment() {
		// No public constructor, use newInstance()
	}	
	
	@Override
	public void onDestroy() {
		destroyNmeaUdpClientService();
		destroyLocationService();    
		super.onDestroy();
	}
	
	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final View rootView = inflater.inflate(R.layout.fragment_map, container, false);

		logTextView = (TextView) rootView.findViewById(R.id.textView1);

		Utils.loadAd(rootView);
		
		setupHttpCachingTileServer();
		setHasOptionsMenu(true);
		setupWebView(rootView);
		
		setupLocationService();
		setupNmeaUdpClientService();
		
		startStopButton = (ToggleButton) rootView.findViewById(R.id.startStopAisButton);
		startStopButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					startReceivingAisFromAntenna();
				} else {
					stopReceivingAisFromAntenna();
				}
			}
		});
			
		// TODO: Not possible to stop. Will start automatically when valid PPM exists and hide (stop) button for now
		startStopButton.setVisibility(View.GONE);
		
		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		
		final int ppm = SettingsUtils.parseFromPreferencesRtlSdrPpm(this.getActivity());
		if (!SettingsUtils.isValidPpm(ppm)) {
			Utils.showPopup(IMAGE_POPUP_ID_CALIBRATE_WARNING,this.getActivity(),this,getString(R.string.popup_no_ppm_set_title),getString(R.string.popup_no_ppm_set_message),R.drawable.warning_icon);			
		} else {
			webView.loadUrl("javascript:setZoomToExtent("+Boolean.toString(SettingsUtils.parseFromPreferencesMapZoomToExtend(getActivity()))+")");
			startReceivingAisFromAntenna();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG,"onResume");
		Analytics.logScreenView(getActivity(), TAG);
	}
		
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.i(TAG, "onActivityResult requestCode: " + requestCode + ", resultCode: " + resultCode);

		switch (requestCode) {
			case REQ_CODE_START_RTLSDR:
				final OpenDeviceResult startRtlSdrResult = FragmentUtils.parseOpenCloseDeviceActivityResult(data);
				Analytics.logEvent(getActivity(), TAG,"OpenDeviceResult",startRtlSdrResult.toString());
				logStatus(startRtlSdrResult.toString());
				
				if (resultCode != Activity.RESULT_OK) {
					resetGuiToInitialState();
					Utils.showPopup(IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR,getActivity(),this,getString(R.string.popup_start_device_failed_title),getString(R.string.popup_start_device_failed_message)+" "+startRtlSdrResult.toString(),R.drawable.thumbs_down_circle);
				} else {
					FragmentUtils.rtlSdrRunning=true;
				}
				break;
			case REQ_CODE_STOP_RTLSDR:
				logStatus(FragmentUtils.parseOpenCloseDeviceActivityResult(data).toString());
				FragmentUtils.rtlSdrRunning=false;
				break;
	
			default:
				Log.e(TAG, "Unexpected request code: " + requestCode);
		}
	}	
	
	private void setupHttpCachingTileServer(){
		final String tag = "setupHttpCachingTileServer - ";
		final HttpCachingTileServer httpCachingTileServer = HttpCachingTileServer.getInstance();		
		final int cleanup = httpCachingTileServer.cleanup();
		Log.i(TAG, tag + "Deleted: " + cleanup + " files from caching tile server.");
		httpCachingTileServer.startServer();
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	private void setupWebView(final View rootView){
		webView = (WebView) rootView.findViewById(R.id.webview);

		// Enable JavaScript on webview
		final WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setAllowFileAccessFromFileURLs(true);

		webView.setWebViewClient(new WebViewClient() {
			public void onPageFinished(WebView view, String url) {
				webView.loadUrl("javascript:setZoomToExtent("+Boolean.toString(SettingsUtils.parseFromPreferencesMapZoomToExtend(getActivity()))+")");
				if (lastReceivedOwnLocation!=null){
					webView.loadUrl("javascript:setCurrentPosition(" + lastReceivedOwnLocation.getLongitude() + "," + lastReceivedOwnLocation.getLatitude() + ")");					
				}
			}
		});
		webView.loadUrl("file:///android_asset/index.html");
		// Flow will resume at: onPageFinished
	}
		
	private void setupLocationService(){
		locationServiceConnection = new LocationServiceConnection((OwnLocationReceivedListener) this);
		Intent serviceIntent = new Intent(getActivity(), TrackService.class);
		getActivity().startService(serviceIntent);
		getActivity().bindService(new Intent(getActivity(), TrackService.class), locationServiceConnection, Context.BIND_AUTO_CREATE);
	}
	
	private void setupNmeaUdpClientService(){
		nmeaUdpClientServiceConnection = new NmeaUdpClientServiceConnection((NmeaReceivedListener) this);
		Intent serviceIntent = new Intent(getActivity(), NmeaUdpClientService.class);
		getActivity().startService(serviceIntent);
		getActivity().bindService(new Intent(getActivity(), NmeaUdpClientService.class), nmeaUdpClientServiceConnection, Context.BIND_AUTO_CREATE);
	}
	
	private void destroyLocationService(){
		if (trackService!=null){
			trackService.setListener(null);
	    }
	    
	    if (locationServiceConnection!=null){
	    	getActivity().unbindService(locationServiceConnection);
	    	locationServiceConnection=null;
	    }
	}
	
	private void destroyNmeaUdpClientService(){
		if (nmeaUdpClientService!=null){
			nmeaUdpClientService.removeListener(this);
	    }
	    
	    if (nmeaUdpClientServiceConnection!=null){
	    	getActivity().unbindService(nmeaUdpClientServiceConnection);
	    	nmeaUdpClientServiceConnection=null;
	    }
	}

	private void startReceivingAisFromAntenna(){
		final String tag="startReceivingAisFromAntenna - ";
		if (!FragmentUtils.rtlSdrRunning){
			final int ppm = SettingsUtils.parseFromPreferencesRtlSdrPpm(getActivity());		
			if (SettingsUtils.isValidPpm(ppm)) {
				logStatus("Start receiving AIS (PPM: "+ppm+")");
				FragmentUtils.startReceivingAisFromAntenna(this,REQ_CODE_START_RTLSDR,ppm);
				// Will continue at onActivityResult (REQ_CODE_START_RTLSDR)
			} else {
				Log.e(TAG,tag+"Invalid PPM: "+ppm);
			}					
		}
	}
	
	private void stopReceivingAisFromAntenna(){
		FragmentUtils.stopReceivingAisFromAntenna(this,REQ_CODE_STOP_RTLSDR);
	}
	
	private void resetGuiToInitialState(){
		startStopButton.setChecked(false);
	}
	
	private void logStatus(final String status){
		Utils.logStatus(getActivity(),logTextView,status);
	}
	
	/************************** LISTENER IMPLEMENTATIONS ******************/

	/**** START ImagePopupListener ****/
	
	@Override
	public void onImagePopupDispose(int id) {
		switch (id) {
		case IMAGE_POPUP_ID_CALIBRATE_WARNING:
			getFragmentManager().beginTransaction().replace(R.id.container, CalibrateFragment.newInstance()).commit();
			// FIXME: Observed exception: "NullPointerException (@ShowMapFragment:onImagePopupDispose:257) {main}"
		break;
		case IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR:
			// TODO: Currently all errors are fatal, because we can't stop and restart the RTL-SDR dongle correctly
			FragmentUtils.stopApplication(this);
		break;		
		default:
			Log.d(TAG,"onImagePopupDispose - id: "+id);
		}
	}
	
	/**** END ImagePopupListener ****/
	
	/**** START NmeaReceivedListener ****/
	
	@Override
	public void onNmeaReceived(String nmea) {
		final String tag="onNmeaReceived - ";
		
		final Ship ship = nmea2Ship.onMessage(nmea);

		if (ship != null && ship.isValid()) {
			if (ship.isValid()){
				final String json = new Gson().toJson(ship);
				
				final String shipIdent="MMSI: "+ ship.getMmsi() + (ship.getName() != null  && !ship.getName().isEmpty() ? " "+ship.getName() : "");
				logStatus("Ship location received ("+shipIdent+")");
	
				if (getActivity()!=null){
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							webView.loadUrl("javascript:onShipReceived('" + json + "')");
						}
					});
				} else {
					Log.e(TAG,tag+"Huh?");
				}
			} else {
				Log.w(TAG,tag+"Ship is invalid: "+ship.toString());
			}
		}
	}
	
	/**** END NmeaListener ****/
	
	/**** START OwnLocationReceivedListener ****/
	
	@Override
	public void onOwnLocationReceived(final Location location) {
		logStatus("Own location received: Lon: " + location.getLongitude() + ", Lat: " + location.getLatitude());
		lastReceivedOwnLocation=location;
				
		if (getActivity()!=null){
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					webView.loadUrl("javascript:setCurrentPosition(" + location.getLongitude() + "," + location.getLatitude() + ")");
				}
			});
		} else {
			Log.e(TAG,"Huh?");
		}
	}
	
	/**** END OwnLocationReceivedListener ****/
		
	
	/************************** PRIVATE CLASS IMPLEMENTATIONS ******************/
	
	private class LocationServiceConnection implements ServiceConnection {
		private final OwnLocationReceivedListener listener;

		public LocationServiceConnection(OwnLocationReceivedListener listener) {
			this.listener = listener;
		}

		public void onServiceConnected(ComponentName className, IBinder service) {
			if (service instanceof TrackService.ServiceBinder) {
				trackService = ((TrackService.ServiceBinder) service).getService();
				trackService.setListener(listener);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			trackService = null;
		}
	}
	
	private class NmeaUdpClientServiceConnection implements ServiceConnection {
		private final String tag="NmeaUdpClientServiceConnection - ";
		private final NmeaReceivedListener listener;

		public NmeaUdpClientServiceConnection(NmeaReceivedListener listener) {
			this.listener = listener;
		}

		public void onServiceConnected(ComponentName className, IBinder service) {
			if (service instanceof NmeaUdpClientService.ServiceBinder) {
				Log.d(TAG,tag+"onServiceConnected");
				nmeaUdpClientService = ((NmeaUdpClientService.ServiceBinder) service).getService();
				nmeaUdpClientService.addListener(listener);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			nmeaUdpClientService = null;
		}
	}
}
