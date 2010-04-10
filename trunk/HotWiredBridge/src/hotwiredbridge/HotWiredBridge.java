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
	private Map<Integer,Transaction> pendingCreateChatTransactions;

	public void run() throws IOException {
		queue = new LinkedBlockingQueue<Transaction>();
		factory = new TransactionFactory();
		userNames = new HashMap<Integer,String>();
		pendingTransactions = new LinkedList<Transaction>();
		pendingCreateChatTransactions = new HashMap<Integer,Transaction>();
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
		case Transaction.ID_SEND_CHAT: {
			Integer chatWindow = t.getObjectDataAsInt(TransactionObject.CHATWINDOW);
			Integer param = t.getObjectDataAsInt(TransactionObject.PARAMETER);
			int chatId = (chatWindow == null || chatWindow == 0 ? 1 : chatWindow);
			if (param != null && param != 0) {
				client.sendEmoteMessage(chatId, MacRoman.toString(t.getObjectData(TransactionObject.MESSAGE)));
			} else {
				client.sendChatMessage(chatId, MacRoman.toString(t.getObjectData(TransactionObject.MESSAGE)));
			}
			break;
		}
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
		case Transaction.ID_CREATE_PCHAT:
			client.createPrivateChat();
			synchronized (pendingTransactions) {
				pendingTransactions.add(t);
			}
			break;
		case Transaction.ID_INVITE_TO_PCHAT:
			client.inviteToChat(t.getObjectDataAsInt(TransactionObject.SOCKET), t.getObjectDataAsInt(TransactionObject.CHATWINDOW));
			break;
		case Transaction.ID_REJECT_PCHAT:
			client.declineInvitation(t.getObjectDataAsInt(TransactionObject.CHATWINDOW));
			break;
		case Transaction.ID_JOIN_PCHAT:
			client.joinChat(t.getObjectDataAsInt(TransactionObject.CHATWINDOW));
			break;
		case Transaction.ID_LEAVING_PCHAT:
			client.leaveChat(t.getObjectDataAsInt(TransactionObject.CHATWINDOW));
			break;
		case Transaction.ID_REQUEST_CHANGE_SUBJECT: {
			String subject = MacRoman.toString(t.getObjectData(TransactionObject.SUBJECT));
			client.changeTopic(t.getObjectDataAsInt(TransactionObject.CHATWINDOW), subject);
			break;
		}
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
		if (event instanceof ChatEvent) {
			ChatEvent chatEvent = (ChatEvent) event;
			Transaction t = factory.createRequest(Transaction.ID_RELAY_CHAT);
			String message;
			if (chatEvent.isEmote()) {
				message = String.format("\r *** %s %s", userNames.get(chatEvent.getUserId()), chatEvent.getMessage());
			} else {
				message = String.format("\r%13.13s: %s", userNames.get(chatEvent.getUserId()), chatEvent.getMessage());
			}
			t.addObject(TransactionObject.MESSAGE, MacRoman.fromString(message));
			t.addObject(TransactionObject.SOCKET, HotlineUtils.pack("n", chatEvent.getUserId()));
			if (chatEvent.getChatId() != 1) {
				t.addObject(TransactionObject.CHATWINDOW, HotlineUtils.pack("N", chatEvent.getChatId()));
			}
			if (chatEvent.isEmote()) {
				t.addObject(TransactionObject.PARAMETER, HotlineUtils.pack("n", 1));	
			}
			queue.offer(t);
		} else if (event instanceof UserListEvent) {
			UserListEvent userListEvent = (UserListEvent) event;
			for (User user : userListEvent.getUsers()) {
				userNames.put(user.getId(), user.getNick());
			}
			Transaction createChatTransaction = pendingCreateChatTransactions.get(userListEvent.getChatId());
			if (createChatTransaction != null) {
				try {
					client.inviteToChat(createChatTransaction.getObjectDataAsInt(TransactionObject.SOCKET), userListEvent.getChatId());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				Transaction reply = factory.createReply(createChatTransaction);
				reply.addObject(TransactionObject.CHATWINDOW, HotlineUtils.pack("N", userListEvent.getChatId()));
				User user = userListEvent.getUsers().get(0);
				reply.addObject(TransactionObject.SOCKET, HotlineUtils.pack("n", user.getId()));
				reply.addObject(TransactionObject.ICON, HotlineUtils.pack("n", 0));
				reply.addObject(TransactionObject.STATUS, HotlineUtils.pack("n", 0));
				reply.addObject(TransactionObject.NICK, MacRoman.fromString(user.getNick()));
				queue.offer(reply);
				pendingCreateChatTransactions.remove(userListEvent.getChatId());
				return;
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
			Transaction t;
			if (userLeaveEvent.getChatId() == 1) {
				t = factory.createRequest(Transaction.ID_USERLEAVE);
			} else {
				t = factory.createRequest(Transaction.ID_LEFT_PCHAT);
				t.addObject(TransactionObject.CHATWINDOW, HotlineUtils.pack("N", userLeaveEvent.getChatId()));
			}
			t.addObject(TransactionObject.SOCKET, HotlineUtils.pack("n", userLeaveEvent.getUserId()));
			queue.offer(t);
		} else if (event instanceof UserJoinEvent) {
			UserJoinEvent userJoinEvent = (UserJoinEvent) event;
			User user = userJoinEvent.getUser();
			Transaction t;
			if (userJoinEvent.getChatId() == 1) {
				userNames.put(user.getId(), user.getNick());
				t = factory.createRequest(Transaction.ID_USERCHANGE);
			} else {
				t = factory.createRequest(Transaction.ID_JOINED_PCHAT);
				t.addObject(TransactionObject.CHATWINDOW, HotlineUtils.pack("N", userJoinEvent.getChatId()));
			}
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
		} else if (event instanceof InviteEvent) {
			InviteEvent inviteEvent = (InviteEvent) event;
			Transaction t = factory.createRequest(Transaction.ID_INVITE_TO_PCHAT);
			t.addObject(TransactionObject.CHATWINDOW, HotlineUtils.pack("N", inviteEvent.getChatId()));
			t.addObject(TransactionObject.SOCKET, HotlineUtils.pack("n", inviteEvent.getUserId()));
			t.addObject(TransactionObject.NICK, MacRoman.fromString(userNames.get(inviteEvent.getUserId())));
			queue.offer(t);
		} else if (event instanceof ChatCreatedEvent) {
			ChatCreatedEvent privateChatCreatedEvent = (ChatCreatedEvent) event;
			synchronized (pendingTransactions) {
				for (Transaction t : pendingTransactions) {
					if (t.getId() == Transaction.ID_CREATE_PCHAT) {
						try {
							client.requestUserList(privateChatCreatedEvent.getChatId());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						pendingCreateChatTransactions.put(privateChatCreatedEvent.getChatId(), t);
						pendingTransactions.remove(t);
						break;
					}
				}
			}
		} else if (event instanceof TopicChangedEvent) {
			TopicChangedEvent topicChangedEvent = (TopicChangedEvent) event;
			Transaction t = factory.createRequest(Transaction.ID_CHANGED_SUBJECT);
			t.addObject(TransactionObject.CHATWINDOW, HotlineUtils.pack("N", topicChangedEvent.getChatId()));
			t.addObject(TransactionObject.SUBJECT, MacRoman.fromString(topicChangedEvent.getTopic()));
			queue.offer(t);
		} else {
			System.out.println("not handled->"+event.getClass());
		}
	}
}
