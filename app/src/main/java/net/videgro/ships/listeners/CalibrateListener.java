package net.videgro.ships.listeners;

public interface CalibrateListener {
	boolean onTryPpm(boolean firstTry,int percentage,int ppm);
	void onCalibrateReady(int ppm);	
	void onCalibrateFailed();
	void onCalibrateCancelled();
}
