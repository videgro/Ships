package net.videgro.ships.tasks;

import android.os.AsyncTask;
import android.util.Log;

import net.videgro.ships.tasks.domain.DatagramSocketConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ValidateDatagramSocketConfigTask extends AsyncTask<Void, Void, DatagramSocketConfig> {
    private static final String TAG = "ValidateDatagramSocketConfigTask";

    private final ValidateDatagramSocketConfigListener listener;
    private final DatagramSocketConfig datagramSocketConfig;

    public ValidateDatagramSocketConfigTask(final ValidateDatagramSocketConfigListener listener,final DatagramSocketConfig datagramSocketConfig) {
        this.listener = listener;
        this.datagramSocketConfig=datagramSocketConfig;
    }

    public DatagramSocketConfig doInBackground(Void... params) {
        Thread.currentThread().setName(TAG);
        return validateDatagramSocketConfig() ? datagramSocketConfig : null;
    }

    private boolean validateDatagramSocketConfig() {
        final String tag = "validateDatagramSocketConfig - ";

        boolean result = false;

        if (datagramSocketConfig!=null && datagramSocketConfig.getPort() > 0){
            final String address=datagramSocketConfig.getAddress();
            if (address != null && !address.isEmpty()) {
                try {
                    // Ignore result, only interested whether an UnknownHostException is thrown.
                    //noinspection ResultOfMethodCallIgnored
                    InetAddress.getByName(address);
                    result = true;
                } catch (UnknownHostException e) {
                    Log.w(TAG, tag + "Invalid host: "+address, e);
                }
            }
        }
        return result;
    }

    public void onPostExecute(DatagramSocketConfig result) {
        listener.onValidatedDatagramSocketConfig(result);
    }

    public interface ValidateDatagramSocketConfigListener {
        void onValidatedDatagramSocketConfig(DatagramSocketConfig result);
    }
}
