package net.videgro.ships.fragments.internal;

public class OpenDeviceResult {
	public static final String TAG="OpenDeviceResult";

	private String message;
	private String deviceDescription;
	private int errorReason;

    // Package protected
	OpenDeviceResult(String message, String deviceDescription, int errorReason) {
		this.message = message;
		this.deviceDescription = deviceDescription;
		this.errorReason = errorReason;
	}
	
	public String getMessage() {
		return message;
	}
	public String getDeviceDescription() {
		return deviceDescription;
	}
	public int getErrorReason() {
		return errorReason;
	}

	@Override
	public String toString() {
		String result=message;		
		if (deviceDescription!=null && !deviceDescription.isEmpty()){
			result+=" Device: "+deviceDescription;
		}
		return result;		
	}
}
