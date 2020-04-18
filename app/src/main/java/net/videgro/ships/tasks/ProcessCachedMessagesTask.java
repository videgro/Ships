package net.videgro.ships.tasks;

import android.os.AsyncTask;
import android.util.Log;

import net.videgro.ships.Analytics;
import net.videgro.ships.Repeater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ProcessCachedMessagesTask extends AsyncTask<Void, Void, Integer> {
	private static final String TAG = "ProcessCachedMsgTask";

	private final File cacheFile;
	private final Repeater repeater;

	public ProcessCachedMessagesTask(final File cacheFile,final Repeater repeater) {
		this.cacheFile = cacheFile;
		this.repeater = repeater;
	}

	public Integer doInBackground(Void... params) {
		final String tag = "doInBackground - ";
		Thread.currentThread().setName(TAG);

		int numLines=0;

		if (cacheFile.exists()) {
			try (final BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
				String line;

				while ((line = reader.readLine()) != null) {
					repeater.repeatViaUdp(line);
					repeater.repeatToCloud(line);
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

	public void onPostExecute(Integer result) {
		if (result != null) {
			Log.d(TAG, "Result: " + result);
			Analytics.logEvent(repeater.getContext(),Analytics.CATEGORY_NMEA_REPEAT, "Numer_of_processed_cached_messages",String.valueOf(result));
		}
	}
}
