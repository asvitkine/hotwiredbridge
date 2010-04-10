package hotwiredbridge.wired;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class EventBasedWiredClient extends WiredClient {
	private WiredEventHandler handler;
	private HashMap<Integer,List<User>> users = new HashMap<Integer,List<User>>();
	private ArrayList<FileInfo> files = new ArrayList<FileInfo>();

	public EventBasedWiredClient(InputStream in, OutputStream out, WiredEventHandler handler) {
		super(in, out);
		this.handler = handler;
	}

	@Override
	protected void processServerMessage(int code, List<String> params) {
		System.out.println("Got " + code);
		if (params != null) {
			System.out.println("With params: ");
			for (String p : params)
				System.out.println("  " + p);
		}
		if (code == WiredClient.MSG_CHAT || code == WiredClient.MSG_ACTION_CHAT) {
			ChatEvent event = new ChatEvent();
			event.setChatId(Integer.valueOf(params.get(0)));
			event.setUserId(Integer.valueOf(params.get(1)));
			event.setMessage(params.get(2));
			event.setEmote(code == WiredClient.MSG_ACTION_CHAT);
			handler.handleEvent(event);
		} else if (code == WiredClient.MSG_USERLIST) {
			int chatId = Integer.valueOf(params.get(0));
			List<User> userlist = users.get(chatId);
			if (userlist == null) {
				userlist = new ArrayList<User>();
				users.put(chatId, userlist);
			}
			userlist.add(readUser(params));
		} else if (code == WiredClient.MSG_USERLIST_DONE) {
			int chatId = Integer.valueOf(params.get(0));
			List<User> userlist = users.get(chatId);
			if (userlist != null) {
				UserListEvent event = new UserListEvent();
				event.setChatId(chatId);
				event.setUsers(userlist);
				handler.handleEvent(event);
				users.remove(chatId);
			}
		} else if (code == WiredClient.MSG_CLIENT_JOIN) {
			UserJoinEvent event = new UserJoinEvent();
			event.setChatId(Integer.valueOf(params.get(0)));
			event.setUser(readUser(params));
			handler.handleEvent(event);
		} else if (code == WiredClient.MSG_CLIENT_LEAVE) {
			UserLeaveEvent event = new UserLeaveEvent();
			event.setChatId(Integer.valueOf(params.get(0)));
			event.setUserId(Integer.valueOf(params.get(1)));
			handler.handleEvent(event);
		} else if (code == WiredClient.MSG_FILE_LISTING) {
			files.add(readFile(params));
		} else if (code == WiredClient.MSG_FILE_LISTING_DONE) {
			FileListEvent event = new FileListEvent();
			event.setPath(params.get(0));
			event.setFiles(files);
			files = new ArrayList<FileInfo>();
			handler.handleEvent(event);
		} else if (code == WiredClient.MSG_PRIVATE_CHAT_INVITE) {
			InviteEvent event = new InviteEvent();
			event.setChatId(Integer.valueOf(params.get(0)));
			event.setUserId(Integer.valueOf(params.get(1)));
			handler.handleEvent(event);
		} else if (code == WiredClient.MSG_PRIVATE_MESSAGE) {
			PrivateMessageEvent event = new PrivateMessageEvent();
			event.setFromUserId(Integer.valueOf(params.get(0)));
			event.setMessage(params.get(1));
			handler.handleEvent(event);
		} else if (code == WiredClient.MSG_CLIENT_INFO) {
			UserInfoEvent event = new UserInfoEvent();
			User user = new User();
			readBaseUserFields(user, 0, params);
			if (params.size() > 16)
				user.setImage(params.get(16));
			event.setUser(user);
			event.setIp(params.get(6));
			event.setHost(params.get(7));
			event.setClientVersion(params.get(8));
			event.setCipherName(params.get(9));
			event.setCipherBits(params.get(10));
			event.setLoginTime(params.get(11));
			event.setIdleTime(params.get(10));
			handler.handleEvent(event);
		} else if (code == WiredClient.MSG_STATUS_CHANGE) {
			UserStatusChangeEvent event = new UserStatusChangeEvent();
			User user = new User();
			readBaseUserFields(user, 0, params);
			event.setUser(user);
			handler.handleEvent(event);
		}
	}

	private static FileInfo readFile(List<String> params) {
		FileInfo file = new FileInfo();
		file.setPath(params.get(0));
		file.setName(new File(params.get(0)).getName());
		file.setType(Integer.valueOf(params.get(1)));		
		file.setSize(Integer.valueOf(params.get(2)));
		file.setCreationDate(parseDate(params.get(3)));
		file.setModificationDate(parseDate(params.get(4)));
		//file.setIcon(getIconAsString(params.get(0)));
		return file;
	}

	private static long parseDate(String dateString) {
		System.out.println(dateString);
		System.out.println(ISO8601.parse(dateString).toString());
		return ISO8601.parse(dateString).getTimeInMillis();
	}
	
	private static void readBaseUserFields(User user, int index, List<String> params) {
		user.setId(Integer.valueOf(params.get(index++)));
		user.setIdle(Boolean.valueOf(params.get(index++)));
		user.setAdmin(Boolean.valueOf(params.get(index++)));
		index++;
		user.setNick(params.get(index++));
		user.setLogin(params.get(index++));
	}
	
	private static User readUser(List<String> params) {
		User user = new User();
		readBaseUserFields(user, 1, params);
		if (params.size() > 9)
			user.setImage(params.get(9));
		return user;
	}

}
