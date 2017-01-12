package net.videgro.ships.tools;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import net.videgro.ships.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

public class OpenDeviceHelper {
	private static final String TAG="OpenDeviceHelper";

	public static final String USB_PERMISSIONS="777";
    public static final String DEFAULT_USPFS_PATH = "/dev/bus/usb";
    
    private static final String NAME_USB_DEVICE="usb-device";
    private static final String ATTRIBUTE_VENDOR_ID="vendor-id";
    private static final String ATTRIBUTE_PRODUCT_ID="product-id";
    
    private static final String PREFIX_VENDOR_ID="v";
    private static final String PREFIX_PRODUCT_ID="p";
    private static final int HEX_RADIX=16;

    private final Context context;

    public OpenDeviceHelper(final Context context){
    	this.context=context;
    }

    public UsbDevice findDevice() {
    	final String tag="findDevice - ";
    	UsbDevice result=null;
    	
        final UsbManager manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList=null;
        try {
        	deviceList = manager.getDeviceList();
        } catch (NullPointerException e){
        	Log.v(TAG,tag,e);
        	Log.w(TAG,tag+"Nullpointer when trying to retrieve USB device list.");
        	/*
        	* Observed this error when requesting via emulator (on devices without USB support?)
        	*         	
        	* java.lang.NullPointerException: Attempt to invoke interface method 'void android.hardware.usb.IUsbManager.getDeviceList(android.os.Bundle)' on a null object reference
        	* at android.hardware.usb.UsbManager.getDeviceList(UsbManager.java:295)
        	*/
        }
        
        if (deviceList!=null && !deviceList.isEmpty()) {
        	final HashSet<String> supportedDevices = getSupportedDevices();            
            for (final String deviceKey : deviceList.keySet()) {
                final UsbDevice usbDevice = deviceList.get(deviceKey);
                final String deviceIdentificationString = PREFIX_VENDOR_ID+usbDevice.getVendorId()+PREFIX_PRODUCT_ID+usbDevice.getProductId();
                if (supportedDevices.contains(deviceIdentificationString)) {
                	if (result!=null){
                		Log.w(TAG,"Multiple matching devices. For now, auto select the last matching device.");
                	}
                	result=usbDevice;
                }    
            }
        }

        return result;
    }
    
    private HashSet<String> getSupportedDevices() {
        final HashSet<String> ans = new HashSet<String>();
        try {
            final XmlResourceParser xml = context.getResources().getXml(R.xml.supported_devices);

            xml.next();
            int eventType;
            while ((eventType = xml.getEventType()) != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (xml.getName().equals(NAME_USB_DEVICE)) {
                            final AttributeSet as = Xml.asAttributeSet(xml);
                            final Integer vendorId = Integer.valueOf(as.getAttributeValue(null,ATTRIBUTE_VENDOR_ID),HEX_RADIX);
                            final Integer productId = Integer.valueOf(as.getAttributeValue(null,ATTRIBUTE_PRODUCT_ID),HEX_RADIX);
                            ans.add(PREFIX_VENDOR_ID+vendorId+PREFIX_PRODUCT_ID+productId);
                        }
                        break;
                }
                xml.next();
            }
        } catch (IOException e) {
            Log.e(TAG,"getDeviceData",e);
        } catch (XmlPullParserException e){
        	Log.e(TAG,"getDeviceData",e);
        }

        return ans;
    }
    
    public final static String properDeviceName(final String deviceName) {
    	String internalDeviceName=deviceName;
        if (deviceName == null) {
        	internalDeviceName=DEFAULT_USPFS_PATH;
        } else {
        	internalDeviceName = deviceName.trim();        
	        if (internalDeviceName.isEmpty()) {
	        	internalDeviceName=DEFAULT_USPFS_PATH;
	        } else {
		        final String[] paths = internalDeviceName.split("/");
		        final StringBuilder sb = new StringBuilder();
		        for (int i = 0; i < paths.length-2; i++){
		            if (i == 0){
		                sb.append(paths[i]);
		            } else {
		                sb.append("/"+paths[i]);
		            }
		        }
		        internalDeviceName = sb.toString().trim();
		        if (internalDeviceName.isEmpty()){
		        	internalDeviceName=DEFAULT_USPFS_PATH;
		        }
	        }
        }
        return internalDeviceName;
    }
}


