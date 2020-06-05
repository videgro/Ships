package net.videgro.ships.services;

import android.app.Service;

import net.videgro.ships.StartRtlSdrRequest;

public abstract class RtlSdrService extends Service {
	
    abstract public void startRtlSdr(final StartRtlSdrRequest startAisRequest);
    abstract public boolean stopRtlSdr();
    abstract public boolean isRtlSdrRunning();
    abstract public boolean registerListener(final RtlSdrServiceListener listener);
    abstract public boolean unregisterListener(final RtlSdrServiceListener listener);
    abstract public boolean changePpm(final int newPpm);
    
    public interface RtlSdrServiceListener {
        void onRtlSdrStarted();
        void onRtlSdrStopped();
        void onRtlSdrException(final int exitCode);
    }
}
