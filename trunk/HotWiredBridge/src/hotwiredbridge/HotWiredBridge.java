package hotwiredbridge;

import hotwiredbridge.hotline.*;
import hotwiredbridge.wired.*;

import java.security.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.*;
import java.net.*;

import javax.net.ssl.*;

public class HotWiredBridge implements WiredEventHandler {
	public static void main(String[] args) throws IOException {
		new HotWiredBridge().run();
	}

	private WiredClient client;
	private DataInputStream in;
	private DataOutputStream out;
	private LinkedBlockingQueue<Transaction> queue;
	private TransactionFactory factory;
	private Map<Integer,String> userNames;
	private List<Transaction> pendingTransactions;

	public void run() throws IOException {
		queue = new LinkedBlockingQueue<Transaction>();
		factory = new TransactionFactory();
		userNames = new HashMap<Integer,String>();
		pendingTransactions = new ArrayList<Transaction>();
		ServerSocket serverSocket = new ServerSocket(4000);
		System.out.println("X");
		while (true) {
			Socket socket = serverSocket.accept();
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());

			if (!performHandshake()) {
				socket.close();
				continue;
			}
	
			try {
				client = createClientFor("localhost", 2000, this);
				// we need to keep a list of pending transactions and answer them as we can.
				// typical process:
				// 		hl sends us a request ('get userlist')
				//		we forward it to wired -> client.requestUserList(1);
				//      we get userlist from wired
				//      we send it to hl (we need to remember the transaction number)
				// and:
				//     wired sends us a msg -> processServerMessage(msg)
				//     convert it, and send it to hl
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			new Thread() {
				public void run() {
					processOutgoingHotlineTransactions();
				}
			}.start();

			processIncomingHotlineTransactions();
		}
	}
	
	private void processIncomingHotlineTransactions() throws IOException {
		while (true) {
			Transaction t = Transaction.readFromStream(in);
			System.out.println("Got transaction: " + t.toString());
			handleTransaction(t);
		}
	}
	
