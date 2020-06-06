package net.videgro.ships.tasks;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import net.videgro.ships.listeners.CalibrateListener;
import net.videgro.ships.listeners.ShipReceivedListener;
import net.videgro.ships.nmea2ship.domain.Ship;
import net.videgro.ships.services.NmeaClientService;

public class CalibrateTask extends AsyncTask<Void, Void, String> implements ShipReceivedListener {
	private static final String TAG="CalibrateTask";
	
	public enum ScanType {
		THOROUGH, NORMAL
	}	
	
	private static final int PPM_UNDEFINED=Integer.MAX_VALUE;
	private static final int PPM_MIDDLE=0;
	private static final int PPM_MINIMAL=-100;
	private static final int PPM_MAXIMAL=100;
	
	private static final int PPM_STEP_SIZE=3;
	private static final int PPM_MONITOR_TIME_THOROUGH=40*1000; // ms
	private static final int PPM_MONITOR_TIME_NORMAL=20*1000; // ms
	
	private static final int NUMBER_OF_STEPS=Math.round((PPM_MAXIMAL-PPM_MINIMAL)/PPM_STEP_SIZE*1f);
	
	private ScanType scanType;
	private Context context;
	private CalibrateListener listener;

	private NmeaClientService nmeaClientService;
	private ServiceConnection nmeaUdpClientServiceConnection;

	/* Use this variable to solve the IllegalArgumentException: Service not registered
	 * More info: https://stackoverflow.com/questions/22079909/android-java-lang-illegalargumentexception-service-not-registered
	 */
    private boolean isBound = false;
	
	private int ppm=PPM_UNDEFINED;
	private boolean ppmIsValid=false;
	
	/**
	 * Will be true when all PPM values are tried or trying next PPM failed.
	 */
	private boolean failedToTryNextPpm;

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
		final String tag="waitForPendingRequestToOpenDevice - ";
		while (pendingRequestToOpenDevice){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				if (isCancelled()){
					Log.i(TAG, tag+"Task has been cancelled. Ignore exception.", e);
				} else {
					Log.e(TAG, tag, e);
				}
			}
		}
	}
	
	public String doInBackground(Void... params) {
		Thread.currentThread().setName(TAG);
		
		setupNmeaUdpClientService();
		
		step=0;
		
		while (!isCancelled() && !failedToTryNextPpm && !ppmIsValid){
			pendingRequestToOpenDevice=true;
			failedToTryNextPpm=!tryNextPpm();
			waitForPendingRequestToOpenDevice();
			try {
				Thread.sleep(scanType.equals(ScanType.THOROUGH) ? PPM_MONITOR_TIME_THOROUGH : PPM_MONITOR_TIME_NORMAL);
			} catch (InterruptedException e) {
				Log.e(TAG,"doInBackground - During monitoring.",e);
			}
			
			Log.e(TAG,"ppmAllValuesTried: "+failedToTryNextPpm);
		}
		
		Log.e(TAG,"EXIT FROM WHILE");
		
		destroyNmeaUdpClientService();

		if (failedToTryNextPpm){
			listener.onCalibrateFailed();
		}
		
		if (isCancelled()){
			listener.onCalibrateCancelled();
		}
		
		return "FINISHED";
	}
	
	private void setupNmeaUdpClientService(){
		nmeaUdpClientServiceConnection = new NmeaUdpClientServiceConnection(this);
		final Intent serviceIntent = new Intent(context, NmeaClientService.class);

		// On Android 8+ let service run in foreground
		if (Build.VERSION.SDK_INT >=  Build.VERSION_CODES.O) {
			context.startForegroundService(serviceIntent);
		} else {
			context.startService(serviceIntent);
		}

        doBindService();
	}
	
	private void destroyNmeaUdpClientService(){
		if (nmeaClientService !=null){
			nmeaClientService.removeListener(this);
	    }
	    
	    if (nmeaUdpClientServiceConnection!=null){
            doUnbindService();
	    	nmeaUdpClientServiceConnection=null;
	    }
	}

	private void doBindService(){
        context.bindService(new Intent(context, NmeaClientService.class), nmeaUdpClientServiceConnection, Context.BIND_AUTO_CREATE);
		isBound = true;
	}

	private void doUnbindService() {
        if (isBound) {
            context.unbindService(nmeaUdpClientServiceConnection);
            isBound = false;
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
			result=listener.onTryPpm(firstTry,Math.round(step/NUMBER_OF_STEPS*100),ppm);			
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
	public void onShipReceived(final Ship ship) {
		if (ship.getSource() != null && ship.getSource().equals(Ship.Source.INTERNAL)){
			Log.d(TAG, "onShipReceived: Ship: " + ship);

			// Test ppmIsValid ->Fire only once
			if (!ppmIsValid) {
				ppmIsValid = true;
				listener.onCalibrateReady(ppm);
			}
		}
	}
	
	private class NmeaUdpClientServiceConnection implements ServiceConnection {
		private final String tag="NmeaUdpClientServiceConnection - ";
		private final ShipReceivedListener listener;

		NmeaUdpClientServiceConnection(ShipReceivedListener listener) {
			this.listener = listener;
		}

		public void onServiceConnected(ComponentName className, IBinder service) {
			if (service instanceof NmeaClientService.ServiceBinder) {
				Log.d(TAG,tag+"onServiceConnected");
				nmeaClientService = ((NmeaClientService.ServiceBinder) service).getService();
				nmeaClientService.addListener(listener);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			nmeaClientService = null;
		}
	}
}
