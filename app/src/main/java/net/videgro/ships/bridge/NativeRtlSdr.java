package net.videgro.ships.bridge;

import android.util.Log;

import androidx.annotation.Keep;

import net.videgro.ships.StartRtlSdrRequest;

import java.util.HashSet;
import java.util.Set;

public class NativeRtlSdr {
    private static final String TAG = "NativeRtlSdr";
    private static final Set<NativeRtlSdrListener> LISTENERS = new HashSet<>();

    public boolean addListener(final NativeRtlSdrListener callback) {
        synchronized (LISTENERS) {
            return LISTENERS.add(callback);
        }
    }

    public boolean removeListener(final NativeRtlSdrListener callback) {
        synchronized (LISTENERS) {
            return LISTENERS.remove(callback);
        }
    }

    /*
     * Calls TO native code
     */
    public native void startRtlSdrAis(final String args, final int fd, final String uspfsPath);

    public native void stopRtlSdrAis();

    public native void changeRtlSdrPpm(final int newPpm);

    public native boolean isRunningRtlSdrAis();

    /*
     * Calls FROM native code
     */
    @Keep
    public void onException(final int exitCode) {
        Log.i(TAG, "onException - exitCode: " + exitCode);

        for (final NativeRtlSdrListener listener : LISTENERS) {
            listener.onRtlSdrException(exitCode);
        }
    }

    @Keep
    public void onReady() {
        Log.d(TAG, "onReady");

        for (final NativeRtlSdrListener listener : LISTENERS) {
            listener.onRtlSdrStarted();
        }
    }

    public void startAis(final StartRtlSdrRequest startAisRequest) {
        final String tag="startAis - ";
        Log.d(TAG, tag);

        new Thread() {
            public void run() {
                startRtlSdrAis(startAisRequest.getArgs(), startAisRequest.getFd(), startAisRequest.getUspfsPath());
                Log.d(TAG, tag+"STOPPED");

                for (final NativeRtlSdrListener c : LISTENERS) {
                    c.onRtlSdrStopped();
                }
            }
        }.start();
    }

    /**
     * Stop native code
     */
    public boolean stopAis() {
        boolean result = false;
        try {
            if (isRunningRtlSdrAis()) {
                stopRtlSdrAis();
                result = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "stopAis", e);
        }
        return result;
    }

    public boolean changePpm(final int newPpm) {
        final String tag = "changePpm - ";
        boolean result = false;
        Log.d(TAG, tag + newPpm);
        if (isRunningRtlSdrAis()) {
            changeRtlSdrPpm(newPpm);
            result = true;
        } else {
            Log.e(TAG, tag + "RTL SDR AIS must be running.");
        }
        return result;
    }

    public boolean isRunningAis() {
        return isRunningRtlSdrAis();
    }

    public interface NativeRtlSdrListener {
        void onRtlSdrException(final int exitCode);
        void onRtlSdrStarted();
        void onRtlSdrStopped();
    }
}
