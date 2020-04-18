package net.videgro.ships;

import android.content.Context;
import android.util.Log;

import net.videgro.ships.tasks.domain.DatagramSocketConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

public class Repeater {
    private static final String TAG = "Repeater";

    private final Context context;
    private final List<DatagramSocketConfig> repeaters;
    private final boolean mustRepeatToCloud;
    private final MyFirebaseMessagingRepeater myFirebaseMessagingRepeater;

    public Repeater(final Context context,final List<DatagramSocketConfig> repeaters,final boolean mustRepeatToCloud){
        this.context=context;
        this.repeaters=repeaters;
        this.mustRepeatToCloud=mustRepeatToCloud;
        myFirebaseMessagingRepeater=new MyFirebaseMessagingRepeater(context);
    }

    public Context getContext() {
        return context;
    }

    /**
     * Repeat NMEA to other host(s) via UDP
     * @param nmea The NMEA sentence to repeat
     */
    public void repeatViaUdp(final String nmea){
        final String tag="repeatViaUdp - ";

        // Repeat to other host(s) via UDP
        for (final DatagramSocketConfig repeater : repeaters) {
            if (repeater != null) {
                try {
                    final DatagramSocket serverSocketRepeater = new DatagramSocket();
                    final byte[] nmeaAsByteArray = nmea.getBytes();
                    final DatagramPacket packet = new DatagramPacket(nmeaAsByteArray, nmeaAsByteArray.length, InetAddress.getByName(repeater.getAddress()), repeater.getPort());
                    serverSocketRepeater.send(packet);
                } catch (IllegalArgumentException e) {
                    Analytics.logEvent(context, Analytics.CATEGORY_ERRORS, tag, e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, tag, e);
                }
            }
        }
    }

    /**
     * Repeat NMEA o cloud (Firebase)
     * Will be send asynchronous in batches
     * @param nmea The NMEA sentence to repeat
     */
    public void repeatToCloud(final String nmea) {
        if (mustRepeatToCloud) {
            myFirebaseMessagingRepeater.broadcast(nmea);
        }
    }

    public void stopFirebaseMessaging(){
        myFirebaseMessagingRepeater.stop();
    }

    public void startFirebaseMessaging(){
        myFirebaseMessagingRepeater.start();
    }
}
