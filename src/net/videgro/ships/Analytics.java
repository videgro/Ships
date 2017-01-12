package net.videgro.ships;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

public final class Analytics {
	private static Tracker analyticsTracker=null;
	
	private Analytics(){
		// Utility class, no public constructor
	}

	private static synchronized Tracker getTracker(final Context context){		
		if (analyticsTracker==null){
			// Create new tracker
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
		    analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
		    analyticsTracker =  analytics.newTracker(R.xml.analytics);
		}
		return analyticsTracker;		    
	}
	
	public static synchronized void logScreenView(final Context context,final String screen){
		Tracker t = getTracker(context);
        t.setScreenName(screen);
        t.send(new HitBuilders.AppViewBuilder().build());
	}
	
	public static synchronized void logEvent(final Context context,final String category,final String action,final String label){
		Tracker t = getTracker(context);

		t.send(new HitBuilders.EventBuilder()
            .setCategory(category)
            .setAction(action)
            .setLabel(label)
            .build());
	}   
}
