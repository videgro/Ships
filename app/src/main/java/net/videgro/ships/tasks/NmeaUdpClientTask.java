package net.videgro.ships.tasks;

import android.os.AsyncTask;
import android.util.Log;

import net.videgro.ships.listeners.NmeaReceivedListener;
import net.videgro.ships.services.NmeaClientService;
import net.videgro.ships.tasks.domain.DatagramSocketConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NmeaUdpClientTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "NmeaUdpClientTask";

    private final DatagramSocketConfig datagramSocketConfig;

    private final NmeaReceivedListener listener;
    private final NmeaClientService.Source source;

    private DatagramSocket socket;
    private boolean stopped=false;

    public NmeaUdpClientTask(final NmeaReceivedListener listener,final NmeaClientService.Source source,final DatagramSocketConfig datagramSocketConfig) {
        this.listener = listener;
        this.source = source;
        this.datagramSocketConfig = datagramSocketConfig;
    }

    public void stop(){
        stopped=true;

        if (socket!=null){
            // Will stop the socket.receive() call blocking
            socket.close();
        }
    }

    public String doInBackground(Void... params) {
        final String tag = "doInBackground - ";
        Thread.currentThread().setName(TAG+datagramSocketConfig);

        try {
            socket = new DatagramSocket(datagramSocketConfig.getPort(), InetAddress.getByName(datagramSocketConfig.getAddress()));
            Log.d(TAG, "Listening on UDP: " + datagramSocketConfig);

            byte[] receiveData = new byte[1024];
            final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            while (!stopped) {
                socket.receive(receivePacket);

                // Split received data into lines (strings)
                final String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                final String[] lines = sentence.split("\\r?\\n");

                for (final String line : lines) {
                    Log.v(TAG, tag + "NMEA received - " + line);
                    listener.onNmeaReceived(line,source);
                }
            }
        } catch (IOException e) {
            if (stopped){
                Log.i(TAG, tag+"Task has been stopped and socket closed. Ignore this exception.", e);
            } else {
                Log.e(TAG, tag, e);
            }
        } finally {
            if (socket!=null && !socket.isClosed()){
                socket.close();
                socket=null;
            }
        }

        return "FINISHED";
    }

    public void onPostExecute(String result) {
        if (result != null) {
            Log.d(TAG, "Result: " + result);
        }
    }
}
