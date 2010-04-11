package hotwiredbridge.hotline;

public class TransactionObject {
	public static final int ERROR_MSG = 100;
	public static final int MESSAGE = 101;
	public static final int NICK = 102;
	public static final int SOCKET = 103;
	public static final int ICON = 104;
	public static final int LOGIN = 105;
	public static final int PASSWORD = 106;
	public static final int PARAMETER = 109;
	public static final int PRIVS = 110;
	public static final int STATUS = 112;
	public static final int STATUS_FLAGS = 113;
	public static final int CHATWINDOW = 114;
	public static final int SUBJECT = 115;
	public static final int VERSION = 160;
	public static final int FILE_ENTRY = 200;
	public static final int FILENAME = 201;
	public static final int FILETYPESTR = 205;
	public static final int FILECREATORSTR = 206;
	public static final int FILESIZE = 207;
	public static final int FILEMODIFIED = 208;
	public static final int FILECREATED = 209;
	public static final int COMMENT = 210;
	public static final int FILETYPE = 213;
	public static final int PATH = 202;
	public static final int SERVER_NAME = 162;
	public static final int USER = 300;

	int id;
	byte[] data;
	
	public TransactionObject(int id, byte[] data) {
		this.id = id;
		this.data = data;
	}
	
	public TransactionObject() {
		
	}
}