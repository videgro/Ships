package net.videgro.ships.services;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import net.videgro.ships.MyFirebaseMessagingRepeater;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FbMsgSrv";

    private LocalBroadcastManager broadcaster;

    public final static String MESSAGING_TOPIC="nmea";
    public final static String LOCAL_BROADCAST_TOPIC="BroadcastNmeaData";
    public final static String LOCAL_BROADCAST_DATA="NmeaData";

    @Override
    public void onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this);
        FirebaseMessaging.getInstance().subscribeToTopic(MESSAGING_TOPIC);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        final String nmeas=remoteMessage.getData().get(MyFirebaseMessagingRepeater.JSON_DATA_FIELD);
        Log.v(TAG,"NMEAs received: "+nmeas);
        final Intent intent = new Intent(LOCAL_BROADCAST_TOPIC);
        intent.putExtra(LOCAL_BROADCAST_DATA, nmeas);
        broadcaster.sendBroadcast(intent);
    }
}
