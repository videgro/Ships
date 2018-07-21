package net.videgro.ships;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public final class Analytics {
	public static final String CATEGORY_STATISTICS="Statistics";
	public static final String CATEGORY_WARNINGS="Warnings";
	public static final String CATEGORY_ANDROID_DEVICE="Device (Android)";
	public static final String CATEGORY_RTLSDR_DEVICE="Device (RTL-SDR)";
	public static final String CATEGORY_NMEA_REPEAT="NMEA Repeat";

	private static Analytics instance=null;

	private Tracker tracker=null;

	private Analytics(){
		// Singleton, no public constructor
	}

	public static Analytics getInstance(){
		if (instance==null){
			instance=new Analytics();
		}
		return instance;
	}

	public void init(final Context context){
		if (tracker==null) {
            if (context==null){
                throw new IllegalArgumentException("No context set.");
            }
			// Create new tracker
			final GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
			tracker = analytics.newTracker(R.xml.analytics);
		}
	}

	private void validateTracker(){
		if (tracker==null){
			throw new IllegalArgumentException("No tracker set, please init first.");
		}
	}
	
	public synchronized void logScreenView(final String screen){
		validateTracker();

		tracker.setScreenName(screen);
		tracker.send(new HitBuilders.ScreenViewBuilder().build());
	}
	
	public synchronized void logEvent(final String category,final String action,final String label){
		validateTracker();

		tracker.send(new HitBuilders.EventBuilder()
            .setCategory(category)
            .setAction(action)
            .setLabel(label)
            .build());
	}

	public synchronized void logEvent(final String category,final String action,final String label,final long value){
		validateTracker();

		tracker.send(new HitBuilders.EventBuilder()
				.setCategory(category)
				.setAction(action)
				.setLabel(label)
				.setValue(value)
				.build());
	}
}
