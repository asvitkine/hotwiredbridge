package hotwiredbridge.hotline;

import wired.WiredUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Transaction {
	public static final int REQUEST = 0;
	public static final int REPLY = 1;

	public static final int ID_GETNEWS = 101;
	public static final int ID_NEWS_POSTED = 102;
	public static final int ID_POST_NEWS = 103;
	public static final int ID_PRIVATE_MESSAGE = 104;
	public static final int ID_SEND_CHAT = 105;
	public static final int ID_RELAY_CHAT = 106;
	public static final int ID_LOGIN = 107;
	public static final int ID_SEND_PM = 108;

	public static final int ID_CREATE_PCHAT = 112;
	public static final int ID_INVITE_TO_PCHAT = 113;
	public static final int ID_REJECT_PCHAT = 114;
	public static final int ID_JOIN_PCHAT = 115;
	public static final int ID_LEAVING_PCHAT = 116;
	public static final int ID_JOINED_PCHAT = 117;
	public static final int ID_LEFT_PCHAT = 118;
	public static final int ID_CHANGED_SUBJECT = 119;
	public static final int ID_REQUEST_CHANGE_SUBJECT = 120;
	public static final int ID_AGREE = 121;

	public static final int ID_GETFOLDERLIST = 200;
	public static final int ID_DOWNLOAD = 202;
	public static final int ID_UPLOAD = 203;
	public static final int ID_CREATEFOLDER = 205;
	public static final int ID_GETFILEINFO = 206;

	public static final int ID_GETUSERLIST = 300;
	public static final int ID_USERCHANGE = 301;
	public static final int ID_USERLEAVE = 302;
	public static final int ID_GETUSERINFO = 303;
	public static final int ID_CHANGE_NICK = 304;
	public static final int ID_USERLIST = 354;
	public static final int ID_BROADCAST = 355;

	private int type;
	private int id;
	private int taskNumber;
	private boolean isError;
	private List<TransactionObject> objects;
	
	public Transaction(int type, int id, int taskNumber, boolean isError) {
		this.type = type;
		this.id = id;
		this.taskNumber = taskNumber;
		this.isError = isError;
		this.objects = new ArrayList<TransactionObject>();
	}

	public void addObject(int id, byte[] data) {
		objects.add(new TransactionObject(id, data));
	}
	
	private TransactionObject findObject(int id) {
		for (TransactionObject object : objects) {
			if (object.id == id) {
				return object;
			}
		}
		return null;
	}
	
	public byte[] getObjectData(int id) {
		TransactionObject object = findObject(id);
		return (object != null ? object.data : null);
	}
	
	public Integer getObjectDataAsInt(int id) {
		byte[] data = getObjectData(id);
		if (data != null) {
			if (data.length == 2) {
				return ((Number)HotlineUtils.unpack("n", data)[0]).intValue();
			} else if (data.length == 4) {
				return ((Number)HotlineUtils.unpack("N", data)[0]).intValue();
			}
		}
		return null;
	}
	
	
	public String toString() {
		String s = String.format("%d %d %d with %d objecs:\n", type, id, taskNumber, objects.size());
		for (TransactionObject o : objects) {
			s += "  obj(" + o.id +") -> " + (o.data == null ? "(null)" : WiredUtils.bytesToHex(o.data)) + " \n";
		}
		return s;
	}
	
	private Transaction() {
		
	}
	
	public static Transaction readFromStream(DataInputStream in) throws IOException {
		Transaction transaction = new Transaction();
		transaction.type = in.readShort();
		transaction.id = in.readShort();
		transaction.taskNumber = in.readInt();
		transaction.isError = (in.readInt() != 0);
		transaction.objects = new ArrayList<TransactionObject>();
		int length1 = in.readInt();
		int length2 = in.readInt();
		if (length1 != length2) {
			throw new RuntimeException("invalid transaction header read");
		}
		if (length1 != 0) {
			int numObjects = in.readShort();
			for (int i = 0; i < numObjects; i++) {
				TransactionObject o = new TransactionObject();
				o.id = in.readShort();
				o.data = new byte[in.readShort()];
				in.read(o.data);
				transaction.objects.add(o);
			}
		}
		return transaction;
	}

	public static void writeToStream(Transaction transaction, DataOutputStream out) throws IOException {
		out.writeShort(transaction.type);
		out.writeShort(transaction.id);
		out.writeInt(transaction.taskNumber);
		out.writeInt(transaction.isError ? 1 : 0);
		if (transaction.objects.size() == 0) {
			out.writeInt(2);
			out.writeInt(2);
			out.writeShort(0);
		} else {
			int length = 0;
			for (TransactionObject object : transaction.objects) {
				length += object.data.length + 4;
			}
			out.writeInt(length + 2);
			out.writeInt(length + 2);
			out.writeShort(transaction.objects.size());
			for (TransactionObject object : transaction.objects) {
				out.writeShort(object.id);
				out.writeShort(object.data.length);
				out.write(object.data);
			}
		}
		out.flush();
	}

	public int getType() {
		return type;
	}
	
	public int getId() {
		return id;
	}
	
	public int getTaskNumber() {
		return taskNumber;
	}
}
