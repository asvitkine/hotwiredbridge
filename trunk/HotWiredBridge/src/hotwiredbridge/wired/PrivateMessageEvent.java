package hotwiredbridge.wired;

public class PrivateMessageEvent extends WiredEvent {
	private long fromUserId;
	private String message;

	public long getFromUserId() {
		return fromUserId;
	}
	public void setFromUserId(long fromUserId) {
		this.fromUserId = fromUserId;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
