package net.videgro.ships.services.internal;

import android.os.AsyncTask;
import android.util.Log;

import net.videgro.ships.Repeater;
import net.videgro.ships.tasks.ProcessCachedMessagesTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NmeaMessagesCache {
    private static final String TAG = "NmeaMessagesCache";

    private final File cacheFile;
    private final Repeater repeater;

    public NmeaMessagesCache(final File cacheDirectory, final Repeater repeater) {
        cacheFile = new File(cacheDirectory, "nmea.cache");
        this.repeater = repeater;
    }

    public void add(final String nmea) {
        final String tag = "cacheDatagramPacket - ";
        try (final FileWriter fw = new FileWriter(cacheFile, true); final BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(nmea + "\n");
        } catch (IOException e) {
            Log.e(TAG, tag, e);
        }
    }

    public void processCachedMessages() {
        // Contains network actions, not allowed on main thread: so execute it as task.
        final ProcessCachedMessagesTask task = new ProcessCachedMessagesTask(cacheFile, repeater);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
