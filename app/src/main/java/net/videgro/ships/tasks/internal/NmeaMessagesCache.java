package net.videgro.ships.tasks.internal;

import android.util.Log;

import net.videgro.ships.services.internal.SocketIoClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class NmeaMessagesCache {
    private static final String TAG = "NmeaMessagesCache";

    private final File cacheNmeaFile;
    private final SocketIoClient socketIoClient;

    public NmeaMessagesCache(final File cacheDirectory,final SocketIoClient socketIoClient){
        cacheNmeaFile = new File(cacheDirectory, "data.nmea");
        this.socketIoClient=socketIoClient;
    }

    public void cacheMessage(final String line) {
        final String tag = "cacheMessage - ";
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(cacheNmeaFile, true);
            bw = new BufferedWriter(fw);
            bw.write(line + "\n");
        } catch (IOException e) {
            Log.e(TAG, tag, e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    Log.e(TAG, tag + "While closing BufferedWriter.", e);
                }
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    Log.e(TAG, tag + "While closing FileWriter.", e);
                }
            }
        }
    }

    public boolean processCachedMessages() {
        final String tag = "processCachedMessages - ";
        boolean result = true;
        if (cacheNmeaFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(cacheNmeaFile));
                String line;

                while ((line = reader.readLine()) != null && (result)) {
                    result = socketIoClient.repeatToSocketIoServer(line);
                }
            } catch (IOException e) {
                Log.e(TAG, tag, e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, tag + "While closing reader.", e);
                    }
                }

                //noinspection ResultOfMethodCallIgnored
                cacheNmeaFile.delete();
            }
        }
        return result;
    }
}
