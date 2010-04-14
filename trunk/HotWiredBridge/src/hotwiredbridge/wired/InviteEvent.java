package hotwiredbridge.wired;

public class InviteEvent extends WiredEvent {
	private long userId;
	private long chatId;

	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public long getChatId() {
		return chatId;
	}
	public void setChatId(long chatId) {
		this.chatId = chatId;
	}	
}
