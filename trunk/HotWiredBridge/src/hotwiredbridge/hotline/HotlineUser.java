package hotwiredbridge.hotline;

public class HotlineUser {
	private int socket;
	private int icon;
	private int status;
	private String nick;

	public HotlineUser(int socket, int icon, int status, String nick) {
		this.socket = socket;
		this.icon = icon;
		this.status = status;
		this.nick = nick;
	}

	public byte[] toByteArray() {
		return HotlineUtils.pack("nnnnB", socket, icon, status, nick.length(), nick.getBytes());
	}
}