	private void processOutgoingHotlineTransactions() {
		try {
			while (true) {
				Transaction t = queue.take();
				System.out.println("Sending transaction: " + t.toString());
				Transaction.writeToStream(t, out);
				out.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean performHandshake() throws IOException {
		byte[] handshake = new byte[8];
		in.readFully(handshake);
		if (!new String(handshake).equals("TRTPHOTL")) {
			return false;
		}
		System.out.println(in.readInt()); // version number - who cares!
		out.write(HotlineUtils.pack("BN", "TRTP".getBytes(), 0));
		out.flush();
		return true;
	}

	public static byte[] decode(byte[] bytes) {
		if (bytes == null) return null;
		byte[] decoded = new byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			int value = (bytes[i] < 0 ? 256 + bytes[i] : bytes[i]);
			decoded[i] = (byte) (255 - value);
		}
		return decoded;
	}
	
	public static WiredClient createClientFor(String host, int port, HotWiredBridge hs) throws Exception {
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
		return new EventBasedWiredClient(socket.getInputStream(), socket.getOutputStream(), hs);
	}

	public void handleTransaction(Transaction t) throws IOException {
		switch (t.getId()) {
		case Transaction.ID_LOGIN:
			/*
			 *   obj(105) obj(106) obj(102) obj(104) obj(160)
			 *  public static final int NICK = 102;
			 *  public static final int ICON = 104;
			 *  public static final int LOGIN = 105;
			 *  public static final int PASSWORD = 106;
			 *  public static final int VERSION = 160;
			 */
			Transaction loginTransaction = t;
			String nick = MacRoman.toString(t.getObjectData(TransactionObject.NICK));
			String login = MacRoman.toString(decode(t.getObjectData(TransactionObject.LOGIN)));
			String password = MacRoman.toString(decode(t.getObjectData(TransactionObject.PASSWORD)));
			int result = client.login(nick, login, password);
			if (result == 0) {
				client.requestUserList(1);
				// FIXME:
				t = factory.createReply(loginTransaction);
				t.addObject(TransactionObject.SOCKET, new byte[] {(byte)0,(byte)1});
				queue.offer(t);
				/*
				t = factory.createRequest(Transaction.ID_USERLIST);
				t.addObject(TransactionObject.PRIVS, new byte[8]));
				t.addObject(TransactionObject.USER, new UserListTransaction.User(1, 0, 0, "Batman").toByteArray()));
				queue.offer(t);*/
			} else {
				// error -> todo: msg?
				t = factory.createReply(t, true);
				queue.offer(t);
			}
			break;
		case Transaction.ID_GETNEWS:
			t = factory.createReply(t);
			t.addObject(TransactionObject.MESSAGE, new byte[0]);
			queue.offer(t);
			break;
		case Transaction.ID_GETUSERLIST:
			client.requestUserList(1);
			synchronized (pendingTransactions) {
				pendingTransactions.add(t);
			}
			break;
		case Transaction.ID_GETUSERINFO:
			client.requestUserInfo(t.getObjectDataAsInt(TransactionObject.SOCKET));
			synchronized (pendingTransactions) {
				pendingTransactions.add(t);
			}
			break;
		case Transaction.ID_SEND_CHAT:
			client.sendChatMessage(1, MacRoman.toString(t.getObjectData(TransactionObject.MESSAGE)));
			break;
		case Transaction.ID_SEND_PM: {
			int userId = t.getObjectDataAsInt(TransactionObject.SOCKET);
			String message = MacRoman.toString(t.getObjectData(TransactionObject.MESSAGE));
			client.sendPrivateMessage(userId, message);			
			break;
		}
		case Transaction.ID_CHANGE_NICK:
			client.sendNick(MacRoman.toString(t.getObjectData(TransactionObject.NICK)));
			break;
		case Transaction.ID_GETFOLDERLIST:
			String path = convertPath(t.getObjectData(TransactionObject.PATH));
			client.requestFileList(path);
			synchronized (pendingTransactions) {
				pendingTransactions.add(t);
			}
			break;
		default:
			System.out.println("NOT HANDLED!");
			t = factory.createReply(t, true);
			queue.offer(t);
		}
	}

	private static String convertPath(byte[] data) {
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		DataInputStream in = new DataInputStream(stream);
		StringBuilder sb = new StringBuilder();
		try {
			int levels = in.readShort();
			for (int i = 0; i < levels; i++) {
				in.readShort();
				int nameLength = in.readUnsignedByte();
				byte[] buf = new byte[nameLength];
				in.readFully(buf);
				sb.append("/");
				sb.append(MacRoman.toString(buf));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb.toString();
	}

	public void handleEvent(WiredEvent event) {
		System.out.println("handleEvent!!");
		if (event instanceof ChatEvent) {
			ChatEvent chatEvent = (ChatEvent) event;
			Transaction t = factory.createRequest(Transaction.ID_CHAT);
			String message = String.format("\r%13.13s: %s", userNames.get(chatEvent.getUserId()), chatEvent.getMessage());
			t.addObject(TransactionObject.MESSAGE, MacRoman.fromString(message));
			t.addObject(TransactionObject.SOCKET, HotlineUtils.pack("n", chatEvent.getUserId()));
			queue.offer(t);
			System.out.println("done");
		} else if (event instanceof UserListEvent) {
			UserListEvent userListEvent = (UserListEvent) event;
			for (User user : userListEvent.getUsers()) {
				userNames.put(user.getId(), user.getNick());
			}
			synchronized (pendingTransactions) {
				for (Transaction t : pendingTransactions) {
					if (t.getId() == Transaction.ID_GETUSERLIST) {
						Transaction reply = factory.createReply(t);
						for (User user : userListEvent.getUsers()) {
							HotlineUser hotlineUser = new HotlineUser(user.getId(), 0, 0, user.getNick());
							reply.addObject(TransactionObject.USER, hotlineUser.toByteArray());
						}
						reply.addObject(TransactionObject.SUBJECT, new byte[0]);
						queue.offer(reply);
						pendingTransactions.remove(t);
						break;
					}
				}
			}
		} else if (event instanceof UserLeaveEvent) {
			UserLeaveEvent userLeaveEvent = (UserLeaveEvent) event;
			// TODO: chatId
			Transaction t = factory.createRequest(Transaction.ID_USERLEAVE);
			t.addObject(TransactionObject.SOCKET, HotlineUtils.pack("n", userLeaveEvent.getUserId()));
			queue.offer(t);
		} else if (event instanceof UserJoinEvent) {
			UserJoinEvent userJoinEvent = (UserJoinEvent) event;
			// TODO: chatId
			User user = userJoinEvent.getUser();
			userNames.put(user.getId(), user.getNick());
			Transaction t = factory.createRequest(Transaction.ID_USERCHANGE);
			t.addObject(TransactionObject.SOCKET, HotlineUtils.pack("n", user.getId()));
			t.addObject(TransactionObject.ICON, HotlineUtils.pack("n", 0));
			t.addObject(TransactionObject.STATUS, HotlineUtils.pack("n", 0)); // TODO
			t.addObject(TransactionObject.NICK, MacRoman.fromString(user.getNick()));
			queue.offer(t);
		} else if (event instanceof PrivateMessageEvent) {
			PrivateMessageEvent privateMessageEvent = (PrivateMessageEvent) event;
			Transaction t = factory.createRequest(Transaction.ID_PRIVATE_MESSAGE);
			t.addObject(TransactionObject.SOCKET, HotlineUtils.pack("n", privateMessageEvent.getFromUserId()));
			t.addObject(TransactionObject.NICK, MacRoman.fromString(userNames.get(privateMessageEvent.getFromUserId())));
			t.addObject(TransactionObject.MESSAGE, MacRoman.fromString(privateMessageEvent.getMessage()));
			queue.offer(t);
		} else if (event instanceof UserInfoEvent) {
			UserInfoEvent userInfoEvent = (UserInfoEvent) event;
			synchronized (pendingTransactions) {
				for (Transaction t : pendingTransactions) {
					if (t.getId() == Transaction.ID_GETUSERINFO &&
						t.getObjectDataAsInt(TransactionObject.SOCKET) == userInfoEvent.getUser().getId())
					{
						User user = userInfoEvent.getUser();
						StringBuilder sb = new StringBuilder();
						sb.append(String.format("    name: %s\r", user.getNick()));
						sb.append(String.format("   login: %s\r", user.getLogin()));
						sb.append(String.format("    host: %s\r", userInfoEvent.getHost()));
						sb.append(String.format(" address: %s\r", userInfoEvent.getIp()));
						sb.append(String.format(" version: %s\r", userInfoEvent.getClientVersion()));
						sb.append(String.format("login tm: %s\r", userInfoEvent.getLoginTime()));
						Transaction reply = factory.createReply(t);
						reply.addObject(TransactionObject.MESSAGE, MacRoman.fromString(sb.toString()));
						queue.offer(reply);
						pendingTransactions.remove(t);
						break;
					}
				}
			}
		} else if (event instanceof UserStatusChangeEvent) {
			UserStatusChangeEvent userStatusChangeEvent = (UserStatusChangeEvent) event;
			User user = userStatusChangeEvent.getUser();
			String oldNick = userNames.get(user.getId());
			if (!user.getNick().equals(oldNick)) {
				userNames.put(user.getId(), user.getNick());
				Transaction t = factory.createRequest(Transaction.ID_USERCHANGE);
				t.addObject(TransactionObject.SOCKET, HotlineUtils.pack("n", user.getId()));
				t.addObject(TransactionObject.ICON, HotlineUtils.pack("n", 0));
				t.addObject(TransactionObject.STATUS, HotlineUtils.pack("n", 0)); // TODO
				t.addObject(TransactionObject.NICK, MacRoman.fromString(user.getNick()));
				queue.offer(t);
			}
		} else if (event instanceof FileListEvent) {
			FileListEvent fileListEvent = (FileListEvent) event;
			synchronized (pendingTransactions) {
				for (Transaction t : pendingTransactions) {
					if (t.getId() == Transaction.ID_GETFOLDERLIST) {
						String path = convertPath(t.getObjectData(TransactionObject.PATH));
						if (path.equals(fileListEvent.getPath())) {
							Transaction reply = factory.createReply(t);
							for (FileInfo file : fileListEvent.getFiles()) {
								int fileSize = (int) (file.isDirectory() ? 0 : file.getSize());
								int containedItems = (int) (!file.isDirectory() ? 0 : file.getSize());
								System.out.println("A:"+file.getName());
								System.out.println("B:"+path);
								System.out.println("C:"+file.getName().substring(path.length()));
								byte[] fileNameBytes = MacRoman.fromString(file.getName());
								byte[] data = HotlineUtils.pack("BBNNNB",
									file.isDirectory() ? "fldr".getBytes() : "????".getBytes(),
									file.isDirectory() ? new byte[4] : "????".getBytes(),
									fileSize, containedItems,
									fileNameBytes.length, fileNameBytes);
								reply.addObject(TransactionObject.FILE_ENTRY, data);	
							}
							queue.offer(reply);
							pendingTransactions.remove(t);
							break;
						}
					}
				}
			}
		} else {
			System.out.println("not handled->"+event.getClass());
		}
	}
}
