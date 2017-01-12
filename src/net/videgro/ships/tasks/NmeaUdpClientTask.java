package net.videgro.ships.tasks;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

public class NmeaUdpClientTask extends AsyncTask<Void, Void, String> {
	private static final String TAG="NmeaUdpClientTask";
	
	private int port;
	
	private NmeaUdpClientListener listener;

	public NmeaUdpClientTask(NmeaUdpClientListener listener,final int port) {
		this.listener = listener;
		this.port=port;
	}

	public String doInBackground(Void... params) {
		Thread.currentThread().setName(TAG);
		
		DatagramSocket serverSocket = null;
		try {
			serverSocket = new DatagramSocket(port);

			Log.d(TAG, "Listening on udp: " + InetAddress.getLocalHost().getHostAddress() + ":" + port);

			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

			while (!isCancelled()) {
				serverSocket.receive(receivePacket);
				final String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
				final String lines[] = sentence.split("\\r?\\n");
				for (final String line : lines) {
					Log.d(TAG,"doInBackground - NMEA received - "+line);
					listener.onNmeaReceived(line);
				}
			}
		} catch (UnknownHostException e) {
			Log.e(TAG, "", e);
		} catch (SocketException e) {
			Log.e(TAG, "", e);
		} catch (IOException e) {
			Log.e(TAG, "", e);
		} finally {
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
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
