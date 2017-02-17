package net.videgro.ships.tasks;

import android.os.AsyncTask;
import android.util.Log;

import net.videgro.ships.tasks.domain.DatagramSocketConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NmeaUdpClientTask extends AsyncTask<Void, Void, String> {
	private static final String TAG="NmeaUdpClientTask";

	private final DatagramSocketConfig datagramSocketConfigIn;
	private final DatagramSocketConfig datagramSocketConfigRepeater;
	
	private final NmeaUdpClientListener listener;

	public NmeaUdpClientTask(final NmeaUdpClientListener listener,final DatagramSocketConfig datagramSocketConfigIn,final DatagramSocketConfig datagramSocketConfigRepeater) {
		this.listener = listener;
		this.datagramSocketConfigIn=datagramSocketConfigIn;
		this.datagramSocketConfigRepeater=datagramSocketConfigRepeater;
	}

	public String doInBackground(Void... params) {
		final String tag="doInBackground - ";
		Thread.currentThread().setName(TAG);
		
		DatagramSocket serverSocketIn = null;

		try {
			serverSocketIn = new DatagramSocket(datagramSocketConfigIn.getPort(), InetAddress.getByName(datagramSocketConfigIn.getAddress()));
			Log.d(TAG, "Listening on UDP: " + datagramSocketConfigIn);

			if (datagramSocketConfigRepeater!=null){
				Log.d(TAG, "Repeating on UDP: " + datagramSocketConfigRepeater);
			}

			byte[] receiveData = new byte[1024];
			final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

			while (!isCancelled()) {
				serverSocketIn.receive(receivePacket);

                // Repeat: Copy packet and send to new destination
                if (datagramSocketConfigRepeater!=null){
                    final DatagramSocket serverSocketRepeater = new DatagramSocket();
                    final DatagramPacket packet = new DatagramPacket(receivePacket.getData(),receivePacket.getLength(),InetAddress.getByName(datagramSocketConfigRepeater.getAddress()),datagramSocketConfigRepeater.getPort());
                    serverSocketRepeater.send(packet);
                }

				final String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
				final String lines[] = sentence.split("\\r?\\n");
				for (final String line : lines) {
					Log.d(TAG,tag+"NMEA received - "+line);
					listener.onNmeaReceived(line);
				}
			}
		} catch (IOException e) {
			Log.e(TAG,tag, e);
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
	
	public interface NmeaUdpClientListener{
		void onNmeaReceived(String line);
	}
}
