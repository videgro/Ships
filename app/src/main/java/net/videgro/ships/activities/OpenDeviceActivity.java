package net.videgro.ships.activities;

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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import net.videgro.ships.Analytics;
import net.videgro.ships.R;
import net.videgro.ships.StartRtlSdrRequest;
import net.videgro.ships.Utils;
import net.videgro.ships.fragments.DeviceDialogFragment;
import net.videgro.ships.services.RtlSdrAisService;
import net.videgro.ships.services.RtlSdrService;
import net.videgro.ships.services.RtlSdrService.RtlSdrServiceListener;
import net.videgro.usb.UsbUtils;
import net.videgro.usb.rtlsdr.RtlSdrDevice;
import net.videgro.usb.rtlsdr.RtlSdrDeviceProvider;
import net.videgro.usb.rtlsdr.RtlSdrDeviceProviderImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * https://developer.android.com/guide/topics/connectivity/usb/host.html
 */
public class OpenDeviceActivity extends FragmentActivity implements RtlSdrServiceListener, DeviceDialogFragment.DeviceDialogListener {
    private static final String TAG = "OpenDeviceActivity";

    private static final RtlSdrDeviceProvider[] DEVICE_PROVIDERS = new RtlSdrDeviceProvider[]{new RtlSdrDeviceProviderImpl()};

    public static final String EXTRA_DISCONNECT = "extra_disconnect";
    public static final String EXTRA_CHANGE_PPM = "extra_change_ppm";
    public static final String EXTRA_RESULT_MESSAGE = "result_message";
    public static final String EXTRA_RESULT_DEVICE_DESCRIPTION = "result_device_description";

    public static final String EXTRA_RESULT_ERROR_REASON = "extra_error_reason";
    public static final int NO_ERROR = 0;
    public static final int ERROR_REASON_RUNNING_ALREADY = 1001;
    public static final int ERROR_REASON_STOP_FAILED = 1002;
    public static final int ERROR_REASON_CHANGE_PPM_FAILED = 1003;
    public static final int ERROR_REASON_NATIVE_LIBRARY_NOT_LOADED = 1004;
    public static final int ERROR_REASON_NO_RTLSDR_DEVICE_FOUND = 1005;
    public static final int ERROR_REASON_NO_RTLSDR_DEVICE_SELECTED = 1006;
    public static final int ERROR_REASON_MISC = 9999;

    private static final String ACTION_USB_PERMISSION = "net.videgro.USB_PERMISSION";

    private RtlSdrService rtlSdrService;
    private String arguments;
    private ServiceConnection rtlsdrServiceConnection;
    private PendingIntent permissionIntent;
    private UsbManager usbManager;
    private UsbDevice currentDevice;
    private RtlSdrDevice rtlSdrDevice;

    /**
     * Parsed value of intent extra: EXTRA_DISCONNECT
     */
    private boolean disconnectRequest = false;

