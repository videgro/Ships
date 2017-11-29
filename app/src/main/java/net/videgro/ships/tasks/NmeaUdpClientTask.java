package net.videgro.ships.tasks;

import android.os.AsyncTask;
import android.util.Log;

import net.videgro.ships.Analytics;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.services.internal.SocketIoClient;
import net.videgro.ships.tasks.domain.DatagramSocketConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NmeaUdpClientTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "NmeaUdpClientTask";

    private final DatagramSocketConfig datagramSocketConfigIn;
    private final DatagramSocketConfig datagramSocketConfigRepeater;

    private final NmeaUdpClientListener listener;

    private final File cacheNmeaFile;
    private final SocketIoClient socketIoClient;
    private final boolean hasDataConnection;

    public NmeaUdpClientTask(final NmeaUdpClientListener listener, final DatagramSocketConfig datagramSocketConfigIn, final DatagramSocketConfig datagramSocketConfigRepeater, final SocketIoClient socketIoClient, final File cacheDirectory, final boolean hasDataConnection) {
        this.listener = listener;
        this.datagramSocketConfigIn = datagramSocketConfigIn;
        this.datagramSocketConfigRepeater = datagramSocketConfigRepeater;
        this.socketIoClient = socketIoClient;
        cacheNmeaFile = new File(cacheDirectory, "data.nmea");
        this.hasDataConnection = hasDataConnection;
    }

    public String doInBackground(Void... params) {
        final String tag = "doInBackground - ";
        Thread.currentThread().setName(TAG);

        DatagramSocket serverSocketIn = null;

        if (SettingsUtils.getInstance().parseFromPreferencesNmeaShare()) {
            if (socketIoClient != null && hasDataConnection) {
                processCachedMessages();
            }
        } else {
            Log.i(TAG, "Not connected to repeat NMEA messages to SocketIO server (user request)");
            Analytics.getInstance().logEvent(Analytics.CATEGORY_NMEA_REPEAT, "Not connected", "User request (settings)");
        }

        try {
            serverSocketIn = new DatagramSocket(datagramSocketConfigIn.getPort(), InetAddress.getByName(datagramSocketConfigIn.getAddress()));
            Log.d(TAG, "Listening on UDP: " + datagramSocketConfigIn);

            if (datagramSocketConfigRepeater != null) {
                Log.d(TAG, "Repeating on UDP: " + datagramSocketConfigRepeater);
            }

            byte[] receiveData = new byte[1024];
            final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            while (!isCancelled()) {
                serverSocketIn.receive(receivePacket);

                // Repeat: Copy packet and send to new destination
                if (datagramSocketConfigRepeater != null) {
                    final DatagramSocket serverSocketRepeater = new DatagramSocket();
                    final DatagramPacket packet = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getByName(datagramSocketConfigRepeater.getAddress()), datagramSocketConfigRepeater.getPort());
                    serverSocketRepeater.send(packet);
                }

                final String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                final String lines[] = sentence.split("\\r?\\n");
                for (final String line : lines) {
                    Log.d(TAG, tag + "NMEA received - " + line);
                    listener.onNmeaViaUdpReceived(line);

                    boolean repeatToSocketIoServerResult=false;
                    if (socketIoClient!=null) {
                        repeatToSocketIoServerResult = socketIoClient.repeatToSocketIoServer(line);
                    }

                    if (!hasDataConnection || !repeatToSocketIoServerResult) {
                        // Cache message
                        cacheMessage(line);
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

    private void cacheMessage(final String line) {
        final String tag = "cacheMessage - ";
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(cacheNmeaFile, true);
            bw = new BufferedWriter(fw);
            bw.write(line + "\n");
        } catch (IOException e) {
            Log.e(TAG, tag, e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    Log.e(TAG, tag + "While closing BufferedWriter.", e);
                }
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    Log.e(TAG, tag + "While closing FileWriter.", e);
                }
            }
        }
    }

    private boolean processCachedMessages() {
        final String tag = "processCachedMessages - ";
        boolean result = true;
        if (cacheNmeaFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(cacheNmeaFile));
                String line;

                while ((line = reader.readLine()) != null && (result)) {
                    result = socketIoClient.repeatToSocketIoServer(line);
                }
            } catch (IOException e) {
                Log.e(TAG, tag, e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, tag + "While closing reader.", e);
                    }
                }

                //noinspection ResultOfMethodCallIgnored
                cacheNmeaFile.delete();
            }
        }
        return result;
    }

    public interface NmeaUdpClientListener {
        void onNmeaViaUdpReceived(String line);
    }
}
