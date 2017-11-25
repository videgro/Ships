package net.videgro.ships.tasks;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import net.videgro.crypt.DigitalSignature;
import net.videgro.ships.Analytics;
import net.videgro.ships.NmeaTO;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.tasks.domain.DatagramSocketConfig;
import net.videgro.ships.tasks.domain.SocketIoConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class NmeaUdpClientTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "NmeaUdpClientTask";

    private final DatagramSocketConfig datagramSocketConfigIn;
    private final DatagramSocketConfig datagramSocketConfigRepeater;
    private final SocketIoConfig socketIoConfig;

    private final NmeaUdpClientListener listener;
    private final DigitalSignature digitalSignature;

    private int repeatedNmeaMessages = 0;

    public NmeaUdpClientTask(final Resources resources, final NmeaUdpClientListener listener, final DatagramSocketConfig datagramSocketConfigIn, final DatagramSocketConfig datagramSocketConfigRepeater, final SocketIoConfig socketIoConfig) {
        this.listener = listener;
        this.datagramSocketConfigIn = datagramSocketConfigIn;
        this.datagramSocketConfigRepeater = datagramSocketConfigRepeater;
        this.socketIoConfig = socketIoConfig;
        this.digitalSignature = new DigitalSignature(resources);
    }

    public String doInBackground(Void... params) {
        final String tag = "doInBackground - ";
        Thread.currentThread().setName(TAG);

        DatagramSocket serverSocketIn = null;

        Socket socketIo = null;

        if (SettingsUtils.getInstance().parseFromPreferencesNmeaShare()) {
            try {
                socketIo = IO.socket(socketIoConfig.getServer());
                socketIo.connect();
            } catch (URISyntaxException e) {
                socketIo = null;
                Log.e(TAG, "Not possible to connect to SocketIO server", e);
                Analytics.getInstance().logEvent(Analytics.CATEGORY_NMEA_REPEAT, "Not connected", e.getMessage());
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

                    // Repeat to SocketIO-server
                    if (socketIo != null) {
                        final String androidId = socketIoConfig.getAndroidId();
                        final String timestamp = String.valueOf(System.currentTimeMillis());
                        final String dataToSign = androidId + NmeaTO.TOPIC_NMEA_SIGN_SEPARATOR + timestamp + NmeaTO.TOPIC_NMEA_SIGN_SEPARATOR + line;
                        final String signature = digitalSignature.sign(dataToSign);
                        if (signature != null) {
                            final NmeaTO nmeaTO = new NmeaTO();
                            nmeaTO.setOrigin(androidId);
                            nmeaTO.setTimestamp(timestamp);
                            nmeaTO.setData(line);
                            nmeaTO.setSignature(signature);

                            final String json = new Gson().toJson(nmeaTO);
                            socketIo.emit(socketIoConfig.getTopic(), json);
                            repeatedNmeaMessages++;
                        } else {
                            Log.e(TAG, tag + "Not possible to sign data.");
                        }
                    } else {
                        Log.e(TAG, tag + "SocketIO is not set.");
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, tag, e);
        } finally {
            if (serverSocketIn != null && !serverSocketIn.isClosed()) {
                serverSocketIn.close();
            }
            if (socketIo != null) {
                socketIo.disconnect();
            }
        }

        return "FINISHED";
    }

    public void onPostExecute(String result) {
        if (result != null) {
            Log.d(TAG, "Result: " + result);
        }
        Analytics.getInstance().logEvent(Analytics.CATEGORY_NMEA_REPEAT, "Number of messages repeated", String.valueOf(repeatedNmeaMessages));
    }

    public interface NmeaUdpClientListener {
        void onNmeaViaUdpReceived(String line);
    }
}
