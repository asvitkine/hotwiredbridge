package hotwiredbridge;

import hotwiredbridge.hotline.MacRoman;
import hotwiredbridge.wired.WiredTransferClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

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
		byte[] handshake = new byte[4];
		try {
			in.readFully(handshake);
			if (!new String(handshake).equals("HTXF")) {
				return false;
			}
			int hotlineTransferId = in.readInt();
			System.out.println("request for " + hotlineTransferId);
			// note: wired issues a textual key string
			// so we need to keep a map somewhere
			// between the two
			int value = in.readInt();
			in.readInt();
			if (value == 0) {
				FileTransfer transfer = fileTransferMap.getTransferByHotlineId(hotlineTransferId);
				SSLSocket socket = openWiredSocket(config.getHost(), config.getPort() + 1);
				WiredTransferClient client = new WiredTransferClient(socket.getOutputStream());
				client.identifyTransfer(transfer.getWiredTransferId());
				out.write("FILP".getBytes());
				out.writeShort(1);
				out.writeShort(0);
				out.writeInt(0);
				out.writeInt(0);
				out.writeInt(0);
				out.writeInt(3);
				out.write("INFO".getBytes());
				out.writeInt(0);
				out.writeInt(0);
				byte[] filename = MacRoman.fromString(transfer.getFileInfo().getName());
				byte[] comment = new byte[0]; // todo
				out.writeInt(74 + filename.length + comment.length);
				out.write("AMAC".getBytes());
				out.write("TEXT".getBytes());
				out.write("ttxt".getBytes());
				out.writeInt(0);
				out.writeInt(256);
				out.writeInt(0);
				out.writeInt(0);
				out.writeInt(0);
				out.writeInt(0);
				out.writeInt(0);
				out.writeInt(0);
				out.writeInt(0);
				out.writeInt(0);
				out.write(new byte[] {(byte)0x07, (byte)0x70, (byte)0x00, (byte)0x00,(byte) 0xAF,(byte) 0xAE, 0x17, 0x23});
				out.write(new byte[] {(byte)0x07, (byte)0x70, (byte)0x00, (byte)0x00,(byte) 0xAF,(byte) 0xAE, 0x17, 0x23});
				out.writeShort(0);
				out.writeShort(filename.length);
				out.write(filename);
				out.writeShort(comment.length);
				out.write(comment);
				out.write("DATA".getBytes());
				out.writeInt(0);
				out.writeInt(0);
				out.writeInt((int)transfer.getFileInfo().getSize());
				InputStream incoming = socket.getInputStream();
				byte[] buf = new byte[64 * 1024];
				int length = incoming.read(buf);
				while (length > 0) {
					out.write(buf, 0, length);
					length = incoming.read(buf);
				}
				out.write("MACR".getBytes());
				out.writeInt(0);
				out.writeInt(0);
				out.flush();
				out.close();
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
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	public SSLSocket openWiredSocket(String host, int port) throws Exception {
		File certificatesFile = new File("/Users/shadowknight/Projects/ventcore/ssl_certs");
		InputStream in = new FileInputStream(certificatesFile);
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(in, "changeit".toCharArray());
		in.close();

		SSLContext context = SSLContext.getInstance("TLS");
		String algorithm = TrustManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
		tmf.init(ks);
		context.init(null, new TrustManager[] {tmf.getTrustManagers()[0]}, null);
		SSLSocketFactory factory = context.getSocketFactory();
		SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
		socket.setEnabledProtocols(new String[] {"TLSv1"});
		return socket;
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
