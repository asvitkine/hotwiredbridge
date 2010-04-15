package hotwiredbridge.wired;

import java.io.IOException;
import java.io.OutputStream;

public class WiredTransferClient extends WiredClientBase {

	public WiredTransferClient(OutputStream out) {
		super(out);
	}

	public synchronized void identifyTransfer(String hash) throws IOException {
		send("TRANSFER").send(SP).send(hash).send(EOT);
	}
}
