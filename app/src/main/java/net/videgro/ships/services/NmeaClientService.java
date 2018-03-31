package net.videgro.ships.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import net.videgro.ships.Analytics;
import net.videgro.ships.NmeaTO;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.Utils;
import net.videgro.ships.listeners.ShipReceivedListener;
import net.videgro.ships.nmea2ship.Nmea2Ship;
import net.videgro.ships.nmea2ship.domain.Ship;
import net.videgro.ships.services.internal.SocketIoClient;
import net.videgro.ships.tasks.NmeaUdpClientTask;
import net.videgro.ships.tasks.NmeaUdpClientTask.NmeaUdpClientListener;
import net.videgro.ships.tasks.ValidateDatagramSocketConfigTask;
import net.videgro.ships.tasks.domain.DatagramSocketConfig;
import net.videgro.ships.tasks.domain.SocketIoConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class NmeaClientService extends Service implements NmeaUdpClientListener, SocketIoClient.SocketIoListener,ValidateDatagramSocketConfigTask.ValidateDatagramSocketConfigListener {
	private static final String TAG = "NmeaClientService";

	public static final int NMEA_UDP_PORT=10109;
    public static final String NMEA_UDP_HOST="127.0.0.1";

	private final IBinder binder = new ServiceBinder();
	private final Set<ShipReceivedListener> listeners=new HashSet<>();

    private Nmea2Ship nmea2Ship = new Nmea2Ship();
	private NmeaUdpClientTask nmeaUdpClientTask;

	private SocketIoClient socketIoClient;

	private boolean relayNmeaFromPeers=true;
    private DatagramSocketConfig datagramSocketConfigRepeater;

    /**
     * Contains all received MMSIs. A set contains unique entries.
     */
    private Set<Integer> mmsiReceivedViaUdp = new HashSet<>();
    private Set<Integer> mmsiReceivedViaSocketIo = new HashSet<>();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

    @Override
    public boolean onUnbind(Intent intent) {
        if (mmsiReceivedViaUdp.isEmpty()) {
            Analytics.getInstance().logEvent(Analytics.CATEGORY_STATISTICS,"No ships received - UDP",Utils.retrieveAbi());
        } else {
            Analytics.getInstance().logEvent(Analytics.CATEGORY_STATISTICS,"Number of received ships - UDP", Utils.retrieveAbi(),mmsiReceivedViaUdp.size());
        }

        if (mmsiReceivedViaSocketIo.isEmpty()) {
            Analytics.getInstance().logEvent(Analytics.CATEGORY_STATISTICS,"No ships received - SocketIO",Utils.retrieveAbi());
        } else {
            Analytics.getInstance().logEvent(Analytics.CATEGORY_STATISTICS,"Number of received ships - SocketIO", Utils.retrieveAbi(),mmsiReceivedViaSocketIo.size());
        }
        return super.onUnbind(intent);
    }

    @Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");

		// Init some singletons which need the Context
		Analytics.getInstance().init(this);
		SettingsUtils.getInstance().init(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String tag="onStartCommand - ";
		int result = super.onStartCommand(intent, flags, startId);
		Log.d(TAG,tag);

        @SuppressLint("HardwareIds")
		final String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        connectSocketIo(new SocketIoConfig(androidId, NmeaTO.SERVER, NmeaTO.TOPIC_NMEA));

        relayNmeaFromPeers=SettingsUtils.getInstance().parseFromPreferencesRelayNmeaFromPeers();

		if (nmeaUdpClientTask==null){
			Log.d(TAG,tag+"Creating new NmeaUdpClient");
            createRepeaterConfig();
            // Flow: ValidateDatagramSocketConfigTask->onValidatedDatagramSocketConfig and this function will create a new NmeaUdpClientTask
		} else {
			Log.d(TAG,tag+"Using existing NmeaUdpClient");
		}

		return result;
	}

	public void connectSocketIo(final SocketIoConfig socketIoConfig){
	    final String tag="connectSocketIo - ";
        if (socketIoClient==null) {
            socketIoClient=new SocketIoClient(getResources(),this,socketIoConfig);

            // On connect-event, the backend will also send the cached messages
            if (socketIoClient.connect()){
                Log.i(TAG, "Connected to SocketIO server.");
            } else {
                Log.e(TAG, "Not possible to connect to SocketIO server.");
            }
        } else {
            Log.d(TAG,tag+"Using existing SocketIoClient");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (nmeaUdpClientTask!=null && !nmeaUdpClientTask.isCancelled()){
            nmeaUdpClientTask.cancel(true);
            nmeaUdpClientTask=null;
        }

        if (socketIoClient!=null){
            socketIoClient.disconnect();
            socketIoClient=null;
        }

        Analytics.getInstance().logEvent(TAG, "destroy", "");
    }

    private void createRepeaterConfig(){
        final String tag="createRepeaterConfig - ";

        final String repeatHost = SettingsUtils.getInstance().parseFromPreferencesAisMessagesDestinationHost();
        final int repeatPort = SettingsUtils.getInstance().parseFromPreferencesAisMessagesDestinationPort();

        String informText="Not repeating NMEA messages.";
        if (repeatHost!=null && !(repeatHost.equals(NMEA_UDP_HOST) && repeatPort==NMEA_UDP_PORT)) {
            ValidateDatagramSocketConfigTask validateDatagramSocketConfigTask = new ValidateDatagramSocketConfigTask(this,new DatagramSocketConfig(repeatHost,repeatPort));
            validateDatagramSocketConfigTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            informText="IGNORE NMEA repeat setting: Asked to repeat to build in address:port or invalid settings.";
        }

        Log.d(TAG,tag+informText);
    }

    public void onValidatedDatagramSocketConfig(final DatagramSocketConfig datagramSocketConfig){
        if (datagramSocketConfig!=null) {
            datagramSocketConfigRepeater=datagramSocketConfig;

            final boolean hasNetworkConnection=Utils.haveNetworkConnection(this);
            nmeaUdpClientTask = new NmeaUdpClientTask(this,new DatagramSocketConfig(NMEA_UDP_HOST,NMEA_UDP_PORT),socketIoClient,getCacheDir(),hasNetworkConnection);
            nmeaUdpClientTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            //  Toast.makeText(this, "Repeating NMEA messages to UDP: " + datagramSocketConfig, Toast.LENGTH_LONG).show();
            Log.d(TAG, "Repeating on UDP: " + datagramSocketConfigRepeater);
            Analytics.getInstance().logEvent(TAG, "NMEA Repeater", "repeatHost: " + datagramSocketConfig.getAddress() + ", repeatPort: " + datagramSocketConfig.getPort());
        }
    }

    public void requestSocketIoServerCachedMessages(){
        if (socketIoClient!=null) {
            socketIoClient.requestServerCachedMessages();
        }
    }

	public boolean addListener(ShipReceivedListener listener) {
		synchronized(listeners){
			return listeners.add(listener);
		}	
	}
	
	public boolean removeListener(ShipReceivedListener listener) {
		synchronized(listeners){
			return listeners.remove(listener);
		}	
	}

	private void repeatViaUdp(final String nmea){
        final String tag="repeatViaUdp - ";
        if (datagramSocketConfigRepeater != null) {
            try {
                final DatagramSocket serverSocketRepeater = new DatagramSocket();
                final byte[] nmeaAsByteArray=nmea.getBytes();
                final DatagramPacket packet = new DatagramPacket(nmeaAsByteArray, nmeaAsByteArray.length, InetAddress.getByName(datagramSocketConfigRepeater.getAddress()), datagramSocketConfigRepeater.getPort());
                serverSocketRepeater.send(packet);
            } catch (IOException e){
                Log.e(TAG,tag,e);
            }
        }
    }

	private void onNmeaReceived(final String nmea,final Nmea2Ship.NmeaSource nmeaSource) {
		Log.d(TAG,"onNmeaReceived - "+nmea);

		// Convert NMEA to Ship
        final Ship ship = nmea2Ship.onMessage(nmea,nmeaSource);
        if (ship != null && ship.isValid()) {
            switch (nmeaSource) {
                case UDP:
                    mmsiReceivedViaUdp.add(ship.getMmsi());
                    repeatViaUdp(nmea);
                    break;
                case SOCKET_IO:
                    mmsiReceivedViaSocketIo.add(ship.getMmsi());
                    if (relayNmeaFromPeers){
                        repeatViaUdp(nmea);
                    }
                    break;
                default:
                    // Nothing to do
                    break;
            } // End switch

            synchronized (listeners) {
                for (final ShipReceivedListener listener : listeners) {
                    listener.onShipReceived(ship);
                }
            }
        }
	}

	@Override
	public void onNmeaViaUdpReceived(String nmea) {
		onNmeaReceived(nmea,Nmea2Ship.NmeaSource.UDP);
	}

	@Override
	public void onNmeaViaSocketIoReceived(final String nmea) {
		onNmeaReceived(nmea,Nmea2Ship.NmeaSource.SOCKET_IO);
    }

	public class ServiceBinder extends Binder {
		public NmeaClientService getService() {
			return NmeaClientService.this;
		}
	}
}
