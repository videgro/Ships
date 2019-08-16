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

    public Repeater(Context context,List<DatagramSocketConfig> repeaters){
        this.context=context;
        this.repeaters=repeaters;
    }

    public void repeat(final String nmea){
        final String tag="repeatViaUdp - ";

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
}
