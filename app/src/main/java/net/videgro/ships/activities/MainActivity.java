package net.videgro.ships.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import net.videgro.ships.Analytics;
import net.videgro.ships.R;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.Utils;
import net.videgro.ships.fragments.CalibrateFragment;
import net.videgro.ships.fragments.ShowMapFragment;
import net.videgro.ships.listeners.ImagePopupListener;
import net.videgro.usb.UsbUtils;

public class MainActivity extends Activity implements ImagePopupListener {
    private static final String TAG="MainActivity";

    private static final int IMAGE_POPUP_ID_CALIBRATE_WARNING = 1101;
    private static final int IMAGE_POPUP_ID_USB_CONNECTED_DURING_RUNNING = 1102;
    private static final int IMAGE_POPUP_ID_IGNORE = 1109;

    private static boolean nativeLibraryLoaded=false;
    private static boolean active = false;

    private boolean tryingToCalibrate=false;
    private boolean failedToCalibrate=false;
    private boolean showingMap=false;

	/*
     * Load native shared object library
    */
	static {
		//android.os.Debug.waitForDebugger();
        try {
            System.loadLibrary("NativeRtlSdr");
            nativeLibraryLoaded = true;
        } catch (Throwable t) {
            Log.e(TAG,"Error while loading native library.",t);
        }
	}

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null) {

                switch (action) {
                    case UsbManager.ACTION_USB_DEVICE_DETACHED:
                        final UsbDevice detDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        final String detMsg="Device DEtached";
                        Log.v(TAG,detMsg+" "+detDevice);
                        //Analytics.logEvent(this,Analytics.CATEGORY_RTLSDR_DEVICE,detMsg,detDevice.toString());
                        break;
                    case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    case UsbManager.ACTION_USB_ACCESSORY_ATTACHED:
                        final UsbDevice attDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        final String attMsg="Device atached";
                        Log.v(TAG,attMsg+" "+attDevice);
                        //Analytics.logEvent(this,Analytics.CATEGORY_RTLSDR_DEVICE,attMsg,attDevice.toString());
                        deviceAttached();
                    break;
                    default:
                        // Nothing to do
                        break;
                } // END SWITCH
            }
        }
    };

	private void deviceAttached(){
        Utils.showPopup(IMAGE_POPUP_ID_USB_CONNECTED_DURING_RUNNING,this, this, getString(R.string.popup_usb_connected_runtime_title), getString(R.string.popup_usb_connected_runtime_message), R.drawable.warning_icon, null);
        // On dismiss: Will continue onImagePopupDispose
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// FIXME: Observed exception "IllegalAccessException (@MainActivity:onCreate:16) {main}"

        // Init singleton which need the Context
        SettingsUtils.getInstance().init(this);

		setContentView(R.layout.activity_main);
	
		final ActionBar actionBar=getActionBar();
		if (actionBar!=null) {
            actionBar.setDisplayShowTitleEnabled(true);
            //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }

        final IntentFilter filter=new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(usbReceiver,filter);
	}

    @Override
    public void onStart() {
        super.onStart();

        /* In older versions of Android it is not possible to make this activity 'android:launchMode="singleInstance"'
         *  and receive data from an Activity which is started using: 'startActivityForResult'
         *  Source: https://stackoverflow.com/questions/28106855/android-singleinstance-and-startactivityforresult
         *
         *  So here is a trick used to make sure there is just one instance active. Any new instance will be stopped directly.
         *  'active' is a static boolean which will be TRUE when this Activity is started.
         */
        if (active){
            // Active already, stop this instance
            Analytics.logEvent(this,TAG, "stopApplication", "MULTIPLE_INSTANCES Stop this instance");
            finish();
        } else {
            active = true;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(usbReceiver);
        super.onDestroy();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result;
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_help:
				openHelp();
				result = true;
				break;
			case R.id.action_settings:
				openSettings();
				result = true;
				break;
		default:
			result = super.onOptionsItemSelected(item);
		}
		return result;
	}

	private void openSettings() {
		startActivity(new Intent(this, SettingsActivity.class));
	}

    private void openHelp() {
        startActivity(new Intent(this, HelpActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        dispatchFragment();
    }

    private void dispatchFragment(){
	    final String tag="dispatchFragment - ";
        if (UsbUtils.isUsbSupported() && nativeLibraryLoaded) {
            final int ppm = SettingsUtils.getInstance().parseFromPreferencesRtlSdrPpm();

            if (SettingsUtils.isValidPpm(ppm)){
                // Everything ok -> show map
                tryingToCalibrate=false;
                showMap();
            } else {
                if (tryingToCalibrate) {
                    if (SettingsUtils.getInstance().parseFromPreferencesInternalIsCalibrationFailed()){
                        // Returned here after trying to calibrate, but this failed -> Show map
                        tryingToCalibrate=false;

                        // Reset calibration failed
                        SettingsUtils.getInstance().setToPreferencesInternalIsCalibrationFailed(false);
                        failedToCalibrate=true;
                        showMap();
                    } else {
                        Log.d(TAG,tag+"CALIBRATING - Invalid PPM, trying to calibrate and this has not failed.");
                    }
                } else {
                    if (failedToCalibrate){
                        showMap();
                    } else {
                        // Start calibration attempt
                        tryingToCalibrate = true;
                        Utils.showPopup(IMAGE_POPUP_ID_CALIBRATE_WARNING, this, this, getString(R.string.popup_no_ppm_set_title), getString(R.string.popup_no_ppm_set_message), R.drawable.warning_icon, null);
                        // On dismiss: Will continue by switching to CalibrateFragment
                    }
                }
            }
        } else {
            final String msg = getString(R.string.popup_usb_host_mode_not_supported_message);
            Utils.showPopup(IMAGE_POPUP_ID_IGNORE, this, this, getString(R.string.popup_usb_host_mode_title), msg, R.drawable.ic_information, Utils.IMAGE_POPUP_AUTOMATIC_DISMISS);
            // On dismiss: Will continue onImagePopupDispose
        }
    }

    private void gotoFragment(final Fragment fragment){
        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    private void showMap(){
        if (!showingMap){
            showingMap=true;
            gotoFragment(new ShowMapFragment());
        }
    }

    public static boolean isNativeLibraryLoaded(){
        return nativeLibraryLoaded;
    }

    /**** START ImagePopupListener ****/

    @Override
    public void onImagePopupDispose(int id) {
        switch (id) {
            case IMAGE_POPUP_ID_CALIBRATE_WARNING:
                gotoFragment(new CalibrateFragment());
                break;
            case IMAGE_POPUP_ID_USB_CONNECTED_DURING_RUNNING:
                // TODO: Currently we can not restart RTL-SDR native code, so stop application
                Analytics.logEvent(this,TAG, "stopApplication", "IMAGE_POPUP_ID_USB_CONNECTED_DURING_RUNNING");
                finishAffinity();
                System.exit(0);
                break;
            case IMAGE_POPUP_ID_IGNORE:
            default:
                Log.d(TAG,"onImagePopupDispose - id: "+id);
                showMap();
        }
    }

    /**** END ImagePopupListener ****/
}
