package net.videgro.ships.tools;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.util.Log;

public class PermissionsHelper{
	private static final String TAG="PermissionsHelper";
	
    private static final String COMMAND_CHMOD="chmod ";
    private static final String COMMAND_SU="su";

    private static final String NEWLINE="\n";
    
    private void chmodRecursive(final String dir,final String permissions, final OutputStreamWriter outputStreamWriter) throws IOException {
    	// Directory
        outputStreamWriter.write(COMMAND_CHMOD+permissions+" "+dir+NEWLINE);
        outputStreamWriter.flush();

        final String[] files = new File(dir).list();
        if (files != null) {        
	        for (final String file : files) {
	            final String fileName = dir + file;
	            final File f = new File(fileName);
	
	            if (f.isDirectory()){
	                chmodRecursive(fileName+"/",permissions,outputStreamWriter);
	            } else {
	            	// File
	                outputStreamWriter.write(COMMAND_CHMOD+permissions+" "+fileName+NEWLINE);
	                outputStreamWriter.flush();
	            }
	        }
        }
    }

    public boolean changePermissions(final String path,final String permissions) {
    	final String tag="changePermissions - ";
    	
    	boolean result=false;
    	
        Runtime runtime = Runtime.getRuntime();
        OutputStreamWriter outputStreamWriter = null;
        Process process = null;
        try {
            process = runtime.exec(COMMAND_SU);
            outputStreamWriter = new OutputStreamWriter(process.getOutputStream());
            chmodRecursive(path+"/",permissions,outputStreamWriter);
            outputStreamWriter.close();
            result=true;
        } catch (IOException e) {
        	Log.v(TAG,tag,e);
            Log.w(TAG,tag+"Not possible to change permissions on: "+path+", to: "+permissions);
        } finally {
            if (outputStreamWriter != null) {
                try {
                    outputStreamWriter.close();
                } catch (IOException e) {
                	Log.e(TAG,tag,e);
                }
            }
        }
        try {
            if (process != null){
                result=process.waitFor() == 0;                
            }
        } catch (InterruptedException e) {
        	Log.e(TAG,tag,e);
        }        
        
        return result;
    }
	
}