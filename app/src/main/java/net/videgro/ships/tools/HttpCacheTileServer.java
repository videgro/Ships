package net.videgro.ships.tools;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import net.videgro.ships.tasks.HttpCacheFifoTask;

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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import fi.iki.elonen.NanoHTTPD;

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

	private File dirCache=null;

	public static HttpCacheTileServer getInstance(){
		if (instance==null){
			instance=new HttpCacheTileServer();
		}
		return instance;
	}
	
	private HttpCacheTileServer() {
		super(8181);		
	}

    public void init(final Context context,final long maxDiskUsageInBytes){
        this.maxDiskUsageInBytes=maxDiskUsageInBytes;
		retrieveCacheDir(context);
    }

	private void retrieveCacheDir(final Context context) {
		final String tag="retrieveCacheDir - ";
		dirCache = null;
		if (isExternalStorageWritable()){
			final File dir=new File(Objects.requireNonNull(context).getExternalFilesDir(Environment.DIRECTORY_PICTURES),DIRECTORY_TILES_CACHE);
			if (!dir.exists()) {
				try {
					if (!dir.mkdirs()){
						Log.w(TAG,tag+"Not possible to create directory: "+ dir.getPath());
					} else {
						dirCache=dir;
					}
				} catch (SecurityException e){
					Log.w(TAG,tag+"Not possible to create directory: "+ dir.getPath(),e);
				}
			} else if (dir.getFreeSpace()<MIN_FREE_BYTES){
				Log.w(TAG,tag+"Not enough free space on external files dir. Minimal required: "+MIN_FREE_BYTES+" bytes.");
			} else {
				dirCache=dir;
			}
		}

		if (dirCache==null){
			Log.w(TAG,tag+"No external files directory available, use cache directory.");
			dirCache = context.getCacheDir();
		}
	}

	/**
	 * Start the caching tiles server
	 * @return TRUE when server is available
	 */
	public boolean startServer(){
        final String tag="startServer - ";
		boolean result=false;

		if (!running){
            final int cleanup = cleanupOldFiles();
            Log.i(TAG, tag + "Deleted: " + cleanup + " files from caching tile server.");

            try {
				start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
				Log.i(TAG,tag+"Running! Point your browser to http://localhost:8181/ \n");
			} catch (IOException e) {
				Log.e(TAG,tag, e);
			}
			running=true;
			result=true;
		} else {
			result=true;
		}
		return result;
	}

	private File getImage(final String urlAsString) {
		final String tag = "getImage - ";
		HttpURLConnection connection = null;
		InputStream inputStream = null;
		OutputStream outputStream = null;

        File file = null;

        if (dirCache!=null) {
            file = new File(dirCache, "/" + urlAsString.replace(":", "").replace("/", "_"));

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
        } else {
            Log.e(TAG,tag+"No cache directory available.");
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

		// Just one slash, URI starts with slash too
		final String url = "https:/" + session.getUri(); // URI starts with /
		if (isAllowed(url)) {
            final File imageFile = getImage(url);
            if (imageFile!=null && imageFile.exists()){
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
			Log.e(TAG, tag + "URL is not allowed.");
		}
		
		if (res==null){
			notFoundCount++;
			res=newFixedLengthResponse(Response.Status.NOT_FOUND,NanoHTTPD.MIME_PLAINTEXT,"Error 404, file not found.");			
		}

		return res;
	}

	private int cleanupOldFiles() {
		int deletedFiles = 0;
		final long now = Calendar.getInstance().getTimeInMillis();
        Log.d(TAG,"Cleaning directory: "+dirCache);
		if (dirCache != null) {
			final File[] files = dirCache.listFiles();
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

	/* Checks if external storage is available for read and write */
	private boolean isExternalStorageWritable() {
		final String state = Environment.getExternalStorageState();
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
			httpCacheFifoTask = new FutureTask<>(new HttpCacheFifoTask(dirCache,maxDiskUsageInBytes));
			executor.execute(httpCacheFifoTask);
		}
	}
}
