package net.videgro.ships;

public class StartRtlSdrRequest {
	final String args;
	final int fd;
	final String uspfsPath;

	public StartRtlSdrRequest(String args, int fd, String uspfsPath) {
		this.args = args;
		this.fd = fd;
		this.uspfsPath = uspfsPath;
	}

	public String getArgs() {
		return args;
	}

	public int getFd() {
		return fd;
	}

	public String getUspfsPath() {
		return uspfsPath;
	}

	@Override
	public String toString() {
		return "StartRtlSdrRequest [args=" + args + ", fd=" + fd + ", uspfsPath=" + uspfsPath + "]";
	}	
}
