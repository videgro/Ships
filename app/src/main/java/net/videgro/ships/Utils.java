package net.videgro.ships;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdRequest.Builder;
import com.google.android.gms.ads.AdView;

import net.videgro.ships.activities.MainActivity;
import net.videgro.ships.listeners.ImagePopupListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Utils {
	private static final String TAG = "Utils";
	
	private static final SimpleDateFormat LOG_TIME_FORMAT = new SimpleDateFormat("[HH:mm:ss] ", Locale.getDefault());
	
	private Utils(){
		// Utility class, no public constructor
	}
	
	public static boolean haveNetworkConnection(Context context) {
	    boolean haveConnectedWifi = false;
	    boolean haveConnectedMobile = false;

	    final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    if (cm!=null) {
			NetworkInfo[] netInfo = cm.getAllNetworkInfo();
			for (NetworkInfo ni : netInfo) {
				if (ni.getTypeName().equalsIgnoreCase("WIFI")) {
					if (ni.isConnected()) {
						haveConnectedWifi = true;
					}
				}
				if (ni.getTypeName().equalsIgnoreCase("MOBILE")) {
					if (ni.isConnected()) {
						haveConnectedMobile = true;
					}
				}
			}
		}
	    return haveConnectedWifi || haveConnectedMobile;
	}

	public static void loadAd(final View view){
		final Builder builder = new AdRequest.Builder();		
		builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR); // Emulator
		
		// Add test devices
		final String[] testDevices = view.getContext().getString(R.string.testDevices).split(",");		
	    for (String testDevice:testDevices){
	    	builder.addTestDevice(testDevice);
	    	
	    }
	    
	    final AdView adView = (AdView) view.findViewById(R.id.adView);
	    adView.loadAd(builder.build());
	}
	
	public static void sendNotification(final Context context,final String postfix,final String message){
		final Notification.Builder mBuilder =
		        new Notification.Builder(context)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentTitle(context.getText(R.string.app_name)+" "+postfix)
		        .setContentText(message);
		// Creates an explicit intent for an Activity in your app
		final Intent resultIntent = new Intent(context, MainActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		final TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(MainActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (mNotificationManager!=null) {
            // mId allows you to update the notification later on.
            int mId = 0;
            mNotificationManager.notify(mId, mBuilder.build());

            Analytics.getInstance().logEvent(TAG, "sendNotification", "message");
        }
	}	
	
	public static void logStatus(final Activity activity,final TextView textView,final String status) {
		final String tag="logStatus - ";
		Log.d(TAG,tag+status);
		if (activity!=null){
			final String text = LOG_TIME_FORMAT.format(new Date()) + status;
			updateText(activity,textView,text);
		} else {
			Log.e(TAG,tag+"Huh? No activity set. ("+status+")");
		}
	}

	private static void updateText(final Activity activity,final TextView textView,final String text) {
		final String tag="updateText - ";
		if (activity!=null){
			activity.runOnUiThread(new Runnable() {
				public void run() {					
					textView.setText(text+"\n"+textView.getText());
				}
			});
		} else {
			Log.e(TAG,tag+"Huh? No activity set. ("+text+")");
		}
	}

	public static void showPopup(final int id,final Activity activity,final ImagePopupListener listener,final String title,final String message,final int imageResource,final Long automaticDismissDelay){
		final String tag="showPopup - ";

		activity.runOnUiThread(new Runnable() {
		    public void run() {
		    	final AlertDialog.Builder ad = new AlertDialog.Builder(activity);
				ad.setTitle(title);
				ad.setMessage(Html.fromHtml(message));
				ad.setIcon(imageResource);
				ad.setNeutralButton("OK", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						if (listener!=null){
							listener.onImagePopupDispose(id);
						}
					}
				});

				final AlertDialog alert = ad.create();
				alert.show();

				if (automaticDismissDelay != null) {
					final Handler handler = new Handler();
					final Runnable runnable = new Runnable() {
						public void run() {
							if (alert.isShowing()) {
								try {
									alert.dismiss();
								} catch (IllegalArgumentException e) {
									// FIXME: Ugly fix (View not attached to window manager)
									Log.e(TAG,tag+"Auto dismiss", e);
								}
							}
						}
					};
					handler.postDelayed(runnable, automaticDismissDelay);
				}
		    }
		});		
	}


	public static boolean is64bit(){
		final String VAL_64="64";
		// API level 21+ use Build.SUPPORTED_64_BIT_ABIS
		return (android.os.Build.CPU_ABI!=null && android.os.Build.CPU_ABI.contains(VAL_64)) || (android.os.Build.CPU_ABI2!=null && android.os.Build.CPU_ABI2.contains(VAL_64));
	}

    public static String retrieveAbi(){
        return ((android.os.Build.CPU_ABI!=null) ? android.os.Build.CPU_ABI:"")+((android.os.Build.CPU_ABI2!=null) ? " - "+android.os.Build.CPU_ABI2:"");
    }
}
