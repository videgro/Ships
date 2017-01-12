package net.videgro.ships.tasks;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import net.videgro.ships.listeners.CalibrateListener;
import net.videgro.ships.listeners.NmeaReceivedListener;
import net.videgro.ships.services.NmeaUdpClientService;

public class CalibrateTask extends AsyncTask<Void, Void, String> implements NmeaReceivedListener {
	private static final String TAG="CalibrateTask";
	
	public enum ScanType {
		THOROUGH, NORMAL
	}	
	
	private static final int PPM_UNDEFINED=-1;
	private static final int PPM_MIDDLE=50;
	private static final int PPM_MINIMAL=0;
	private static final int PPM_MAXIMAL=100;
	
	private static final int PPM_STEP_SIZE=3;
	private static final int PPM_MONITOR_TIME_THOROUGH=40*1000; // ms
	private static final int PPM_MONITOR_TIME_NORMAL=20*1000; // ms
	
	private static final int NUMBER_OF_STEPS=Math.round((PPM_MAXIMAL-PPM_MINIMAL)/PPM_STEP_SIZE*1f);
	
	private ScanType scanType;
	private Context context;
	private CalibrateListener listener;

	private NmeaUdpClientService nmeaUdpClientService;
	private ServiceConnection nmeaUdpClientServiceConnection;
	
	private int ppm=PPM_UNDEFINED;
	private boolean ppmIsValid=false;
	private boolean ppmAllValuesTried;
	
	private int lowPpm=PPM_MIDDLE;
	private int highPpm=PPM_MIDDLE;
	
	private boolean pendingRequestToOpenDevice=false;
	
	private boolean lastStepWasIncrease=false;
	
	private float step=0;	
	
	public CalibrateTask(Context context,ScanType scanType,CalibrateListener listener){
		this.context = context;
		this.scanType = scanType;
		this.listener = listener;
	}
	
	private void waitForPendingRequestToOpenDevice(){
		while (pendingRequestToOpenDevice){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Log.e(TAG,"waitForPendingRequestToOpenDevice",e);
			}
		}
	}
	
	public String doInBackground(Void... params) {
		Thread.currentThread().setName(TAG);
		
		setupNmeaUdpClientService();
		
		step=0;
		
		while (!isCancelled() && !ppmAllValuesTried && !ppmIsValid){
			pendingRequestToOpenDevice=true;
			ppmAllValuesTried=!tryNextPpm();
			waitForPendingRequestToOpenDevice();
			try {
				Thread.sleep(scanType.equals(ScanType.THOROUGH) ? PPM_MONITOR_TIME_THOROUGH : PPM_MONITOR_TIME_NORMAL);
			} catch (InterruptedException e) {
				Log.e(TAG,"doInBackground - During monitoring.",e);
			}
			
			Log.e(TAG,"ppmAllValuesTried: "+ppmAllValuesTried);
		}
		
		Log.e(TAG,"EXIT FROM WHILE");
		
		destroyNmeaUdpClientService();

		if (ppmAllValuesTried){
			listener.onCalibrateFailed();
		}
		
		if (isCancelled()){
			listener.onCalibrateCancelled();
		}
		
		return "FINISHED";
	}
	
	private void setupNmeaUdpClientService(){
		nmeaUdpClientServiceConnection = new NmeaUdpClientServiceConnection((NmeaReceivedListener) this);
		Intent serviceIntent = new Intent(context, NmeaUdpClientService.class);
		context.startService(serviceIntent);
		context.bindService(new Intent(context, NmeaUdpClientService.class), nmeaUdpClientServiceConnection, Context.BIND_AUTO_CREATE);
	}
	
	private void destroyNmeaUdpClientService(){
		if (nmeaUdpClientService!=null){
			nmeaUdpClientService.removeListener(this);
	    }
	    
	    if (nmeaUdpClientServiceConnection!=null){
	    	context.unbindService(nmeaUdpClientServiceConnection);
	    	nmeaUdpClientServiceConnection=null;
	    }
	}
	
	private boolean tryNextPpm(){
		boolean result=false;
		boolean firstTry=true;
		
		if (ppm==PPM_UNDEFINED){
			ppm=lowPpm; // First try is 'decrease'
		} else {
			firstTry=false;
			if (!lastStepWasIncrease){
				lastStepWasIncrease=true;
				highPpm+=PPM_STEP_SIZE;
				ppm=(highPpm<=PPM_MAXIMAL) ? highPpm : PPM_UNDEFINED;
			} else {
				lastStepWasIncrease=false;
				lowPpm-=PPM_STEP_SIZE;
				ppm=(lowPpm>=PPM_MINIMAL) ? lowPpm : PPM_UNDEFINED;
			}
		}
		
		if (ppm!=PPM_UNDEFINED){
			step++;
			listener.onTryPpm(firstTry,Math.round(step/NUMBER_OF_STEPS*100),ppm);
			result=true;
		}
		
		return result;
	}
	
	public void onPostExecute(String result) {
		if (result != null) {
			Log.d(TAG, "Result: " + result);
		}
	}
	
	public void onDeviceOpened(){
		Log.d(TAG, "onDeviceOpened");
		pendingRequestToOpenDevice=false;
	}
	
	public void onDeviceClosed(){
		Log.d(TAG, "onDeviceClosed");
	}
	
	@Override
	public void onNmeaReceived(String nmea) {
		Log.d(TAG, "onNmeaReceived: NMEA: "+nmea);
		
		// Test ppmIsValid ->Fire only once
		if (!ppmIsValid){
			ppmIsValid=true;
			listener.onCalibrateReady(ppm);
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
