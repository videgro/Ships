package net.videgro.ships.services;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import net.videgro.ships.Analytics;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.listeners.NmeaReceivedListener;
import net.videgro.ships.tasks.NmeaUdpClientTask;
import net.videgro.ships.tasks.NmeaUdpClientTask.NmeaUdpClientListener;
import net.videgro.ships.tasks.domain.DatagramSocketConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class NmeaUdpClientService extends Service implements NmeaUdpClientListener {
	private static final String TAG = "NmeaUdpClientService";

	public static final int NMEA_UDP_PORT=10109;
    public static final String NMEA_UDP_HOST="127.0.0.1";

	private final IBinder binder = new ServiceBinder();
	private final Set<NmeaReceivedListener> listeners=new HashSet<NmeaReceivedListener>();
	
	private NmeaUdpClientTask nmeaUdpClientTask;

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");

		if (nmeaUdpClientTask!=null && !nmeaUdpClientTask.isCancelled()){
			nmeaUdpClientTask.cancel(true);
			nmeaUdpClientTask=null;
		}
		
		Analytics.logEvent(this, TAG, "destroy", "");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String tag="onStartCommand - ";
		int result = super.onStartCommand(intent, flags, startId);
		Log.d(TAG,tag);

		if (nmeaUdpClientTask==null){
			Log.d(TAG,tag+"Creating new NmeaUdpClient");
            nmeaUdpClientTask = new NmeaUdpClientTask(this,new DatagramSocketConfig(NMEA_UDP_HOST,NMEA_UDP_PORT),createRepeaterConfig());
            nmeaUdpClientTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			Log.d(TAG,tag+"Using existing NmeaUdpClient");
		}
		return result;
	}

    private DatagramSocketConfig createRepeaterConfig(){
        final String tag="createRepeaterConfig - ";
        DatagramSocketConfig result=null;

        final String repeatHost = SettingsUtils.parseFromPreferencesAisMessagesDestinationHost(this);
        final int repeatPort = SettingsUtils.parseFromPreferencesAisMessagesDestinationPort(this);

        String informText="Not repeating NMEA messages.";
        if (repeatHost!=null && !(repeatHost.equals(NMEA_UDP_HOST) && repeatPort==NMEA_UDP_PORT)) {
            result = new DatagramSocketConfig(repeatHost,repeatPort);
            if (validateDatagramSocketConfig(result)) {
                informText="Repeating NMEA messages to UDP: " + result;
				Toast.makeText(this,informText,Toast.LENGTH_LONG).show();
                Analytics.logEvent(this, TAG,"NMEA Repeater","repeatHost: "+repeatHost+", repeatPort: "+repeatPort);
            } else {
                result=null;
            }
        } else {
            informText="IGNORE NMEA repeat setting: Asked to repeat to build in address:port or invalid settings.";
        }

        Log.d(TAG,tag+informText);

        return result;
    }

	private static boolean validateDatagramSocketConfig(final DatagramSocketConfig datagramSocketConfig) {
		final String tag = "validateDatagramSocketConfig - ";

		boolean result = false;

		if (datagramSocketConfig!=null && datagramSocketConfig.getPort() > 0){
			final String address=datagramSocketConfig.getAddress();
			if (address != null && !address.isEmpty()) {
				try {
                    // Ignore result, only interested whether an UnknownHostException is thrown.
					InetAddress.getByName(address);
                    result = true;
				} catch (UnknownHostException e) {
					Log.w(TAG, tag + "Invalid host: "+address, e);
				}
			}
		}
		return result;
	}

	public boolean addListener(NmeaReceivedListener listener) {
		synchronized(listeners){
			return listeners.add(listener);
		}	
	}
	
	public boolean removeListener(NmeaReceivedListener listener) {
		synchronized(listeners){
			return listeners.remove(listener);
		}	
	}

	@Override
	public void onNmeaReceived(String line) {
		Log.d(TAG,"onNmeaReceived - "+line);
		synchronized(listeners){
			for (final NmeaReceivedListener listener:listeners){
				listener.onNmeaReceived(line);
			}
		}	
	}

	public class ServiceBinder extends Binder {
		public NmeaUdpClientService getService() {
			return NmeaUdpClientService.this;
		}
	}
}
