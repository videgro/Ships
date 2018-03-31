package net.videgro.ships.tasks;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Callable;

import android.util.Log;

public class HttpCacheFifoTask implements Callable<String> {
	private static final String TAG = "HttpCacheFifoTask";

	private final File cacheDir;
	private final long maxDiskUsageInBytes;

	private long totalDeletedBytes = 0;
	private int totalDeletedFiles = 0;

	public HttpCacheFifoTask(final File cacheDir, final long maxDiskUsageInBytes) {
		this.cacheDir = cacheDir;
		this.maxDiskUsageInBytes = maxDiskUsageInBytes;
	}

	@Override
	public String call() {
		
		// Get the size of the directory
		long dirSize = retrieveDirSize(cacheDir);
		
		if (dirSize > maxDiskUsageInBytes){
			
			// Get a orderded (by last modified) list of files
			final File[] files = cacheDir.listFiles();
			if (files != null && files.length > 0) {
				Arrays.sort(files, new Comparator<File>() {
					public int compare(File f1, File f2) {
						return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
					}
				});
			}
						
			int victimId=0;

			while (files!=null && dirSize > maxDiskUsageInBytes && victimId < files.length) {
				final File victim = files[victimId++];
				final long fileSize = victim.length();
				//Log.d(TAG,"Deleting file: "+victim+", Last modified: "+victim.lastModified()+", Size: "+fileSize+" bytes.");

				if (victim.delete()) {
					dirSize -= fileSize;
					totalDeletedBytes += fileSize;
					totalDeletedFiles++;
				}
			}
		}
		
		final String result="dirSize: "+dirSize+", maxDiskUsageInBytes: "+maxDiskUsageInBytes+", totalDeletedBytes: "+totalDeletedBytes+", totalDeletedFiles: "+totalDeletedFiles;
		Log.d(TAG,result);
		return result;
	}

	private long retrieveDirSize(final File dir) {
		long size = 0;
		final File[] files = dir.listFiles();
		if (files != null) {
			for (final File file : files) {
				if (file.isFile()) {
					size += file.length();
				} else {
					size += retrieveDirSize(file);
				}
			}
		}
		return size;
	}
}
