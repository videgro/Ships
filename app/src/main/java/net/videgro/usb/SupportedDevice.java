package net.videgro.usb;

import android.util.Pair;

public class SupportedDevice {
    private final Pair<Integer, Integer> vendorAndProductId;
    private final String description;

    // Package protected
    SupportedDevice(Pair<Integer, Integer> vendorAndProductId, String description) {
        this.vendorAndProductId = vendorAndProductId;
        this.description = description;
    }

    // Package protected
    Pair<Integer, Integer> getVendorAndProductId() {
        return vendorAndProductId;
    }

    public String getDescription() {
        return description;
    }
}
