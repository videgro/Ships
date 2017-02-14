package net.videgro.ships.services;

import java.util.HashSet;
import java.util.Set;

import net.videgro.ships.StartRtlSdrRequest;
import net.videgro.ships.Utils;
import net.videgro.ships.bridge.NativeRtlSdr;
import net.videgro.ships.bridge.NativeRtlSdr.NativeRtlSdrListener;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class RtlSdrAisService extends RtlSdrService implements NativeRtlSdrListener {
    private static final String TAG = "RtlSdrAisService";

    private final Set<RtlSdrServiceListener> listeners = new HashSet<RtlSdrServiceListener>();
    private final NativeRtlSdr nativeRtlSdr=new NativeRtlSdr();
    
    private WakeLock wakelock;
    private final IBinder binder = new ServiceBinder();
    private StartRtlSdrRequest pendingStartRtlSdrRequest;
    
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public boolean registerListener(final RtlSdrServiceListener listener) {		
		synchronized(listeners){	
			return listeners.add(listener);
		}
	}

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	super.onStartCommand(intent, flags, startId);
    	nativeRtlSdr.addListener(this);
        return START_STICKY;
    }
	
    @Override
	public void onDestroy() {
    	final String tag="onDestroy - ";
    	Log.i(TAG,tag);
    	nativeRtlSdr.stopAis();
    	nativeRtlSdr.removeListener(this);
    	releaseWakeLock();
		super.onDestroy();
	}
    
    private void aquireWakeLock(){
    	wakelock=((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,TAG);
    	wakelock.setReferenceCounted(false);
    	if (!wakelock.isHeld()){
	    	wakelock.acquire();
	        Log.i(TAG,"Acquired wake lock. Will keep the screen on.");
    	}
    }
    
    private void releaseWakeLock(){    	
    	if (wakelock!=null && wakelock.isHeld()){
	    	wakelock.release();
	        Log.i(TAG,"Released wake lock.");
    	}
    }

    public void startRtlSdr(StartRtlSdrRequest startRtlSdrRequest) {
    	final String tag="startRtlSdr - ";
    	           
        Log.d(TAG,tag+"StartRtlSdrRequest: " + startRtlSdrRequest);
        
        if (nativeRtlSdr.isRunningAis()){
        	// Store request to use after RTL-SDR has been stopped
        	pendingStartRtlSdrRequest=startRtlSdrRequest;
        	nativeRtlSdr.stopAis();
        	// Will continue in: onStopped()
        } else {
        	nativeRtlSdr.startAis(startRtlSdrRequest);        	
        }
    }
    
    public boolean isRtlSdrRunning(){
    	return nativeRtlSdr.isRunningAis();
    }
    
    public boolean stopRtlSdr(){
    	return nativeRtlSdr.stopAis();
    }
    
    public boolean changePpm(final int newPpm){
    	return nativeRtlSdr.changePpm(newPpm);
    }

    public boolean unregisterListener(final RtlSdrServiceListener listener) {
    	synchronized(listeners){
    		return listeners.remove(listener);
    	}
    }
    
	public class ServiceBinder extends Binder {
		public RtlSdrAisService getService() {
			return RtlSdrAisService.this;
		}
	}
	
	@Override
	public void onMessage(String data) {
		// Nothing to do					
	}

	@Override
	public void onError(String data) {
		// Nothing to do					
	}

	@Override
	public void onException(int exitCode) {
		// Nothing to do					
	}

	@Override
	public void onPpm(int ppmCurrent, int ppmCumulative) {
		// Nothing to do					
	}

	@Override
	public void onRtlSdrStarted() {
		Log.d(TAG,"NativeRtlSdrListener - onRtlSdrStarted");
		
		// Inform listeners
		for (final RtlSdrServiceListener listener : listeners){
            listener.onRtlSdrStarted();
        }
				
		aquireWakeLock();
		
    	Utils.sendNotification(this,"is running","Touch to see the ships.");
	}

    @Override
    public void onRtlSdrStopped() {
    	Log.d(TAG,"NativeRtlSdrListener - onRtlSdrStopped");
    	
    	// Inform listeners
		for (final RtlSdrServiceListener listener : listeners){
            listener.onRtlSdrStopped();
        }

    	releaseWakeLock();
    	    	
    	if (pendingStartRtlSdrRequest!=null){
    		// Requested to stop AIS before starting again. Now it's time to start AIS

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Log.e(TAG,"start - wait for closed device",e);
			}
    		
        	nativeRtlSdr.startAis(pendingStartRtlSdrRequest);
        	pendingStartRtlSdrRequest=null;
    	}
    }
}
