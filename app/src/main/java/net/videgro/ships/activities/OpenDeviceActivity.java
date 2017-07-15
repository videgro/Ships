package net.videgro.ships.activities;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import net.videgro.ships.Analytics;
import net.videgro.ships.R;
import net.videgro.ships.StartRtlSdrRequest;
import net.videgro.ships.Utils;
import net.videgro.ships.services.RtlSdrAisService;
import net.videgro.ships.services.RtlSdrService;
import net.videgro.ships.services.RtlSdrService.RtlSdrServiceListener;
import net.videgro.ships.tools.OpenDeviceHelper;

/**
 * 
 * https://developer.android.com/guide/topics/connectivity/usb/host.html
 */
public class OpenDeviceActivity extends Activity implements RtlSdrServiceListener {
	private static final String TAG="OpenDeviceActivity";

	public static final String EXTRA_DISCONNECT="extra_disconnect";
	public static final String EXTRA_CHANGE_PPM="extra_change_ppm";
	public static final String EXTRA_RESULT_MESSAGE="result_message";
	public static final String EXTRA_RESULT_DEVICE_DESCRIPTION="result_device_description";
	
	public static final String EXTRA_RESULT_ERROR_REASON="extra_error_reason";
	public static final int NO_ERROR=0;
	public static final int ERROR_REASON_RUNNING_ALREADY=1001;
	public static final int ERROR_REASON_STOP_FAILED=1002;
	public static final int ERROR_REASON_CHANGE_PPM_FAILED=1003;
	public static final int ERROR_REASON_MISC=9999;

	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	
	private RtlSdrService rtlSdrService;
    private String arguments;
    private ServiceConnection rtlsdrServiceConnection;
	private PendingIntent permissionIntent;
	private UsbManager usbManager;	
	private UsbDevice currentDevice;

	/**
	 * Parsed value of intent extra: EXTRA_DISCONNECT
	 */
	private boolean disconnectRequest=false;
	
	/**
	 * Parsed value of intent extra: EXTRA_CHANGE_PPM
	 */
	private int newPpm=0;
	
	private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        if (ACTION_USB_PERMISSION.equals(action)) {
	            synchronized (this) {
	                final UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
	                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
	                    if(device != null){
		                  currentDevice=device;
	                      Log.i(TAG,"permission granted for device: "+device);
	                      // Try again to open device, now we have permission
	                      final int connectUsbDeviceStatus = openDevice(device);
	                      processConnectUsbDeviceStatus(connectUsbDeviceStatus);
	                   }
	                }  else {
	                    Log.d(TAG, "permission denied for device: " + device);
	                    processConnectUsbDeviceStatus(R.string.connect_usb_device_status_error_permission_denied);
	                }
	            }
	        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
        		boolean stopResult=rtlSdrService.stopRtlSdr();
                // Will continue in: onRtlSdrStopped()
        		
