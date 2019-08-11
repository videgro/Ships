package net.videgro.ships.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import net.videgro.ships.Analytics;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.services.internal.SocketIoClient;
import net.videgro.ships.tasks.domain.DatagramSocketConfig;
import net.videgro.ships.tasks.internal.NmeaMessagesCache;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NmeaUdpClientTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "NmeaUdpClientTask";

    private final DatagramSocketConfig datagramSocketConfigIn;

    private final Context context;
    private final NmeaUdpClientListener listener;
    private final SocketIoClient socketIoClient;
    private final boolean hasDataConnection;
    private final NmeaMessagesCache nmeaMessagesCache;

    public NmeaUdpClientTask(final Context context,final NmeaUdpClientListener listener, final DatagramSocketConfig datagramSocketConfigIn, final SocketIoClient socketIoClient, final File cacheDirectory, final boolean hasDataConnection) {
        this.context = context;
        this.listener = listener;
        this.datagramSocketConfigIn = datagramSocketConfigIn;
        this.socketIoClient = socketIoClient;
        this.hasDataConnection = hasDataConnection;
        nmeaMessagesCache=new NmeaMessagesCache(cacheDirectory,socketIoClient);
    }

    public String doInBackground(Void... params) {
        final String tag = "doInBackground - ";
        Thread.currentThread().setName(TAG);

        DatagramSocket serverSocketIn = null;

        final boolean shareNmea=SettingsUtils.getInstance().parseFromPreferencesNmeaShare();
        Analytics.logEvent(context,Analytics.CATEGORY_NMEA_REPEAT, "Share NMEA - User preferences",String.valueOf(shareNmea));
        Analytics.logEvent(context,Analytics.CATEGORY_NMEA_REPEAT, "Share NMEA - Has data connection",String.valueOf(hasDataConnection));

        if (shareNmea && (socketIoClient != null && hasDataConnection)) {
            nmeaMessagesCache.processCachedMessages();
        }

        try {
            serverSocketIn = new DatagramSocket(datagramSocketConfigIn.getPort(), InetAddress.getByName(datagramSocketConfigIn.getAddress()));
            Log.d(TAG, "Listening on UDP: " + datagramSocketConfigIn);

            byte[] receiveData = new byte[1024];
            final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            while (!isCancelled()) {
                serverSocketIn.receive(receivePacket);

                // Split received data into lines (strings)
                final String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                final String lines[] = sentence.split("\\r?\\n");

                for (final String line : lines) {
                    Log.d(TAG, tag + "NMEA received - " + line);
                    listener.onNmeaViaUdpReceived(line);

                    if (shareNmea) {
                        // Repeat: Send data of received UDP-packet to Socket.IO server, when user selected to share NMEA message.
                        boolean repeatToSocketIoServerResult = false;
                        if (socketIoClient != null) {
                            repeatToSocketIoServerResult = socketIoClient.repeatToSocketIoServer(line);
                        }

                        // Cache message when there was no network connection when starting Task or when repeatToSocketIoServer failed.
                        //if (!hasDataConnection || !repeatToSocketIoServerResult) {
                        // TODO: Experimental - Network connection can change when Task has started. To be safe cache all messages. Cached messages will be processed, next time app starts and there is a network connection available.
                            // Cache message
                            nmeaMessagesCache.cacheMessage(line);
                        //}
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, tag, e);
        } finally {
            if (serverSocketIn != null && !serverSocketIn.isClosed()) {
                serverSocketIn.close();
            }
        }

        return "FINISHED";
    }

    public void onPostExecute(String result) {
        if (result != null) {
            Log.d(TAG, "Result: " + result);
        }
    }

    public interface NmeaUdpClientListener {
        void onNmeaViaUdpReceived(String line);
    }
}
