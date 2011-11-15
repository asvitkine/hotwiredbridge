package hotwiredbridge;

import sun.misc.*;

public class SignalSuppressor implements SignalHandler {
	public static final String SIGTERM = "TERM";

	private String signalName;

	public SignalSuppressor(String signalName) {
		this.signalName = signalName;
	}

	public String getSignalName() {
		return signalName;
	}

	public void installHandler() {
		Signal.handle(new Signal(signalName), this);
	}

	@Override
	public void handle(Signal sig) {
		System.out.println("INFO: Received signal " + sig + ". Ignoring.");		
	}
}
