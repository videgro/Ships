package net.videgro.ships.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import android.os.Environment;
import android.util.Log;
import fi.iki.elonen.NanoHTTPD;
import net.videgro.ships.tasks.HttpCacheFifoTask;

public class HttpCacheTileServer extends NanoHTTPD {
	private static final String TAG = "HttpCacheTileServer - ";

	private static final String DIRECTORY_TILES_CACHE = "map_tiles_cache";
	private static final long MAX_AGE = 1000L * 60 * 60 * 24 * 30; // 30 days
	private static final long MIN_FREE_BYTES = 1024L * 1024 * 100; // 100 megabytes

	private static final String[] ALLOWED_URLS = { "openstreetmap", "openseamap" };
	
	private static HttpCacheTileServer instance;
	private ExecutorService executor = Executors.newFixedThreadPool(1);
	private boolean running=false;
	private FutureTask<String> httpCacheFifoTask=null;
	
	private long maxDiskUsageInBytes;
	
	private int hitCount=0;
	private int networkCount=0;
	private int requestCount=0;
	private int notFoundCount=0;
	
	public static HttpCacheTileServer getInstance(){
		if (instance==null){
			instance=new HttpCacheTileServer();
		}
		return instance;
	}
	
	private HttpCacheTileServer() {
		super(8181);		
	}
	
	public boolean startServer(final long maxDiskUsageInBytes){
		boolean result=false;
		if (!running){
			this.maxDiskUsageInBytes=maxDiskUsageInBytes;
			try {
				start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
				Log.i(TAG, "Running! Point your browser to http://localhost:8181/ \n");
			} catch (IOException e) {
				Log.e(TAG, "startServer", e);
			}
			createCacheDir();
			running=true;
			result=true;
		}
		return result;
	}

	private void createCacheDir() {
		final File file = retrieveCacheDir();
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

		final File file = new File(retrieveCacheDir(),"/" + urlAsString.replace(":", "").replace("/", "_"));

		if (file.exists() && ((Calendar.getInstance().getTimeInMillis() - file.lastModified()) < MAX_AGE)) {
			//Log.i(TAG, "File: " + file.getAbsolutePath() + " exists, serve file from cache.");
			hitCount++;
		} else {
			//Log.i(TAG,"Downloading file: "+file.getAbsolutePath()+" from URL: "+urlAsString);
			networkCount++;
			try {
				final URL url = new URL(urlAsString);
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
				applyFifo();
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
		
		requestCount++;

		final String url = "http:/" + session.getUri(); // URI starts with /
		if (isAllowed(url)) {
			if (isExternalStorageWritable()) {
				if (isEnoughDiskSpace()) {
					final File imageFile = getImage(url);
					if (imageFile.exists()){
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
						Log.e(TAG, tag + "Image file does not exist ("+imageFile+").");
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
		
		if (res==null){
			notFoundCount++;
			res=newFixedLengthResponse(Response.Status.NOT_FOUND,NanoHTTPD.MIME_PLAINTEXT,"Error 404, file not found.");			
		}

		return res;
	}
	
	private File retrieveCacheDir(){
		return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_TILES_CACHE);
	}
	
	public int cleanupOldFiles() {
		int deletedFiles = 0;
		final long now = Calendar.getInstance().getTimeInMillis();
		final File folder = retrieveCacheDir();
		if (folder != null) {
			final File[] files = folder.listFiles();
			if (files != null) {
				for (final File fileEntry : files) {
					final long age=now-fileEntry.lastModified();
					if (!fileEntry.isDirectory() && (age > MAX_AGE)) {
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
		
	/**
	 * Returns the number of HTTP requests whose response was provided by the cache.
	 */
 	public int getHitCount(){
 		return hitCount;
 	}

 	/**
 	 * Returns the number of HTTP requests that required the network to either supply a response or validate a locally cached response.
 	 */
	public int getNetworkCount(){
		return networkCount;
	}

	/**
	 * Returns the total number of HTTP requests that were made. 
	 */
	public int getRequestCount(){
		return requestCount;
	}
	
	/**
	 * Return the number of files which were not found.	 
	 */
	public int getNotFoundCount() {
		return notFoundCount;
	}

	public String getStatistics(){
		return "Number of requests made: "+requestCount+", Number of responses from cache: "+hitCount+", Number of network requests: "+networkCount+", Number of not found files: "+notFoundCount+".";
	}
	
	private void applyFifo(){
		if (httpCacheFifoTask==null || httpCacheFifoTask.isDone() || httpCacheFifoTask.isCancelled()){
			// Just cue one at a time
			httpCacheFifoTask = new FutureTask<String>(new HttpCacheFifoTask(retrieveCacheDir(),maxDiskUsageInBytes));
			executor.execute(httpCacheFifoTask);
		}
	}
}
