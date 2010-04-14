package hotwiredbridge;

import hotwiredbridge.hotline.*;
import hotwiredbridge.wired.*;

import java.security.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.*;

import javax.net.ssl.*;

public class HotWiredBridge implements WiredEventHandler {
	private WiredServerConfig config;
	private IconDatabase iconDB;
	private DataInputStream in;
	private DataOutputStream out;

	private WiredClient client;
	private SSLSocket wiredSocket;
	private LinkedBlockingQueue<Transaction> queue;
	private TransactionFactory factory;
	private Map<Long,String> userNames;
	private List<Transaction> pendingTransactions;
	private Map<Long,Transaction> pendingCreateChatTransactions;
	private boolean closed;

	public HotWiredBridge(WiredServerConfig config, IconDatabase iconDB, InputStream in, OutputStream out) {
		this.config = config;
		this.iconDB = iconDB;
		this.in = new DataInputStream(in);
		this.out = new DataOutputStream(out);
	}
	
	public void run() {
		if (!performHandshake()) {
			close();
			return;
		}

		try {
			client = createClientFor(config.getHost(), config.getPort());			
		} catch (Exception e) {
			close();
			return;
		}

		queue = new LinkedBlockingQueue<Transaction>();
		factory = new TransactionFactory();
		userNames = new HashMap<Long,String>();
		pendingTransactions = new LinkedList<Transaction>();
		pendingCreateChatTransactions = new HashMap<Long,Transaction>();
		
		new Thread() {
			public void run() {
				processOutgoingHotlineTransactions();
			}
		}.start();

		processIncomingHotlineTransactions();
	}

