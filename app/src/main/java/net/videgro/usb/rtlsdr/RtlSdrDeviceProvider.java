package net.videgro.usb.rtlsdr;

import android.content.Context;

import java.util.List;

public interface RtlSdrDeviceProvider {
    List<RtlSdrDevice> retrieveDevices(Context context);

    String getName();
}
