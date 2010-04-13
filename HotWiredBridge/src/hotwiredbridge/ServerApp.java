package hotwiredbridge;

import hotwiredbridge.hotline.IconDatabase;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class ServerApp {
	public static void main(String[] args) throws IOException {
		WiredServerConfig config = new WiredServerConfig("localhost", 2000);

		System.out.println("INFO: Starting server...");

		IconDatabase iconDB = new IconDatabase();
		try {
			System.out.println("INFO: Loading Hotline icons...");
			iconDB.loadIconsFromResourceFile(new File("/Users/shadowknight/Downloads/HotlineConnectClient-Mac/HLclient19.bin"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int port = 4000;
		ServerSocket serverSocket = new ServerSocket(port);
		try {
			InetAddress address = InetAddress.getLocalHost();
			System.out.println("INFO: Server running on: " + address.getHostAddress() + ":" + port);
		} catch (UnknownHostException e) {
			System.out.println("WARNING: Unable to determine this host's address");
		}

		while (true) {
			System.out.println("INFO: Waiting for connections...");
			Socket socket = serverSocket.accept();
			System.out.println("INFO: Client connection from: " + socket.getInetAddress().getHostAddress() + ":" + port);
			final HotWiredBridge bridge = new HotWiredBridge(config, iconDB, socket.getInputStream(), socket.getOutputStream());
			new Thread() {
				public void run() {
					try {
						bridge.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
}
