package net.videgro.ships.services;

import net.videgro.ships.StartRtlSdrRequest;
import android.app.Service;

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
    }
}
