package net.videgro.ships.services;

import net.videgro.ships.services.RtlSdrService.RtlSdrServiceListener;
import android.os.Binder;

public abstract class RtlSdrServiceBinder extends Binder {
	abstract public RtlSdrService getService(final RtlSdrServiceListener listener);
}
