package hotwiredbridge.hotline;

import java.io.*;

public class HotlinePrivileges {
	public static final int CAN_GET_USER_INFO = 31;
	public static final int CAN_UPLOAD_ANYWHERE = 30;
	public static final int CAN_USE_ANY_NAME = 29;
	public static final int DONT_SHOW_AGREEMENT = 28;
	public static final int CAN_COMMENT_FILES = 27;
	public static final int CAN_COMMENT_FOLDERS = 26;
	public static final int CAN_VIEW_DROP_BOXES = 25;
	public static final int CAN_MAKE_ALIASES = 24;
	public static final int CAN_READ_USERS = 23;
	public static final int CAN_MODIFY_USERS = 22;
	public static final int CAN_READ_NEWS = 19;
	public static final int CAN_POST_NEWS = 18;
	public static final int CAN_DISCONNECT_USERS = 17;
	public static final int CANNOT_BE_DISCONNECTED = 16;
	public static final int CAN_MOVE_FOLDERS = 15;
	public static final int CAN_READ_CHAT = 14;
	public static final int CAN_SEND_CHAT = 13;
	public static final int CAN_CREATE_USERS = 9;
	public static final int CAN_DELETE_USERS = 8;
	public static final int CAN_DELETE_FILES = 7;
	public static final int CAN_UPLOAD_FILES = 6;
	public static final int CAN_DOWNLOAD_FILES = 5;
	public static final int CAN_RENAME_FILES = 4;
	public static final int CAN_MOVE_FILES = 3;
	public static final int CAN_CREATE_FOLDERS = 2;
	public static final int CAN_DELETE_FOLDERS = 1;
	public static final int CAN_RENAME_FOLDERS = 0;

	private long value;

	public HotlinePrivileges() {
	}
	
	public HotlinePrivileges(byte[] data) {
		try {
			value = new DataInputStream(new ByteArrayInputStream(data)).readLong();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean hasPrivilege(int bit) {
		return (value & (1L << (63-bit))) != 0;
	}
	
	public void setPrivilege(int bit, boolean bitValue) {
		if (bitValue) {
			value |= (1L << (63-bit));
		} else {
			value &= ~(1L << (63-bit));
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
		return out.toByteArray();
	}
}
