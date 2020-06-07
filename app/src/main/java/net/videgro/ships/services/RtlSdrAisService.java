package net.videgro.ships.services;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import net.videgro.ships.Analytics;
import net.videgro.ships.Notifications;
import net.videgro.ships.R;
import net.videgro.ships.StartRtlSdrRequest;
import net.videgro.ships.activities.MainActivity;
import net.videgro.ships.bridge.NativeRtlSdr;
import net.videgro.ships.bridge.NativeRtlSdr.NativeRtlSdrListener;

import java.util.HashSet;
import java.util.Set;

public class RtlSdrAisService extends RtlSdrService implements NativeRtlSdrListener {
    private static final String TAG = "RtlSdrAisService";

    private final Set<RtlSdrServiceListener> listeners = new HashSet<>();
    private final NativeRtlSdr nativeRtlSdr=new NativeRtlSdr();
    
    private WakeLock wakelock;
    private final IBinder binder = new ServiceBinder();

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
		final String tag="onStartCommand - ";
		final int result=super.onStartCommand(intent, flags, startId);

		Log.d(TAG,tag);

		// On Android 8+ let RtlSdrAisService run in foreground
		// More information: https://developer.android.com/about/versions/oreo/background-location-limits.html
		//
		// After startForegroundService, we must call startForeground in service.
		//
		// See: 1) https://stackoverflow.com/questions/44425584/context-startforegroundservice-did-not-then-call-service-startforeground
		//      2) https://stackoverflow.com/questions/46375444/remoteserviceexception-context-startforegroundservice-did-not-then-call-servic
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			final Notification notification= Notifications.getInstance().createNotification(this,getString(R.string.notification_channel_services_id),getString(R.string.notification_service_rtlsdrais_title),getString(R.string.notification_service_rtlsdrais_description));
			// Send always the same ID (TAG.hashCode()) so user won't be spammed by different multiple instances of the same notification.
			startForeground(TAG.hashCode(), notification);
		}

    	nativeRtlSdr.addListener(this);
        return result;
    }
	
    @Override
	public void onDestroy() {
    	final String tag="onDestroy - ";
    	Log.i(TAG,tag);

    	if (MainActivity.isNativeLibraryLoaded()) {
			try {
				nativeRtlSdr.stopAis();
			} catch (UnsatisfiedLinkError e) {
				/*
				 * Catch UnsatisfiedLinkError to prevent:
				 *
				 * Fatal Exception: java.lang.UnsatisfiedLinkError
				 * No implementation found for boolean net.videgro.ships.bridge.NativeRtlSdr.isRunningRtlSdrAis() (tried Java_net_videgro_ships_bridge_NativeRtlSdr_isRunningRtlSdrAis and Java_net_videgro_ships_bridge_NativeRtlSdr_isRunningRtlSdrAis__)
				 */
				Analytics.logEvent(this, Analytics.CATEGORY_ERRORS, TAG, tag + "Stop AIS native call: " + e.getMessage());
			}
		} else {
			Analytics.logEvent(this, Analytics.CATEGORY_ERRORS, TAG, tag + "Stop AIS native call, but native lib not loaded.");
		}

    	nativeRtlSdr.removeListener(this);
    	releaseWakeLock();
		super.onDestroy();
	}
    
    private void aquireWakeLock(){
		final Object powerManagerObj=getSystemService(Context.POWER_SERVICE);
		if (powerManagerObj instanceof PowerManager) {
            final PowerManager powerManager = (PowerManager) powerManagerObj;

            wakelock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, getText(R.string.app_name)+":"+TAG);
            wakelock.setReferenceCounted(false);
            if (!wakelock.isHeld()) {
                // Limit wakelock to one day
                wakelock.acquire(1000 * 60 * 60 * 24);
                Log.i(TAG, "Acquired wake lock. Will keep the screen on.");
            }
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
       	nativeRtlSdr.startAis(startRtlSdrRequest);
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
	public void onRtlSdrException(int exitCode) {

		Log.d(TAG,"NativeRtlSdrListener - onRtlSdrException - "+exitCode);

		Notifications.getInstance().send(this,this.getString(R.string.notification_channel_services_id),getString(R.string.connect_usb_device_status_exception_unknown_title),getString(R.string.connect_usb_device_status_exception_unknown_message)+" Exit code: "+exitCode);
		Analytics.logEvent(this,Analytics.CATEGORY_RTLSDR_DEVICE, TAG,"onRtlSdrException",exitCode);

		// Inform listeners
		for (final RtlSdrServiceListener listener : listeners){
			Log.d(TAG,"NativeRtlSdrListener - onRtlSdrException - listener "+listener);
			listener.onRtlSdrException(exitCode);
		}
	}

	@Override
	public void onRtlSdrStarted() {
		Log.d(TAG,"NativeRtlSdrListener - onRtlSdrStarted");
		
		// Inform listeners
		for (final RtlSdrServiceListener listener : listeners){
            listener.onRtlSdrStarted();
        }
				
		aquireWakeLock();
		
    	Notifications.getInstance().send(this,this.getString(R.string.notification_channel_services_id),"Ships is running","Touch to see the ships.");
	}

    @Override
    public void onRtlSdrStopped() {
    	Log.d(TAG,"NativeRtlSdrListener - onRtlSdrStopped");

		Analytics.logEvent(this,Analytics.CATEGORY_RTLSDR_DEVICE, TAG,"onRtlSdrStopped");

    	// Inform listeners
		for (final RtlSdrServiceListener listener : listeners){
            listener.onRtlSdrStopped();
        }

    	releaseWakeLock();
    }
}
