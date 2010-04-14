package hotwiredbridge.wired;

public class UserJoinEvent extends WiredEvent {
	private long chatId;
	private User user;

	public long getChatId() {
		return chatId;
	}
	public void setChatId(long chatId) {
		this.chatId = chatId;
	}

	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
}
