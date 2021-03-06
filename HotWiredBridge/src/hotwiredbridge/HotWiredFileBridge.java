package hotwiredbridge;

import hotwiredbridge.hotline.HotlineUtils;
import hotwiredbridge.hotline.MacRoman;
import wired.WiredTransferClient;
import wired.WiredUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
	private KeyStoreProvider ksp;
	private FileTypeAndCreatorMap fileTypeAndCreatorMap;
	private FileTransferMap fileTransferMap;
	private DataInputStream in;
	private DataOutputStream out;

	private boolean closed;

	public HotWiredFileBridge(WiredServerConfig config, KeyStoreProvider ksp,
			FileTypeAndCreatorMap fileTypeAndCreatorMap,
			FileTransferMap fileTransferMap, InputStream in, OutputStream out) {
		this.config = config;
		this.ksp = ksp;
		this.fileTypeAndCreatorMap = fileTypeAndCreatorMap;
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

	// TODO: Use this!
	private static String readString(DataInputStream in) throws IOException {
		int length = in.readUnsignedShort();
		byte[] data = new byte[length];
		in.readFully(data);
		return MacRoman.toString(data);
	}
	
	private void writeFileData(byte[] checksumBuf, int length, SSLSocket socket, String wiredTransferId) throws IOException {
		WiredTransferClient client = new WiredTransferClient(socket.getOutputStream());
		client.identifyTransfer(wiredTransferId);
		OutputStream outgoing = socket.getOutputStream();
		outgoing.write(checksumBuf);
		length -= checksumBuf.length;
		if (length > 0) {
			byte[] buf = new byte[64 * 1024];
			while (length > 0) {
				int n = (length > buf.length ? buf.length : length);
				int nread = in.read(buf, 0, n);
				if (nread <= 0) {
					break;
				}
				outgoing.write(buf, 0, nread);
				length -= nread;
			}
		}
		outgoing.flush();
		outgoing.close();
		socket.close();
	}
	
	private void uploadFileToWired(FileTransfer transfer, int length) throws Exception {
		String checksum = transfer.getWiredChecksum();
		int checksumSize = 0;
		if (checksum == null) {
			checksumSize = (1024 * 1024 > length ? length : 1024 * 1024);
		}
		byte[] checksumBuf = new byte[checksumSize];
		if (checksumSize > 0) {
			in.readFully(checksumBuf);
			checksum = WiredUtils.SHA1(checksumBuf);
		}
		String path = transfer.getFileInfo().getPath();
		// We have enough info to send a putFile() to the wired server.
		transfer.getHotWiredBridge().getWiredClient().putFile(path, length, checksum);
		// Wait for wired to reply and tell us to proceed with the upload.
		transfer = fileTransferMap.waitForWiredTransferIdForUploadTransfer(transfer);
		SSLSocket socket = openWiredSocket(config.getHost(), config.getPort() + 1);
		writeFileData(checksumBuf, length, socket, transfer.getWiredTransferId());
	}
	
	private void receiveFileFromHotline(FileTransfer transfer) throws Exception {
		while (in.available() > 0) {
			String tag = HotlineUtils.readTag(in);
			if (tag.equals("FILP")) {				
				byte[] header = new byte[20];
				in.readFully(header);
			} else if (tag.equals("INFO")) {
				in.readInt();
				in.readInt();
				int length = in.readInt();
				byte[] info = new byte[length];
				in.readFully(info);
			} else if (tag.equals("DATA")) {
				in.readInt();
				in.readInt();
				int length = in.readInt();
				uploadFileToWired(transfer, length);
			} else if (tag.equals("MACR")) {
				in.readInt();
				in.readInt();
				int length = in.readInt();
				byte[] buf = new byte[64 * 1024];
				while (length > 0) {
					int n = (length > buf.length ? buf.length : length);
					int nread = in.read(buf, 0, n);
					if (nread <= 0) {
						break;
					}
					// discard!
					// out.write(buf, 0, nread);
					length -= nread;
				}
			} else {
				// unknown
			}
		}
		
	}

	private void sendFileToHotline(FileTransfer transfer) throws Exception {
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
		String[] codes = fileTypeAndCreatorMap.getCodesFor(transfer.getFileInfo().getName());
		out.write(codes[FileTypeAndCreatorMap.TYPE].getBytes());
		out.write(codes[FileTypeAndCreatorMap.CREATOR].getBytes());
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
		out.write(HotlineUtils.pack("D", transfer.getFileInfo().getCreationDate()));
		out.write(HotlineUtils.pack("D", transfer.getFileInfo().getModificationDate()));
		out.writeShort(0);
		out.writeShort(filename.length);
		out.write(filename);
		out.writeShort(comment.length);
		out.write(comment);
		out.write("DATA".getBytes());
		out.writeInt(0);
		out.writeInt(0);
		out.writeInt((int)(transfer.getFileInfo().getSize() - transfer.getFileOffset()));
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
	}

	private boolean performHandshake() {
		byte[] handshake = new byte[4];
		try {
			in.readFully(handshake);
			if (!new String(handshake).equals("HTXF")) {
				return false;
			}
			int hotlineTransferId = in.readInt();
			int value = in.readInt();
			in.readInt();
			if (value == 0) {
				FileTransfer transfer = fileTransferMap.getTransferByHotlineId(hotlineTransferId);
				sendFileToHotline(transfer);
			} else {
				FileTransfer transfer = fileTransferMap.getTransferByHotlineId(hotlineTransferId);
				receiveFileFromHotline(transfer);
			}
			in.close();
			out.flush();
			out.close();
		} catch (IOException e) {
			return false;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	public SSLSocket openWiredSocket(String host, int port) throws Exception {
		KeyStore ks = ksp.getKeyStore();
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
