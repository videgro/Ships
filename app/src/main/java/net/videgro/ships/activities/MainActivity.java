package net.videgro.ships.activities;

import android.app.ActionBar;
import android.app.Activity;
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
import net.videgro.ships.fragments.ShowMapFragment;

public class MainActivity extends Activity {
    private static final String TAG="MainActivity";

    private static boolean nativeLibraryLoaded=false;

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
                        final UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        final String msg="Device DEtached";
                        Log.v(TAG,msg+" "+device);
                        Analytics.getInstance().logEvent(Analytics.CATEGORY_RTLSDR_DEVICE,msg,device.toString());
                        break;
                    default:
                        // Nothing to do
                        break;
                } // END SWITCH
            }
        }
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// FIXME: Observed exception "IllegalAccessException (@MainActivity:onCreate:16) {main}"

        // Init some singletons which need the Context
        Analytics.getInstance().init(this);
        SettingsUtils.getInstance().init(this);

		setContentView(R.layout.activity_main);
	
		final ActionBar actionBar=getActionBar();
		if (actionBar!=null) {
            actionBar.setDisplayShowTitleEnabled(true);
            //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }

        final IntentFilter filter=new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver,filter);

        getFragmentManager().beginTransaction().replace(R.id.container, new ShowMapFragment()).commit();
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
		boolean result = false;
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

    public static boolean isNativeLibraryLoaded(){
	    return nativeLibraryLoaded;
    }
}
