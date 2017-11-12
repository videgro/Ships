package net.videgro.usb.rtlsdr;

import android.content.Context;

import net.videgro.usb.UsbUtils;

import java.util.List;

public class RtlSdrDeviceProviderImpl implements RtlSdrDeviceProvider {
    @Override
    public List<RtlSdrDevice> retrieveDevices(final Context context) {
        return UsbUtils.retrieveAvailableUsbDevices(context);
    }

    @Override
    public String getName() {
        return "RTL-SDR";
    }
}
