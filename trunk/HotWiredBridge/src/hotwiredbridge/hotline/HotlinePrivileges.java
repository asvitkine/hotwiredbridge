package hotwiredbridge.hotline;

import java.io.*;

public class HotlinePrivileges {
	public static final int CAN_DELETE_FILES = 63;
	public static final int CAN_UPLOAD_FILES = 62;
	public static final int CAN_DOWNLOAD_FILES = 61;
	public static final int CAN_RENAME_FILES = 60;
	public static final int CAN_MOVE_FILES = 59;
	public static final int CAN_CREATE_FOLDERS = 58;
	public static final int CAN_DELETE_FOLDERS = 57;
	public static final int CAN_RENAME_FOLDERS = 56;
	public static final int CAN_MOVE_FOLDERS = 55;
	public static final int CAN_READ_CHAT = 54;
	public static final int CAN_SEND_CHAT = 53;
	public static final int CAN_START_CHATS = 52;
	public static final int CAN_CREATE_USERS = 49;
	public static final int CAN_DELETE_USERS = 48;
	public static final int CAN_READ_USERS = 47;
	public static final int CAN_MODIFY_USERS = 46;
	public static final int CAN_READ_NEWS = 43;
	public static final int CAN_POST_NEWS = 42;
	public static final int CAN_DISCONNECT_USERS = 41;
	public static final int CANNOT_BE_DISCONNECTED = 40;
	public static final int CAN_GET_USER_INFO = 39;
	public static final int CAN_UPLOAD_ANYWHERE = 38;
	public static final int CAN_USE_ANY_NAME = 37;
	public static final int DONT_SHOW_AGREEMENT = 36;
	public static final int CAN_COMMENT_FILES = 35;
	public static final int CAN_COMMENT_FOLDERS = 34;
	public static final int CAN_VIEW_DROP_BOXES = 33;
	public static final int CAN_MAKE_ALIASES = 32;
	public static final int CAN_BROADCAST = 31;
	public static final int CAN_DELETE_NEWS_POSTS = 30;
	public static final int CAN_CREATE_NEWS_CATEGORIES = 29;
	public static final int CAN_DELETE_NEWS_CATEGORIES = 28;
	public static final int CAN_CREATE_NEWS_BUNDLES = 27;
	public static final int CAN_DELETE_NEWS_BUNDLES = 26;
	public static final int CAN_UPLOAD_FOLDERS = 25;
	public static final int CAN_DOWNLOAD_FOLDERS = 24;
	public static final int CAN_SEND_MESSAGES = 23;

	private long value;

	public HotlinePrivileges() {
	}
	
	public HotlinePrivileges(long value) {
		init(value);
	}
	
	public HotlinePrivileges(byte[] data) {
		try {
			init(new DataInputStream(new ByteArrayInputStream(data)).readLong());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void init(long value) {
		this.value = value;
		for (int i = 0; i < 64; i++) {
			if (hasPrivilege(i)) {
				System.err.print(i+" ");
			}
		}
		System.err.println();
	}
	
	private long flagForBit(int bit) {
		return (1L << bit);
	}
	
	public boolean hasPrivilege(int bit) {
		return (value & flagForBit(bit)) != 0;
	}

	public void setPrivilege(int bit, boolean bitValue) {
		if (bitValue) {
			value |= flagForBit(bit);
		} else {
			value &= ~flagForBit(bit);
		}
	}

	public byte[] toByteArray() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(out);
		try {
			dout.writeLong(value);
			dout.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for (int i = 0; i < 64; i++) {
			if (hasPrivilege(i)) {
				System.err.print(i+" ");
			}
		}
		System.err.println();
		return out.toByteArray();
	}
}
