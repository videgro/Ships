package net.videgro.ships.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

import android.os.Environment;
import android.util.Log;
import fi.iki.elonen.NanoHTTPD;

public class HttpCachingTileServer extends NanoHTTPD {
	private static final String TAG = "HttpCachingTileServer - ";

	private static final String DIRECTORY_TILES_CACHE = "map_tiles_cache";
	private static final int MAX_AGE = 1000 * 60 * 60 * 24 * 30; // 30 days
	private static final int MIN_FREE_BYTES = 1024 * 1024 * 100; // 100 megabytes

	private static final String[] ALLOWED_URLS = { "openstreetmap", "openseamap" };
	
	private static HttpCachingTileServer instance;
	private boolean running=false;
	
	public static HttpCachingTileServer getInstance(){
		if (instance==null){
			instance=new HttpCachingTileServer();
		}
		return instance;
	}
	
	private HttpCachingTileServer() {
		super(8181);		
	}
	
	public void startServer(){
		if (!running){
			try {
				start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
				Log.i(TAG, "Running! Point your browser to http://localhost:8181/ \n");
			} catch (IOException e) {
				Log.e(TAG, "startServer", e);
			}
			createCacheDir();
			running=true;
		}
	}

	private void createCacheDir() {
		final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_TILES_CACHE);
		Log.i(TAG, "Cache dir: " + file.getAbsolutePath());
		if (!file.mkdirs()) {
			Log.w(TAG, "Directory not created");
		}
	}

	private File getImage(final String urlAsString) {
		final String tag = "getImage";
		HttpURLConnection connection = null;
		InputStream inputStream = null;
		OutputStream outputStream = null;

		File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_TILES_CACHE + "/" + urlAsString.replace(":", "").replace("/", "_"));

		if (file.exists() && ((Calendar.getInstance().getTimeInMillis() - file.lastModified()) < MAX_AGE)) {
			Log.i(TAG, "File: " + file.getAbsolutePath() + " exists, serve file from cache.");
		} else {
			// Log.i(TAG,"Downloading file: "+file.getAbsolutePath()+" from URL: "+urlAsString);
			try {
				URL url = new URL(urlAsString);
				connection = (HttpURLConnection) url.openConnection();
				// connection.setRequestProperty("User-Agent", "");
				// connection.setRequestMethod("POST");
				// connection.setDoInput(true);
				connection.connect();

				inputStream = connection.getInputStream();
				outputStream = new FileOutputStream(file);
				byte[] buf = new byte[512];
				int num;
				while ((num = inputStream.read(buf)) != -1) {
					outputStream.write(buf, 0, num);
				}
			} catch (MalformedURLException e) {
				Log.e(TAG, tag, e);

			} catch (IOException e) {
				Log.e(TAG, tag, e);
			} finally {
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (IOException e) {
						Log.e(TAG, tag, e);
					}
				}
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						Log.e(TAG, tag, e);
					}
				}
				if (connection != null) {
					connection.disconnect();
				}
			}
		}

		return file;
	}

	private boolean isAllowed(final String url) {
		boolean ok = false;
		for (int i = 0; i < ALLOWED_URLS.length && !ok; i++) {
			if (url.contains(ALLOWED_URLS[i])) {
				ok = true;
			}
		}
		return ok;
	}

	@Override
	public Response serve(IHTTPSession session) {
		final String tag = "serve - ";

		Response res = null;

		final String url = "http:/" + session.getUri(); // URI starts with /
		if (isAllowed(url)) {

			if (isExternalStorageWritable()) {
				if (isEnoughDiskSpace()) {
					File imageFile = getImage(url);

					try {
						res = newFixedLengthResponse(Response.Status.OK, "image/png", new FileInputStream(imageFile), (int) imageFile.length());
						res.addHeader("Accept-Ranges", "bytes");
						
						res.addHeader("Access-Control-Allow-Methods", "DELETE, GET, POST, PUT");						
			            res.addHeader("Access-Control-Allow-Origin",  "*");
			            res.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
					} catch (FileNotFoundException e) {
						Log.e(TAG, tag, e);
					}
				} else {
					Log.e(TAG, tag + "Not enough disk space available.");
				}
			} else {
				Log.e(TAG, tag + "Not possible to use external storage.");
			}
		} else {
			Log.e(TAG, tag + "URL is not allowed.");
		}

		return res;
	}

	public int cleanup() {
		int deletedFiles = 0;
		File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_TILES_CACHE);
		if (folder != null) {
			final File[] files = folder.listFiles();
			if (files != null) {
				for (final File fileEntry : folder.listFiles()) {
					if (!fileEntry.isDirectory() && ((Calendar.getInstance().getTimeInMillis() - fileEntry.lastModified()) > MAX_AGE)) {
						if (fileEntry.delete()) {
							deletedFiles++;
						}
					}
				}
			}
		}
		return deletedFiles;
	}

	private boolean isEnoughDiskSpace() {
		final long freeBytes = new File(Environment.getExternalStorageDirectory().toString()).getFreeSpace();
		return freeBytes > MIN_FREE_BYTES;
	}

	/* Checks if external storage is available for read and write */
	private boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}

}
