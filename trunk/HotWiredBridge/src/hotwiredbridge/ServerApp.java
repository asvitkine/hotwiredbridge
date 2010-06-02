package hotwiredbridge;

import hotwiredbridge.hotline.IconDatabase;

import java.io.*;
import java.net.*;

public class ServerApp {
	public static void main(String[] args) throws IOException {
		System.setProperty("java.awt.headless", "true"); 

		final WiredServerConfig config = new WiredServerConfig("localhost", 2000);

		System.out.println("INFO: Starting server...");

		final IconDatabase iconDB = new IconDatabase();
		try {
			System.out.println("INFO: Loading Hotline icons...");
			iconDB.loadIconsFromResourceFile(new File("/Users/shadowknight/Downloads/HotlineConnectClient-Mac/HLclient19.bin"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		final FileTypeAndCreatorMap fileTypeAndCreatorMap = new FileTypeAndCreatorMap();
		try {
			System.out.println("INFO: Loading type and creator codes database...");
			fileTypeAndCreatorMap.loadFromAppleVolumesFile(new File("/Users/shadowknight/Projects/hotwiredbridge/AppleVolumes.system"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		final FileTransferMap fileTransferMap = new FileTransferMap();

		int port = 4000;
		ServerSocket serverSocket = new ServerSocket(port);
		try {
			InetAddress address = InetAddress.getLocalHost();
			System.out.println("INFO: Server running on: " + address.getHostAddress() + ":" + port);
		} catch (UnknownHostException e) {
			System.out.println("WARNING: Unable to determine this host's address");
		}
	
		final int fileBridgePort = port + 1;
		new Thread() {
			public void run() {
				try {
					runFileBridgeServer(config, fileTypeAndCreatorMap, fileTransferMap, fileBridgePort);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

		while (true) {
			System.out.println("INFO: Waiting for connections...");
			final Socket socket = serverSocket.accept();
			System.out.println("INFO: Client connection from: " + socket.getInetAddress().getHostAddress() + ":" + port);
			new Thread() {
				public void run() {
					try {
						InputStream in = socket.getInputStream();
						OutputStream out = socket.getOutputStream();
						HotWiredBridge bridge = new HotWiredBridge(config, iconDB, fileTypeAndCreatorMap, fileTransferMap, in, out);
						bridge.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	private static void runFileBridgeServer(final WiredServerConfig config, final FileTypeAndCreatorMap fileTypeAndCreatorMap, final FileTransferMap fileTransferMap, int port) throws IOException {
		ServerSocket serverSocket = new ServerSocket(port);
		try {
			InetAddress address = InetAddress.getLocalHost();
			System.out.println("INFO: File bridge running on: " + address.getHostAddress() + ":" + port);
		} catch (UnknownHostException e) {
			System.out.println("WARNING: Unable to determine this host's address");
		}
		
		while (true) {
			System.out.println("INFO: Waiting for file protocol connections...");
			final Socket socket = serverSocket.accept();
			System.out.println("INFO: File protocol connection from: " + socket.getInetAddress().getHostAddress() + ":" + port);
			new Thread() {
				public void run() {
					try {
						InputStream in = socket.getInputStream();
						OutputStream out = socket.getOutputStream();
						HotWiredFileBridge bridge = new HotWiredFileBridge(config, fileTypeAndCreatorMap, fileTransferMap, in, out);
						bridge.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
}
