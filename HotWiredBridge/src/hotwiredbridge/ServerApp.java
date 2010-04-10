package hotwiredbridge;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class ServerApp {
	public static void main(String[] args) throws IOException {
		WiredServerConfig config = new WiredServerConfig("localhost", 2000);
		int port = 4000;
		ServerSocket serverSocket = new ServerSocket(port);
		try {
			InetAddress address = InetAddress.getLocalHost();
			System.out.println("INFO: Server running on: " + address.getHostAddress() + ":" + port);
		} catch (UnknownHostException e) {
			System.out.println("WARNING: Unable to determine this host's address");
		}

		while (true) {
			Socket socket = serverSocket.accept();
			System.out.println("INFO: Client connection from: " + socket.getInetAddress().getHostAddress() + ":" + port);
			final HotWiredBridge bridge = new HotWiredBridge(config, socket.getInputStream(), socket.getOutputStream());
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
