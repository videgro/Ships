package net.videgro.ships.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import net.videgro.ships.Analytics;
import net.videgro.ships.Notifications;
import net.videgro.ships.R;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.listeners.OwnLocationReceivedListener;

public class TrackService extends Service implements LocationListener {
	private static final String TAG = "TrackService";

	private LocationManager locationManager;
	private final IBinder binder = new ServiceBinder();
	private OwnLocationReceivedListener listener;

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");

		SettingsUtils.getInstance().init(this);

		// Get the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");

		if (locationManager != null) {
			locationManager.removeUpdates(this);
		}
		Analytics.logEvent(this,TAG, "destroy", "");
	}

	private void askLocationManagerForLocationUpdates() {
		final String tag = "askLocationManagerForLocationUpdates - ";
		// Define the criteria how to select the location provider -> use
		// default
		Criteria criteria = new Criteria();

		final String provider = locationManager.getBestProvider(criteria, true);
		Log.i(TAG, "Provider " + provider + " has been selected.");
		Analytics.logEvent(this,TAG, "SelectedProvider", provider);

		if (provider != null) {
			try {
				Location location = locationManager.getLastKnownLocation(provider);
				if (location != null) {
					onLocationChanged(location);
				}

				/*
				 * Minimum time: 1000 ms Minimum distance: 8 meters
				 *
				 */
				locationManager.requestLocationUpdates(provider, 1000, 8, this);
				Analytics.logEvent(this,TAG, tag, "");
			} catch (SecurityException e){
				Log.e(TAG,tag+"SecurityException",e);
				Analytics.logEvent(this,TAG, tag, e.getMessage());
			}

		} else {
			Log.w(TAG, tag + "No Provider available.");
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final int result = super.onStartCommand(intent, flags, startId);
		Log.d(TAG, "onStartCommand");

		// On Android 8+ let TrackService run in foreground
		// More information: https://developer.android.com/about/versions/oreo/background-location-limits.html
		//
		// After startForegroundService, we must call startForeground in service.
		//
		// See: 1) https://stackoverflow.com/questions/44425584/context-startforegroundservice-did-not-then-call-service-startforeground
		//      2) https://stackoverflow.com/questions/46375444/remoteserviceexception-context-startforegroundservice-did-not-then-call-servic
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			final Notification notification = Notifications.getInstance().createNotification(this, getString(R.string.notification_channel_services_id), getString(R.string.notification_service_tracker_title),getString(R.string.notification_service_tracker_description));
			// Send always the same ID (TAG.hashCode()) so user won't be spammed by different multiple instances of the same notification.
			startForeground(TAG.hashCode(), notification);
		}

		if (locationManager != null) {
			// check if enabled and if not send user to the GPS settings
			// Better solution would be to display a dialog and suggesting to
			// go to the settings
			if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				Notifications.getInstance().send(this,getString(R.string.notification_channel_services_id),"Own location",getString(R.string.msg_gps_not_enabled));
			} else {
				askLocationManagerForLocationUpdates();
			}
		} else {
			Log.w(TAG, "onStartCommand - No LocationManager available.");
		}

		return result;
	}

	@Override
	public void onLocationChanged(final Location location) {
		if (listener != null) {
			listener.onOwnLocationReceived(location);
		}
	}

	public void setListener(OwnLocationReceivedListener listener) {
		this.listener = listener;
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(TAG, "Enabled new provider " + provider);
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.w(TAG, "Disabled provider " + provider + ". Can't track anymore.");
	}

	public class ServiceBinder extends Binder {
		public TrackService getService() {
			return TrackService.this;
		}
	}
}
