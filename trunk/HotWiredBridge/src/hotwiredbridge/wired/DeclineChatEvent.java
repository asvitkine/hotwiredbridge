package hotwiredbridge.wired;

public class DeclineChatEvent extends WiredEvent {
	private long chatId;
	private long userId;

	public long getChatId() {
		return chatId;
	}

	public void setChatId(long chatId) {
		this.chatId = chatId;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}
}
