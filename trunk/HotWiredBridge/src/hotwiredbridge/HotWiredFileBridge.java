package hotwiredbridge;

import hotwiredbridge.hotline.HotlineUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HotWiredFileBridge {
	private WiredServerConfig config;
	private FileTransferMap fileTransferMap;
	private DataInputStream in;
	private DataOutputStream out;

	private boolean closed;

	public HotWiredFileBridge(WiredServerConfig config, FileTransferMap fileTransferMap, InputStream in, OutputStream out) {
		this.config = config;
		this.fileTransferMap = fileTransferMap;
		this.in = new DataInputStream(in);
		this.out = new DataOutputStream(out);
	}

	public void run() {
		if (!performHandshake()) {
			close();
			return;
		}
	}

	private boolean performHandshake() {
		byte[] handshake = new byte[8];
		try {
			in.readFully(handshake);
			if (!new String(handshake).equals("HTXF")) {
				return false;
			}
			int hotlineTransferId = in.readInt();
			// note: wired issues a textual key string
			// so we need to keep a map somewhere
			// between the two
			int value = in.readInt();
			in.readInt();
			if (value == 0) {
				// client wants to download file
				// validate
				// connect_to_wired
				// send_it
			} else {
				// client is uploading file of size value
				// validate
				// connect_to_wired
				// get_it
			}
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	
	private synchronized void close() {
		if (!closed) {
			/*if (wiredSocket != null) {
				try { wiredSocket.close(); } catch (Exception e) {}
				wiredSocket = null;
			}*/
			if (in != null) {
				try { in.close(); } catch (Exception e) {}
				in = null;
			}
			if (out != null) {
				try { out.close(); } catch (Exception e) {}
				out = null;
			}
			closed = true;
		}
	}
}
