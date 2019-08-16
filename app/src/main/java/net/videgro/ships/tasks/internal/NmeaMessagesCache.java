package net.videgro.ships.tasks.internal;

import android.util.Log;

import net.videgro.ships.Repeater;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class NmeaMessagesCache {
    private static final String TAG = "NmeaMessagesCache";

    private final File cacheFile;
    private final Repeater repeater;

    public NmeaMessagesCache(final File cacheDirectory, final Repeater repeater){
        cacheFile = new File(cacheDirectory, "nmea.cache");
        this.repeater=repeater;
    }

    public void add(final String nmea) {
        final String tag = "cacheDatagramPacket - ";
        try (final FileWriter fw = new FileWriter(cacheFile, true);final BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(nmea + "\n");
        } catch (IOException e) {
            Log.e(TAG, tag, e);
        }
    }

    public int processCachedMessages() {
        final String tag = "processCachedMessages - ";

        int numLines=0;

        if (cacheFile.exists()) {
            try (final BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    repeater.repeat(line);
                    numLines++;
                }
            } catch (IOException e) {
                Log.e(TAG, tag, e);
            } finally {
                //noinspection ResultOfMethodCallIgnored
                cacheFile.delete();
            }
        }
        return numLines;
    }
}
