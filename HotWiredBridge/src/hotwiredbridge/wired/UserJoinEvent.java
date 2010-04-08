package hotwiredbridge.wired;

public class UserJoinEvent extends WiredEvent {
	private int chatId;
	private User user;

	public int getChatId() {
		return chatId;
	}
	public void setChatId(int chatId) {
		this.chatId = chatId;
	}

	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
}
