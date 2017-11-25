package net.videgro.usb;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.Xml;

import net.videgro.ships.R;
import net.videgro.usb.rtlsdr.RtlSdrDevice;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class UsbUtils {
    private final static String TAG = "UsbUtils";

    private static final String DEFAULT_USPFS_PATH = "/dev/bus/usb";

    private static boolean usbSupported = false;
    private static List<SupportedDevice> supportedDevices = null;

    static {
        testUsbSupported();
    }

    private UsbUtils() {
        // Utility class, no public constructor
    }

    private static void testUsbSupported() {
        try {
            usbSupported = Class.forName("android.hardware.usb.UsbManager") != null;
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Android USB is not supported.", e);
        }
    }

    public static List<RtlSdrDevice> retrieveAvailableUsbDevices(final Context context) {
        final List<RtlSdrDevice> result = new LinkedList<>();
        if (usbSupported) {
            final Object usbManagerObj=context.getSystemService(Context.USB_SERVICE);
            if (usbManagerObj instanceof UsbManager) {
                final UsbManager manager = (UsbManager) usbManagerObj;
                retrieveSupportedDevices(context.getResources());

                final HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

                for (final Map.Entry<String, UsbDevice> desc : deviceList.entrySet()) {
                    final UsbDevice candidate = desc.getValue();
                    final Pair<Integer, Integer> candidatePair = new Pair<>(candidate.getVendorId(), candidate.getProductId());

                    SupportedDevice device = null;
                    for (int i = 0; i < supportedDevices.size() && device == null; i++) {
                        final SupportedDevice supportedDevice = supportedDevices.get(i);
                        if (supportedDevice.getVendorAndProductId().equals(candidatePair)) {
                            device = supportedDevice;
                        }
                    }

                    if (device != null) {
                        final String friendlyName = candidate.getDeviceName() + " " + device.getDescription() + " (" + candidate.getVendorId() + ":" + candidate.getProductId() + ")";
                        result.add(new RtlSdrDevice(candidate, friendlyName));
                    }
                }
            }
        }
        return result;
    }

    private static void retrieveSupportedDevices(final Resources resources) {
        final String tag = "retrieveDeviceData - ";

        if (supportedDevices == null) {
            final List<SupportedDevice> result = new ArrayList<>();
            final XmlResourceParser xml = resources.getXml(R.xml.supported_devices);
            try {
                xml.next();
                int eventType;
                while ((eventType = xml.getEventType()) != XmlPullParser.END_DOCUMENT) {
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            if (xml.getName().equals("usb-device")) {
                                final AttributeSet as = Xml.asAttributeSet(xml);
                                final Integer vendorId = parseInt(as.getAttributeValue(null, "vendor-id"));
                                final Integer productId = parseInt(as.getAttributeValue(null, "product-id"));

                                // Read description
                                String description=null;
                                xml.next();
                                if (xml.getName().equals("description")) {
                                    xml.require(XmlPullParser.START_TAG, null, "description");
                                    if (xml.next() == XmlPullParser.TEXT) {
                                        description = xml.getText();
                                        xml.nextTag();
                                    }

                                    xml.require(XmlPullParser.END_TAG, null, "description");
                                }
                                result.add(new SupportedDevice(new Pair<>(vendorId, productId), description));
                            }
                            break;
                    }
                    xml.next();
                }
            } catch (XmlPullParserException | IOException e) {
                Log.e(TAG, tag, e);
            }

            supportedDevices = result;
        }
    }

    public static String deriveProperDeviceName(final String deviceName) {
        String internalDeviceName = deviceName;
        if (deviceName == null) {
            internalDeviceName = DEFAULT_USPFS_PATH;
        } else {
            internalDeviceName = deviceName.trim();
            if (internalDeviceName.isEmpty()) {
                internalDeviceName = DEFAULT_USPFS_PATH;
            } else {
                final String[] paths = internalDeviceName.split("/");
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < paths.length - 2; i++) {
                    if (i == 0) {
                        sb.append(paths[i]);
                    } else {
                        sb.append("/").append(paths[i]);
                    }
                }
                internalDeviceName = sb.toString().trim();
                if (internalDeviceName.isEmpty()) {
                    internalDeviceName = DEFAULT_USPFS_PATH;
                }
            }
        }
        return internalDeviceName;
    }

    private static Integer parseInt(String number) {
        return (number.startsWith("0x") ? Integer.valueOf(number.substring(2), 16) : Integer.valueOf(number, 10));
    }

    public static boolean isUsbSupported(){
        return usbSupported;
    }
}
