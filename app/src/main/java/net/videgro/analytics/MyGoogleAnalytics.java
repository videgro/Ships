package net.videgro.analytics;

import android.content.Context;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import net.videgro.ships.R;

public final class MyGoogleAnalytics {
	private static Tracker tracker = null;

	private MyGoogleAnalytics() {
		// Utility class, no public constructor
	}

	private static synchronized Tracker getTracker(final Context context) {
		if (tracker == null) {
			// Create new tracker
			final com.google.android.gms.analytics.GoogleAnalytics analytics = com.google.android.gms.analytics.GoogleAnalytics.getInstance(context);
			tracker = analytics.newTracker(R.xml.analytics);
		}
		return tracker;
	}

	public static synchronized void logScreenView(final Context context, final String screen) {
		final Tracker t = getTracker(context);
		t.setScreenName(screen);
		t.send(new HitBuilders.ScreenViewBuilder().build());
	}

	public static synchronized void logEvent(final Context context, final String category, final String action, final String label) {
		final Tracker t = getTracker(context);

		t.send(new HitBuilders.EventBuilder()
				.setCategory(category)
				.setAction(action)
				.setLabel(label)
				.build());
	}

	public static synchronized void logEvent(final Context context, final String category, final String action, final String label,final long value) {
		final Tracker t = getTracker(context);

		t.send(new HitBuilders.EventBuilder()
				.setCategory(category)
				.setAction(action)
				.setLabel(label)
				.setValue(value)
				.build());
	}
}
