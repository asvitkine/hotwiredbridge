package hotwiredbridge;

import hotwiredbridge.hotline.IconDatabase;

import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
	private static SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");

	public static ServerConfig getDefaultServerConfig() {
		ServerConfig config = new ServerConfig();
		String root = "/Users/shadowknight/Projects/hotwiredbridge/";
		config.setWiredHost("localhost");
		config.setWiredPort(2000);
		config.setIconsFilePath(root + "HLclient19.bin");
		config.setAppleVolumesFilePath(root + "AppleVolumes.system");
		config.setCertificatesFilePath(root + "ssl_certs");
		config.setCertificatesFilePassphrase("changeit");
		config.setHotlinePort(4000);
		return config;
	}
	
	public static void log(String message, Object... args) {
		System.out.println(format.format(new Date()) + "\t" + String.format(message, args));
	}
	
	public static void main(String[] args) throws IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		System.setProperty("java.awt.headless", "true");

		ServerConfig serverConfig = null;

		boolean nextIsConfigPath = false;
		for (String arg : args) {
			if (nextIsConfigPath) {
				log("INFO: Reading server config from file '" + arg + "'.");
				serverConfig = ServerConfigReader.read(arg);
				nextIsConfigPath = false;
				break;
			}
			if (arg.equals("-config")) {
				nextIsConfigPath = true;
			}
		}
		
		if (nextIsConfigPath) {
			log("ERROR: Missing argument follow '-config'.");
			return;
		}

		if (serverConfig == null) {
			log("WARNING: Using default (hardcoded) server config...");
			serverConfig = getDefaultServerConfig();
		}

		if (serverConfig.getIgnoreSigTerm()) {
			log("INFO: Installing SIGTERM handler...");
			new SignalSuppressor(SignalSuppressor.SIGTERM).installHandler();
		}

		final WiredServerConfig config = new WiredServerConfig(serverConfig.getWiredHost(), serverConfig.getWiredPort());

		log("INFO: Starting server...");

		final IconDatabase iconDB = new IconDatabase();
		try {
			boolean parallel = serverConfig.getParallelIconLoading();
			log("INFO: Loading Hotline icons (parallel = " + parallel + ")...");
			ExecutorService pool = parallel ?
				Executors.newCachedThreadPool() :
				Executors.newSingleThreadExecutor();

			iconDB.loadIconsFromResourceFile(new File(serverConfig.getIconsFilePath()), pool);
		} catch (IOException e) {
			e.printStackTrace();
		}

		final FileTypeAndCreatorMap fileTypeAndCreatorMap = new FileTypeAndCreatorMap();
		try {
			log("INFO: Loading type and creator codes database...");
			fileTypeAndCreatorMap.loadFromAppleVolumesFile(new File(serverConfig.getAppleVolumesFilePath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File certificatesFile = new File(serverConfig.getCertificatesFilePath());
		String passphrase = serverConfig.getCertificatesFilePassphrase();

		final KeyStoreProvider ksp = new KeyStoreProvider(certificatesFile, passphrase);
		log("INFO: Getting server SSL certificates...");
		boolean added = ksp.addCertificatesForServer(config.getHost(), config.getPort());
		log("INFO: SSL certificates were " +
			(added ? "added to" : "already present in") + " keystore file.");

		final FileTransferMap fileTransferMap = new FileTransferMap();

		int port = serverConfig.getHotlinePort();
		ServerSocket serverSocket = new ServerSocket(port);
		try {
			InetAddress address = InetAddress.getLocalHost();
			log("INFO: Server running on: " + address.getHostAddress() + ":" + port);
		} catch (UnknownHostException e) {
			log("WARNING: Unable to determine this host's address");
		}
	
		final int fileBridgePort = port + 1;
		new Thread() {
			public void run() {
				try {
					runFileBridgeServer(config, ksp, fileTypeAndCreatorMap, fileTransferMap, fileBridgePort);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

		while (true) {
			log("INFO: Waiting for connections...");
			final Socket socket = serverSocket.accept();
			log("INFO: Client connection from: " + socket.getInetAddress().getHostAddress() + ":" + port);
			new Thread() {
				public void run() {
					try {
						InputStream in = socket.getInputStream();
						OutputStream out = socket.getOutputStream();
						HotWiredBridge bridge = new HotWiredBridge(config, iconDB, ksp,
							fileTypeAndCreatorMap, fileTransferMap, in, out);
						bridge.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	private static void runFileBridgeServer(final WiredServerConfig config, final KeyStoreProvider ksp, final FileTypeAndCreatorMap fileTypeAndCreatorMap, final FileTransferMap fileTransferMap, int port) throws IOException {
		ServerSocket serverSocket = new ServerSocket(port);
		try {
			InetAddress address = InetAddress.getLocalHost();
			log("INFO: File bridge running on: " + address.getHostAddress() + ":" + port);
		} catch (UnknownHostException e) {
			log("WARNING: Unable to determine this host's address");
		}
		
		while (true) {
			log("INFO: Waiting for file protocol connections...");
			final Socket socket = serverSocket.accept();
			log("INFO: File protocol connection from: " + socket.getInetAddress().getHostAddress() + ":" + port);
			new Thread() {
				public void run() {
					try {
						InputStream in = socket.getInputStream();
						OutputStream out = socket.getOutputStream();
						HotWiredFileBridge bridge = new HotWiredFileBridge(config, ksp, fileTypeAndCreatorMap, fileTransferMap, in, out);
						bridge.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
}