        		if (!stopResult){
        			processConnectUsbDeviceStatus(R.string.connect_usb_device_status_error_stop);
        		}
	        }
	    }
	};

    private void setupRtlsdrServiceConnection(){
		Log.d(TAG,"setupRtlsdrServiceConnection");
    	rtlsdrServiceConnection = new RtlsdrServiceConnection((RtlSdrServiceListener) this);
    	Intent serviceIntent = new Intent(this, RtlSdrAisService.class);
    	startService(serviceIntent);
		bindService(new Intent(this, RtlSdrAisService.class), rtlsdrServiceConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress);
        
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        Log.d(TAG, "onCreate");
    }        
    
    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG,"onStart"); 
        
        // Parse arguments
	    final Uri data = getIntent().getData();	    
	    arguments = data.toString().replace(getString(R.string.opendevice_intent_filter_schema) + "://", "");
	    
	    final Bundle extras = getIntent().getExtras();
	    if (extras != null) {
	    	disconnectRequest=extras.getBoolean(EXTRA_DISCONNECT);
	    	newPpm=extras.getInt(EXTRA_CHANGE_PPM);	    	
	    }

		registerReceiver(usbReceiver,new IntentFilter(ACTION_USB_PERMISSION));

	    if (rtlsdrServiceConnection==null){
	        setupRtlsdrServiceConnection();
	        // Will continue in: RtlsdrServiceConnection - onServiceConnected (will call also connectUsbDevice())
	    }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy");
        
	   	if (rtlSdrService!=null){
	    	rtlSdrService.unregisterListener(this);
	    }
	    
	    if (rtlsdrServiceConnection!=null){
	    	unbindService(rtlsdrServiceConnection);
	    	rtlsdrServiceConnection=null;
	    }
    }

    @Override
    protected void onStop() {
        Log.i(TAG,"onStop");
		unregisterReceiver(usbReceiver);
        super.onStop();
    }
	
    private int connectUsbDevice(){
    	final String tag="connectUsbDevice - ";
    	Log.d(TAG,tag);

    	int result;

        if (Utils.is64bit()) {
            Analytics.logEvent(this, Analytics.CATEGORY_ANDROID_DEVICE,"64 bit device","");
        }

		UsbDevice device=(UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
		if (device==null){
			device=new OpenDeviceHelper(this).findDevice();
			
			if (device!=null){
				result=openDevice(device);
			} else {
				result=R.string.connect_usb_device_status_error_no_device_found;
			}			
		} else {
			Log.d(TAG,tag+"Reusing connected device.");
			result=R.string.connect_usb_device_status_ok;
		}
		
		currentDevice=device;
    	
		return result;
    }

    private int openDevice(final UsbDevice device) {
    	Log.d(TAG,"openDevice");
    	
    	int result;
    	    	
        if (!usbManager.hasPermission(device)) {
            Log.d(TAG,"No permissions to open device: "+device+" - Requesting permission.");
            usbManager.requestPermission(device, permissionIntent);
            result=R.string.connect_usb_device_status_pending_permission_request;
        } else {
        	result=startRtlSdrService(device);        	
        }
        return result;
    }

    private int startRtlSdrService(final UsbDevice device){
    	int result;
    	UsbDeviceConnection connection = usbManager.openDevice(device);
    	
        if (connection == null){
        	Log.d(TAG,"Unknown error while opening device: "+device);
        	result=R.string.connect_usb_device_status_error_unknown;
        } else {
        	final int usbFd = connection.getFileDescriptor();
            final String uspfsPathInput = OpenDeviceHelper.properDeviceName(device.getDeviceName());
            
            if (rtlSdrService!=null){	            	
            	rtlSdrService.startRtlSdr(new StartRtlSdrRequest(arguments, usbFd, uspfsPathInput));
            }
        	result=R.string.connect_usb_device_status_ok;
        }
        
        return result;
    }

	private void finish(final int errorReason,final String message) {
		  final Intent data = new Intent();
		  data.putExtra(EXTRA_RESULT_MESSAGE, message);
		  data.putExtra(EXTRA_RESULT_DEVICE_DESCRIPTION,usbDevice2string(currentDevice));
		  data.putExtra(EXTRA_RESULT_ERROR_REASON,errorReason);

		  final int resultCode=(errorReason==NO_ERROR) ? RESULT_OK : RESULT_CANCELED;
		  
		  if (getParent() == null) {
		      setResult(resultCode, data);
		  } else {
		      getParent().setResult(resultCode, data);
		  }
		  finish();
	}

	private static String usbDevice2string(final UsbDevice device){
		String result="";
		if (device!=null){
			//result=device.getDeviceName()+" - "+device.getProductName()+", serial: "+device.getSerialNumber()+", version: "+device.getVersion()+", manufacturer: "+device.getManufacturerName()+" ("+device.getVendorId()+":"+device.getDeviceId()+")";
			result=device.getDeviceName()+" ("+device.getVendorId()+":"+device.getProductId()+")";
		}
		return result;
	}
		
    private void processConnectUsbDeviceStatus(final int connectUsbDeviceStatus){    	
    	// On error, finish this Action with an error result
    	switch (connectUsbDeviceStatus) {
			case R.string.connect_usb_device_status_error_no_device_found:
			case R.string.connect_usb_device_status_error_permission_denied:
			case R.string.connect_usb_device_status_error_unknown:
				finish(ERROR_REASON_MISC,getString(connectUsbDeviceStatus));
				break;
			case R.string.connect_usb_device_status_error_stop:
				finish(ERROR_REASON_STOP_FAILED,getString(connectUsbDeviceStatus));
				break;
			case R.string.connect_usb_device_status_error_running_already:
				finish(ERROR_REASON_RUNNING_ALREADY,getString(connectUsbDeviceStatus));
				break;
			case R.string.connect_usb_device_status_change_ppm_failed:
				finish(ERROR_REASON_CHANGE_PPM_FAILED,getString(connectUsbDeviceStatus));
				break;
			case R.string.connect_usb_device_status_change_ppm_ok:
				finish(NO_ERROR,getString(connectUsbDeviceStatus));
				break;
			default:
				// Do nothing
				break;
    	}
    }
    
    /************************** LISTENER IMPLEMENTATIONS ******************/
    
	@Override
	public void onRtlSdrStopped() {
		Log.d(TAG,"onRtlSdrStopped");		
		finish(NO_ERROR,getString(R.string.connect_usb_device_status_stopped));		
	}
	
	@Override
	public void onRtlSdrStarted() {
		Log.d(TAG,"onRtlSdrStarted");
		finish(NO_ERROR,getString(R.string.connect_usb_device_status_started));		
	}

	/************************** PRIVATE CLASS IMPLEMENTATIONS ******************/
		
	private class RtlsdrServiceConnection implements ServiceConnection {
		private final RtlSdrServiceListener listener;

		public RtlsdrServiceConnection(RtlSdrServiceListener listener) {
			this.listener = listener;
		}

		public void onServiceConnected(ComponentName className, IBinder service) {
			if (service instanceof RtlSdrAisService.ServiceBinder) {
				Log.d(TAG,"RtlsdrServiceConnection - onServiceConnected");
				rtlSdrService = ((RtlSdrAisService.ServiceBinder) service).getService();
				rtlSdrService.registerListener(listener);
				
				if (!disconnectRequest){
					if (newPpm!=0){
						Log.d(TAG,"Request to change PPM to: "+newPpm);
						final boolean changePpmResult=rtlSdrService.changePpm(newPpm);
						processConnectUsbDeviceStatus(changePpmResult ? R.string.connect_usb_device_status_change_ppm_ok : R.string.connect_usb_device_status_change_ppm_failed);
					} else if (rtlSdrService.isRtlSdrRunning()){
						processConnectUsbDeviceStatus(R.string.connect_usb_device_status_error_running_already);
					} else {
                        processConnectUsbDeviceStatus(connectUsbDevice());
					}
				} else {
	        		boolean stopResult=rtlSdrService.stopRtlSdr();
	                // Will continue in: onRtlSdrStopped()
	        		
	        		if (!stopResult){
	        			processConnectUsbDeviceStatus(R.string.connect_usb_device_status_error_stop);	        			
	        		}
				}
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG,"RtlsdrServiceConnection - onServiceDisconnected");
			rtlSdrService = null;
		}
	}
}

