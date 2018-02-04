package net.videgro.ships.tasks;

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

    private final NmeaUdpClientListener listener;
    private final SocketIoClient socketIoClient;
    private final boolean hasDataConnection;
    private final NmeaMessagesCache nmeaMessagesCache;

    public NmeaUdpClientTask(final NmeaUdpClientListener listener, final DatagramSocketConfig datagramSocketConfigIn, final SocketIoClient socketIoClient, final File cacheDirectory, final boolean hasDataConnection) {
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

        if (SettingsUtils.getInstance().parseFromPreferencesNmeaShare()) {
            if (socketIoClient != null && hasDataConnection) {
                nmeaMessagesCache.processCachedMessages();
            }
        } else {
            Log.i(TAG, "Not connected to repeat NMEA messages to SocketIO server (user request)");
            Analytics.getInstance().logEvent(Analytics.CATEGORY_NMEA_REPEAT, "Not connected", "User request (settings)");
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

                    // Repeat: Send data of received UDP-packet to Socket.IO server
                    boolean repeatToSocketIoServerResult=false;
                    if (socketIoClient!=null) {
                        repeatToSocketIoServerResult = socketIoClient.repeatToSocketIoServer(line);
                    }

                    // When not possible to send data directly to Socket.IO server, cache is for future transmission.
                    if (!hasDataConnection || !repeatToSocketIoServerResult) {
                        // Cache message
                        nmeaMessagesCache.cacheMessage(line);
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