	private void processIncomingHotlineTransactions() {
		try {
			while (!closed) {
				Transaction t = Transaction.readFromStream(in);
				System.out.println("Got transaction: " + t.toString());
				handleTransaction(t);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close();
		}
	}
	
	private void processOutgoingHotlineTransactions() {
		try {
			while (!closed) {
				Transaction t = queue.take();
				System.out.println("Sending transaction: " + t.toString());
				Transaction.writeToStream(t, out);
				out.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			close();
		}
	}
	
	private synchronized void close() {
		if (!closed) {
			if (wiredSocket != null) {
				try { wiredSocket.close(); } catch (Exception e) {}
				wiredSocket = null;
			}
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

	private boolean performHandshake() {
		byte[] handshake = new byte[8];
		try {
			in.readFully(handshake);
			if (!new String(handshake).equals("TRTPHOTL")) {
				return false;
			}
			in.readInt(); // version
			out.write(HotlineUtils.pack("BN", "TRTP".getBytes(), 0));
			out.flush();
		} catch (IOException e) {
			return false;
		}
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
	
	public WiredClient createClientFor(String host, int port) throws Exception {
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
		wiredSocket = (SSLSocket) factory.createSocket(host, port);
		wiredSocket.setEnabledProtocols(new String[] {"TLSv1"});
		return new EventBasedWiredClient(wiredSocket.getInputStream(), wiredSocket.getOutputStream(), this);
	}

	public void handleTransaction(Transaction t) throws IOException {
		switch (t.getId()) {
		case Transaction.ID_LOGIN: {
			Transaction loginTransaction = t;
			String nick = MacRoman.toString(t.getObjectData(TransactionObject.NICK));
			String login = MacRoman.toString(decode(t.getObjectData(TransactionObject.LOGIN)));
			String password = MacRoman.toString(decode(t.getObjectData(TransactionObject.PASSWORD)));
			int result = client.login(nick, login, password);
			if (result == 0) {
				Integer icon = t.getObjectDataAsInt(TransactionObject.ICON);
				if (icon != null) {
					client.sendIcon(icon, iconDB.getBase64FromIconIndex(icon));
				}
				client.requestUserList(1);
				t = factory.createReply(loginTransaction);
				t.addObject(TransactionObject.SOCKET, HotlineUtils.pack("n", client.getLocalUserId()));
				queue.offer(t);
			} else {
				// error -> todo: msg?
				t = factory.createReply(t, true);
				queue.offer(t);
			}
			break;
		}
		case Transaction.ID_GETNEWS:
			client.requestNews();
			synchronized (pendingTransactions) {
				pendingTransactions.add(t);
			}
			break;
		case Transaction.ID_POST_NEWS: {
			String message = MacRoman.toString(t.getObjectData(TransactionObject.MESSAGE));
			client.postNews(message);
			break;
		}
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
		case Transaction.ID_CHANGE_NICK: {
			String nick = MacRoman.toString(t.getObjectData(TransactionObject.NICK));
			if (nick != null) {
				client.sendNick(nick);
			}
			Integer icon = t.getObjectDataAsInt(TransactionObject.ICON);
			if (icon != null) {
				client.sendIcon(icon, iconDB.getBase64FromIconIndex(icon));
			}
			break;
		}
		case Transaction.ID_GETFOLDERLIST: {
			String path = convertPath(t.getObjectData(TransactionObject.PATH));
			client.requestFileList(path);
			synchronized (pendingTransactions) {
				pendingTransactions.add(t);
			}
			break;
		}
		case Transaction.ID_GETFILEINFO: {
			String path = convertPath(t.getObjectData(TransactionObject.PATH));
			path += "/" + MacRoman.toString(t.getObjectData(TransactionObject.FILENAME));
			client.requestFileInfo(path);
			synchronized (pendingTransactions) {
				pendingTransactions.add(t);
			}
			break;
		}
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
			client.requestUserList(t.getObjectDataAsInt(TransactionObject.CHATWINDOW));
			synchronized (pendingTransactions) {
				pendingTransactions.add(t);
			}
			break;
		case Transaction.ID_LEAVING_PCHAT:
			client.leaveChat(t.getObjectDataAsInt(TransactionObject.CHATWINDOW));
			break;
		case Transaction.ID_REQUEST_CHANGE_SUBJECT: {
			String subject = MacRoman.toString(t.getObjectData(TransactionObject.SUBJECT));
			client.changeTopic(t.getObjectDataAsInt(TransactionObject.CHATWINDOW), subject);
			break;
		}
		case Transaction.ID_BROADCAST:
			client.broadcastMessage(MacRoman.toString(t.getObjectData(TransactionObject.MESSAGE)));
			break;
		default:
			System.out.println("NOT HANDLED!");
			t = factory.createReply(t, true);
			queue.offer(t);
		}
	}

	private static String convertPath(byte[] data) {
		if (data == null)
			return "";
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
	
	private static int getUserStatus(User user) {
		int status = 0;
		if (user.isIdle()) {
			status |= 1;
		}
		if (user.isAdmin()) {
			status |= 2;
		}
		return status;
	}
	
	private void addUserObjects(Transaction t, User user) {
		t.addObject(TransactionObject.SOCKET, HotlineUtils.pack("n", user.getId()));
		t.addObject(TransactionObject.ICON, HotlineUtils.pack("n", user.getIcon()));
		t.addObject(TransactionObject.STATUS, HotlineUtils.pack("n", getUserStatus(user)));
		t.addObject(TransactionObject.NICK, MacRoman.fromString(user.getNick()));
	}
	
	public byte[] packUser(User user) {
		byte[] nick = MacRoman.fromString(user.getNick());
		return HotlineUtils.pack("nnnnB", user.getId(), user.getIcon(), getUserStatus(user), nick.length, nick);
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
				addUserObjects(reply, user);
				queue.offer(reply);
				pendingCreateChatTransactions.remove(userListEvent.getChatId());
				return;
			}
			synchronized (pendingTransactions) {
				for (Transaction t : pendingTransactions) {
					if ((t.getId() == Transaction.ID_GETUSERLIST) ||
						(t.getId() == Transaction.ID_JOIN_PCHAT &&
						 t.getObjectDataAsInt(TransactionObject.CHATWINDOW) == userListEvent.getChatId()))
					{
						Transaction reply = factory.createReply(t);
						for (User user : userListEvent.getUsers()) {
							reply.addObject(TransactionObject.USER, packUser(user));
						}
						byte[] subject = t.getObjectData(TransactionObject.SUBJECT);
						if (subject != null) {
							reply.addObject(TransactionObject.SUBJECT, subject);
						}
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
			addUserObjects(t, user);
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
			userNames.put(user.getId(), user.getNick());
			Transaction t = factory.createRequest(Transaction.ID_USERCHANGE);
			addUserObjects(t, user);
			queue.offer(t);
		} else if (event instanceof FileListEvent) {
			FileListEvent fileListEvent = (FileListEvent) event;
			synchronized (pendingTransactions) {
				for (Transaction t : pendingTransactions) {
					if (t.getId() == Transaction.ID_GETFOLDERLIST) {
						String path = convertPath(t.getObjectData(TransactionObject.PATH));
						if (path.equals(fileListEvent.getPath())) {
							Transaction reply = factory.createReply(t);
							for (FileInfo file : fileListEvent.getFiles()) {
								int containedItems = (int) (!file.isDirectory() ? 0 : file.getSize());
								byte[] fileNameBytes = MacRoman.fromString(file.getName());
								byte[] data = HotlineUtils.pack("BBNNNB",
									file.isDirectory() ? "fldr".getBytes() : "????".getBytes(),
									file.isDirectory() ? new byte[4] : "????".getBytes(),
									file.getSize(), containedItems,
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
			long chatId = topicChangedEvent.getChatId();
			synchronized (pendingTransactions) {
				for (Transaction t : pendingTransactions) {
					if (t.getId() == Transaction.ID_JOIN_PCHAT && t.getObjectDataAsInt(TransactionObject.CHATWINDOW) == chatId) {
						t.addObject(TransactionObject.SUBJECT, MacRoman.fromString(topicChangedEvent.getTopic()));
						break;
					}
				}
			}
			Transaction t = factory.createRequest(Transaction.ID_CHANGED_SUBJECT);
			t.addObject(TransactionObject.CHATWINDOW, HotlineUtils.pack("N", chatId));
			t.addObject(TransactionObject.SUBJECT, MacRoman.fromString(topicChangedEvent.getTopic()));
			queue.offer(t);
		} else if (event instanceof WiredErrorEvent) {
			WiredErrorEvent wiredErrorEvent = (WiredErrorEvent) event;
			if (wiredErrorEvent.getErrorCode() == WiredClient.MSG_FILE_OR_DIRECTORY_NOT_FOUND) {
				synchronized (pendingTransactions) {
					for (Transaction t : pendingTransactions) {
						if (t.getId() == Transaction.ID_GETFOLDERLIST) {
							Transaction reply = factory.createReply(t, true);
							reply.addObject(TransactionObject.ERROR_MSG, MacRoman.fromString(wiredErrorEvent.getMessage()));
							queue.offer(reply);
							pendingTransactions.remove(t);
							break;
						}
					}
				}
			} else {
				Transaction t = factory.createRequest(Transaction.ID_PRIVATE_MESSAGE);
				t.addObject(TransactionObject.MESSAGE, MacRoman.fromString(wiredErrorEvent.getMessage()));
				queue.offer(t);
			}
		} else if (event instanceof FileInfoEvent) {
			FileInfoEvent fileInfoEvent = (FileInfoEvent) event;
			synchronized (pendingTransactions) {
				for (Transaction t : pendingTransactions) {
					if (t.getId() == Transaction.ID_GETFILEINFO) {
						String path = convertPath(t.getObjectData(TransactionObject.PATH));
						path += "/" + MacRoman.toString(t.getObjectData(TransactionObject.FILENAME));
						FileInfo fileInfo = fileInfoEvent.getFileInfo();
						if (path.equals(fileInfo.getPath())) {
							Transaction reply = factory.createReply(t);
							reply.addObject(TransactionObject.FILETYPE, fileInfo.isDirectory() ? "fldr".getBytes() : "????".getBytes());
							reply.addObject(TransactionObject.FILETYPESTR, "????".getBytes());
							reply.addObject(TransactionObject.FILECREATORSTR, "????".getBytes());
							reply.addObject(TransactionObject.FILENAME, MacRoman.fromString(fileInfo.getName()));
							reply.addObject(TransactionObject.FILECREATED, HotlineUtils.pack("D", fileInfo.getCreationDate()));
							reply.addObject(TransactionObject.FILEMODIFIED, HotlineUtils.pack("D", fileInfo.getModificationDate()));
							reply.addObject(TransactionObject.FILESIZE, HotlineUtils.pack("N", fileInfo.getSize()));
							if (fileInfoEvent.getComment() != null) {
								reply.addObject(TransactionObject.COMMENT, MacRoman.fromString(fileInfoEvent.getComment()));
							}
							queue.offer(reply);
							pendingTransactions.remove(t);
							break;
						}
					}
				}
			}
		} else if (event instanceof NewsPostEvent) {
			NewsPostEvent newsPostEvent = (NewsPostEvent) event;
			NewsPost post = newsPostEvent.getNewsPost();
			Transaction t = factory.createRequest(Transaction.ID_NEWS_POSTED);
			String message = String.format("From %s (%s):\r\r%s\r_________________________________________________\r",
				post.getNick(), new Date(post.getPostTime()).toString(), post.getPost());
			t.addObject(TransactionObject.MESSAGE, MacRoman.fromString(message));
			queue.offer(t);
		} else if (event instanceof NewsListEvent) {
			NewsListEvent newsListEvent = (NewsListEvent) event;
			synchronized (pendingTransactions) {
				for (Transaction t : pendingTransactions) {
					if (t.getId() == Transaction.ID_GETNEWS) {
						Transaction reply = factory.createReply(t);
						StringBuilder sb = new StringBuilder();
						for (NewsPost post : newsListEvent.getNewsPosts()) {
							if (post != newsListEvent.getNewsPosts().get(0)) {
								sb.append("_________________________________________________\r");
							}
							sb.append(String.format("From %s (%s):\r\r%s\r",
								post.getNick(), new Date(post.getPostTime()).toString(), post.getPost()));
						}
						reply.addObject(TransactionObject.MESSAGE, MacRoman.fromString(sb.toString()));
						queue.offer(reply);
						pendingTransactions.remove(t);
						break;
					}
				}
			}
		} else {
			System.out.println("not handled->"+event.getClass());
		}
	}
}
