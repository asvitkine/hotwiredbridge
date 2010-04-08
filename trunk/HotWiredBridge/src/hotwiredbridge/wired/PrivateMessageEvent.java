package hotwiredbridge.wired;

public class PrivateMessageEvent extends WiredEvent {
	private int fromUserId;
	private String message;

	public int getFromUserId() {
		return fromUserId;
	}
	public void setFromUserId(int fromUserId) {
		this.fromUserId = fromUserId;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