    /**
     * Parsed value of intent extra: EXTRA_CHANGE_PPM
     */
    private int newPpm = 0;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
        if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
            synchronized (this) {
                final UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        currentDevice = device;
                        Log.i(TAG, "permission granted for device: " + device);
                        // Try again to open device, now we have permission
                        final int connectUsbDeviceStatus = openDevice(device);
                        processConnectUsbDeviceStatus(connectUsbDeviceStatus);
                    }
                } else {
                    Log.d(TAG, "permission denied for device: " + device);
                    processConnectUsbDeviceStatus(R.string.connect_usb_device_status_error_permission_denied);
                }
            }
        }
        }
    };

    private void setupRtlsdrServiceConnection(final RtlSdrDevice rtlSdrDevice) {
        Log.d(TAG, "setupRtlsdrServiceConnection");
        this.rtlSdrDevice = rtlSdrDevice;
        rtlsdrServiceConnection = new RtlsdrServiceConnection(this);
        final Intent serviceIntent = new Intent(this, RtlSdrAisService.class);

        // On Android 8+ let service run in foreground
        if (Build.VERSION.SDK_INT >=  Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        bindService(new Intent(this, RtlSdrAisService.class), rtlsdrServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!MainActivity.isNativeLibraryLoaded()) {
            final String msg=getString(R.string.connect_usb_device_status_error_native_library_not_loaded);
            Analytics.logEvent(this,Analytics.CATEGORY_ANDROID_DEVICE,msg, "");
            finish(ERROR_REASON_NATIVE_LIBRARY_NOT_LOADED,msg);
            return;
        }

        setContentView(R.layout.progress);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        registerReceiver(usbReceiver,new IntentFilter(ACTION_USB_PERMISSION));

        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");

        // Parse arguments
        final Uri data = getIntent().getData();
        if (data != null) {
            arguments = data.toString().replace(getString(R.string.opendevice_intent_filter_schema) + "://", "");

            final Bundle extras = getIntent().getExtras();
            if (extras != null) {
                disconnectRequest = extras.getBoolean(EXTRA_DISCONNECT);
                newPpm = extras.getInt(EXTRA_CHANGE_PPM);
            }

            retrieveAvailableDevices();
        } else {
            Log.e(TAG, "Data is null.");
        }
    }

    private void retrieveAvailableDevices() {
        final List<RtlSdrDevice> devices = new ArrayList<>();

        for (final RtlSdrDeviceProvider deviceProvider : DEVICE_PROVIDERS) {
            devices.addAll(deviceProvider.retrieveDevices(this));
        }

        switch (devices.size()) {
            case 0:
                finish(ERROR_REASON_NO_RTLSDR_DEVICE_FOUND, getString(R.string.connect_usb_device_status_error_no_device_found));
                break;
            case 1:
                if (rtlsdrServiceConnection == null) {
                    setupRtlsdrServiceConnection(devices.get(0));
                    // Will continue in: RtlsdrServiceConnection - onServiceConnected (will call also connectUsbDevice())
                }

                break;
            default:
                showDialog(DeviceDialogFragment.invokeDialog(devices), "select_device_dialog");
                break;
        }
    }

    private void showDialog(final DialogFragment dialog, final String nameOfFragment) {
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final Fragment prev = getSupportFragmentManager().findFragmentByTag(nameOfFragment);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        dialog.show(ft, nameOfFragment);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");

        unregisterReceiver(usbReceiver);

        if (rtlSdrService != null) {
            rtlSdrService.unregisterListener(this);
        }

        if (rtlsdrServiceConnection != null) {
            unbindService(rtlsdrServiceConnection);
            rtlsdrServiceConnection = null;
        }
    }

    private int connectUsbDevice() {
        final String tag = "connectUsbDevice - ";
        Log.d(TAG, tag);

        int result;

        if (Utils.is64bit()) {
            Analytics.logEvent(this,Analytics.CATEGORY_ANDROID_DEVICE, "64 bit device", "");
        }

        UsbDevice device = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device == null) {
            device = rtlSdrDevice.getUsbDevice();

            if (device != null) {
                result = openDevice(device);
            } else {
                result = R.string.connect_usb_device_status_error_no_device_found;
            }
        } else {
            Log.d(TAG, tag + "Reusing connected device.");
            result = R.string.connect_usb_device_status_ok;
        }

        currentDevice = device;

        return result;
    }

    private int openDevice(final UsbDevice device) {
        final String tag = "openDevice - ";
        Log.d(TAG, tag);

        int result;

        if (!usbManager.hasPermission(device)) {
            Log.d(TAG, "No permissions to open device: " + device + " - Requesting permission.");
            try {
                usbManager.requestPermission(device, permissionIntent);
                result = R.string.connect_usb_device_status_pending_permission_request;
            } catch (SecurityException e) {
                Log.e(TAG, tag, e);
                Analytics.logEvent(this,Analytics.CATEGORY_ANDROID_DEVICE, tag, e.getMessage());
                result = R.string.connect_usb_device_status_error_security_exception;
            }
        } else {
            result = startRtlSdrService(device);
        }
        return result;
    }

    private int startRtlSdrService(final UsbDevice device) {
        int result;
        final UsbDeviceConnection connection = usbManager.openDevice(device);

        if (connection == null) {
            Log.d(TAG, "Unknown error while opening device: " + device);
            result = R.string.connect_usb_device_status_error_unknown;
        } else {
            final int usbFd = connection.getFileDescriptor();
            final String uspfsPathInput = UsbUtils.deriveProperDeviceName(device.getDeviceName());

            if (rtlSdrService != null) {
                if (!rtlSdrService.isRtlSdrRunning()) {
                    rtlSdrService.startRtlSdr(new StartRtlSdrRequest(arguments, usbFd, uspfsPathInput));
                } else {
                    result = R.string.connect_usb_device_status_error_running_already;
                }
            }
            result = R.string.connect_usb_device_status_ok;
        }

        return result;
    }

    private void finish(final int errorReason, final String message) {
        final Intent data = new Intent();
        data.putExtra(EXTRA_RESULT_MESSAGE, message);
        data.putExtra(EXTRA_RESULT_DEVICE_DESCRIPTION, rtlSdrDevice!=null ? rtlSdrDevice.getFriendlyName() : null);
        data.putExtra(EXTRA_RESULT_ERROR_REASON, errorReason);

        final int resultCode = (errorReason == NO_ERROR) ? RESULT_OK : RESULT_CANCELED;

        if (getParent() == null) {
            setResult(resultCode, data);
        } else {
            getParent().setResult(resultCode, data);
        }

        if (errorReason != NO_ERROR) {
            // Log error to Analytics
            Analytics.logEvent(this, Analytics.CATEGORY_RTLSDR_DEVICE, message, "");
        }

        Log.d(TAG, "finish "+message);
        finish();
    }

    private void processConnectUsbDeviceStatus(final int connectUsbDeviceStatus) {
        // On error, finish this Action with an error result
        switch (connectUsbDeviceStatus) {
            case R.string.connect_usb_device_status_error_no_device_found:
            case R.string.connect_usb_device_status_error_permission_denied:
            case R.string.connect_usb_device_status_error_security_exception:
            case R.string.connect_usb_device_status_error_unknown:
                finish(ERROR_REASON_MISC, getString(connectUsbDeviceStatus));
                break;
            case R.string.connect_usb_device_status_error_stop:
                finish(ERROR_REASON_STOP_FAILED, getString(connectUsbDeviceStatus));
                break;
            case R.string.connect_usb_device_status_error_running_already:
                finish(ERROR_REASON_RUNNING_ALREADY, getString(connectUsbDeviceStatus));
                break;
            case R.string.connect_usb_device_status_change_ppm_failed:
                finish(ERROR_REASON_CHANGE_PPM_FAILED, getString(connectUsbDeviceStatus));
                break;
            case R.string.connect_usb_device_status_change_ppm_ok:
                finish(NO_ERROR, getString(connectUsbDeviceStatus));
                break;
            default:
                // Do nothing
                break;
        }
    }

    /************************** LISTENER IMPLEMENTATIONS ******************/

    /* START implementation RtlSdrServiceListener */
    @Override
    public void onRtlSdrStopped() {
        Log.d(TAG, "onRtlSdrStopped");
        finish(NO_ERROR, getString(R.string.connect_usb_device_status_stopped));
    }

    @Override
    public void onRtlSdrStarted() {
        Log.d(TAG, "onRtlSdrStarted");
        finish(NO_ERROR, getString(R.string.connect_usb_device_status_started));
    }

    @Override
    public void onRtlSdrException(final int exitCode){
        Log.d(TAG, "onRtlSdrException - "+exitCode);
        finish(ERROR_REASON_MISC, getString(R.string.connect_usb_device_status_exception_unknown_title)+" "+getString(R.string.connect_usb_device_status_exception_unknown_message)+" ("+exitCode+")");
    }

	/* START implementation DeviceDialogListener */

    @Override
    public void onDeviceDialogDeviceChosen(final RtlSdrDevice selected) {
        Log.i(TAG, "User has selected device:" + selected.getFriendlyName());
        setupRtlsdrServiceConnection(selected);
    }

    @Override
    public void onDeviceDialogCanceled() {
        Log.i(TAG, "User has canceled the device selection dialog.");
        finish(ERROR_REASON_NO_RTLSDR_DEVICE_SELECTED, getString(R.string.connect_usb_device_status_error_no_rtlsdr_device_selected));
    }

    /************************** PRIVATE CLASS IMPLEMENTATIONS ******************/

    private class RtlsdrServiceConnection implements ServiceConnection {
        private final RtlSdrServiceListener listener;

        // Package protected
        RtlsdrServiceConnection(RtlSdrServiceListener listener) {
            this.listener = listener;
        }

        public void onServiceConnected(ComponentName className, IBinder service) {
            if (service instanceof RtlSdrAisService.ServiceBinder) {
                Log.d(TAG, "RtlsdrServiceConnection - onServiceConnected");
                rtlSdrService = ((RtlSdrAisService.ServiceBinder) service).getService();
                rtlSdrService.registerListener(listener);

                if (!disconnectRequest) {
                    if (newPpm != 0) {
                        Log.d(TAG, "Request to change PPM to: " + newPpm);
                        final boolean changePpmResult = rtlSdrService.changePpm(newPpm);
                        processConnectUsbDeviceStatus(changePpmResult ? R.string.connect_usb_device_status_change_ppm_ok : R.string.connect_usb_device_status_change_ppm_failed);
                    } else if (rtlSdrService.isRtlSdrRunning()) {
                        processConnectUsbDeviceStatus(R.string.connect_usb_device_status_error_running_already);
                    } else {
                        processConnectUsbDeviceStatus(connectUsbDevice());
                    }
                } else {
                    final boolean stopResult = rtlSdrService.stopRtlSdr();
                    // Will continue in: onRtlSdrStopped()

                    if (!stopResult) {
                        processConnectUsbDeviceStatus(R.string.connect_usb_device_status_error_stop);
                    }
                }
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "RtlsdrServiceConnection - onServiceDisconnected");
            rtlSdrService = null;
        }
    }
}

