package net.videgro.ships.tasks;

import android.os.AsyncTask;
import android.util.Log;

import net.videgro.ships.MyFirebaseMessagingRepeater;
import net.videgro.ships.listeners.NmeaReceivedListener;
import net.videgro.ships.services.NmeaClientService;

import java.util.regex.Pattern;

public class NmeaFirebaseMessagingClientTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "NmeaFirebaseMsgCltTsk";

    private final NmeaReceivedListener listener;
    private final String nmeasRaw;


    public NmeaFirebaseMessagingClientTask(final NmeaReceivedListener listener, final String nmeasRaw) {
        this.listener = listener;
        this.nmeasRaw = nmeasRaw;
    }

    public String doInBackground(Void... params) {
        final String tag = "doInBackground - ";
        Thread.currentThread().setName(TAG);

        if (nmeasRaw != null) {
            final String[] nmeas = nmeasRaw.split(Pattern.quote(MyFirebaseMessagingRepeater.NMEA_SEP));
            for (final String nmea : nmeas) {
                if (!nmea.isEmpty()) {
                    listener.onNmeaReceived(MyFirebaseMessagingRepeater.PREFIX_AIVDM + nmea, NmeaClientService.Source.CLOUD);
                }
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
