package net.videgro.analytics;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class MyFirebaseAnalytics {
    private static FirebaseAnalytics firebaseAnalytics=null;

    private static FirebaseAnalytics getFirebaseAnalyticsInstance(final Context context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        }
        return firebaseAnalytics;
    }

    public static void logEvent(final Context context, final String category, final String action, final String label) {
        final Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY,category);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,label);
        getFirebaseAnalyticsInstance(context).logEvent(action.replaceAll(" ","_").replaceAll("-",""), bundle);
    }

    public static void logEvent(final Context context, final String category, final String action, final String label, final long value) {
        final Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY,category);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,label);
        bundle.putString(FirebaseAnalytics.Param.VALUE,""+value);
        getFirebaseAnalyticsInstance(context).logEvent(action.replaceAll(" ","_").replaceAll("-",""), bundle);
    }
}
