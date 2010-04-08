package hotwiredbridge.hotline;

public class UserListTransaction extends Transaction {

	public UserListTransaction(int taskNumber) {
		super(REPLY, ID_GETUSERLIST, taskNumber, false);
	}

	public void addUser(User user) {
		addObject(new TransactionObject(TransactionObject.USER, user.toByteArray()));
	}
	
	public static class User {
		private int socket;
		private int icon;
		private int status;
		private String nick;

		public User(int socket, int icon, int status, String nick) {
			this.socket = socket;
			this.icon = icon;
			this.status = status;
			this.nick = nick;
		}

		public byte[] toByteArray() {
			return HotlineUtils.pack("nnnnB", socket, icon, status, nick.length(), nick.getBytes());
		}
	}
}
