package net.videgro.ships.bridge;

import java.util.HashSet;
import java.util.Set;

import android.util.Log;
import net.videgro.ships.StartRtlSdrRequest;

public class NativeRtlSdr {
    private static final String TAG = "NativeRtlSdr";
    private static final Set<NativeRtlSdrListener> LISTENERS = new HashSet<NativeRtlSdrListener>();
    
    /*
     * Load native shared object library
     */
    static {
    	//android.os.Debug.waitForDebugger();
        System.loadLibrary("NativeRtlSdr");
    }

    public boolean addListener(final NativeRtlSdrListener callback) {
    	synchronized (LISTENERS){
    		return LISTENERS.add(callback);
    	}
    }

    public boolean removeListener(final NativeRtlSdrListener callback) {
    	synchronized (LISTENERS){
    		return LISTENERS.remove(callback);
    	}
    }
    
    /*
     * Calls TO native code
     */
    private static native void startRtlSdrAis(final String args, final int fd, final String uspfsPath);
    private static native void stopRtlSdrAis();
    private static native void changeRtlSdrPpm(final int newPpm);
    private static native boolean isRunningRtlSdrAis();

    /*
     * Calls FROM native code
     */
    private static void onMessage(final String data) {
    	Log.d(TAG, "onMessage - "+data);
    	for (final NativeRtlSdrListener listener : LISTENERS){
        	listener.onMessage(data);
        }        
    }

    private static void onError(final String data) {
    	Log.w(TAG, "onError - "+data);
    	for (final NativeRtlSdrListener listener : LISTENERS){
        	listener.onError(data);
        }        
    }

    private static void onException(final int exitCode) {
    	Log.i(TAG,"onException - exitCode: "+exitCode);

    	for (final NativeRtlSdrListener listener : LISTENERS){
        	listener.onException(exitCode);
        }   
    }

    private static void onReady() {
    	Log.d(TAG, "onReady");

        for (final NativeRtlSdrListener listener : LISTENERS){
        	listener.onRtlSdrStarted();
        }
    }

    private static void onPpm(final int ppmCurrent,final int ppmCumulative){
    	Log.d(TAG, "onPpm - current: "+ppmCurrent+", cumulative: "+ppmCumulative);
        for (final NativeRtlSdrListener listener : LISTENERS){
        	listener.onPpm(ppmCurrent,ppmCumulative);
        }
    }

    /**
     * Start native code
     * @param args
     * @param fd
     * @param uspfsPath
     */
    public void startAis(final StartRtlSdrRequest startAisRequest) {
    	Log.d(TAG,"startAis");
    	
        new Thread() {
            public void run() {
                startRtlSdrAis(startAisRequest.getArgs(),startAisRequest.getFd(),startAisRequest.getUspfsPath());
                
                for (final NativeRtlSdrListener c : LISTENERS){
                    c.onRtlSdrStopped();
                }
            };
        }.start();
    }

    /**
     * Stop native code
     */
    public boolean stopAis() {
    	boolean result=false;
    	try {
			if (isRunningRtlSdrAis()) {
				stopRtlSdrAis();
				result=true;
			}
		} catch (Exception e) {
			Log.e(TAG,"stopAis",e);
		}
    	return result;
    }
    
    public boolean changePpm(final int newPpm){
		final String tag="changePpm - ";
		boolean result=false;
		Log.d(TAG,tag+newPpm);
		if (isRunningRtlSdrAis()){
			changeRtlSdrPpm(newPpm);
			result=true;
		} else {
			Log.e(TAG,tag+"RTL SDR AIS must be running.");
		}
		return result;
    }

    public boolean isRunningAis(){
    	return isRunningRtlSdrAis();
    }    

    public interface NativeRtlSdrListener {
    	void onMessage(final String data);
    	void onError(final String data);    
    	void onException(final int exitCode);
    	void onPpm(final int ppmCurrent,final int ppmCumulative);
        void onRtlSdrStarted();
        void onRtlSdrStopped();
    }
}
