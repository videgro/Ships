package net.videgro.ships;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import net.videgro.ships.activities.MainActivity;

public class Notifications {
    private static final String TAG = "Notifications";

    private static Notifications instance;
    private boolean initialized=false;

    private Notifications(){
        // No public constructor, this is a singleton
    }

    public static Notifications getInstance(){
        if (instance==null){
            instance=new Notifications();
        }
        return instance;
    }

    private void init(final Context context){
        createChannels(context);
    }

    private void createChannels(final Context context){
        createChannel(context,context.getString(R.string.notification_channel_general_id),context.getString(R.string.notification_channel_general_name),context.getString(R.string.notification_channel_general_description));
        createChannel(context,context.getString(R.string.notification_channel_services_id),context.getString(R.string.notification_channel_services_name),context.getString(R.string.notification_channel_services_description));
        createChannel(context,context.getString(R.string.notification_channel_repeater_id),context.getString(R.string.notification_channel_repeater_name),context.getString(R.string.notification_channel_repeater_description));
        initialized=true;
    }

    private static void createChannel(final Context context,final String channelId,final String name,final String description){
        /*
         *  https://developer.android.com/training/notify-user/build-notification.html
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library

            final NotificationChannel channel = new NotificationChannel(channelId,name,NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(description);

            final Object notificationManagerObj=context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManagerObj instanceof NotificationManager) {
                ((NotificationManager) notificationManagerObj).createNotificationChannel(channel);
            }
        }
    }

    public Notification createNotification(final Context context,final String channelId,final String title,final String message){
        final String tag="createNotification - ";

        if (!initialized){
            init(context);
        }

        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context,channelId)
                        .setSmallIcon(R.drawable.ic_stat_notification)
                        .setContentTitle(title)
                        .setContentText(message);
        // Creates an explicit intent for an Activity in the app
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
        final PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        return builder.build();
    }

    public void send(final Context context,final String channelId,final String title,final String message){
        final String tag="send - ";

        if (!initialized){
            init(context);
        }

        final Notification notification=createNotification(context,channelId,title,message);

        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager!=null) {
            // messageId allows you to update the notification later on.
            final int messageId=title.hashCode();
            notificationManager.notify(messageId, notification);
            Analytics.logEvent(context,TAG, tag+channelId,title+" "+message);
        } else {
            Analytics.logEvent(context,Analytics.CATEGORY_WARNINGS, tag,"NotificationManager == NULL");
        }
    }
}
