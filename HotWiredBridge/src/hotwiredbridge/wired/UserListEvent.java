package hotwiredbridge.wired;

import java.util.List;

public class UserListEvent extends WiredEvent {
	private long chatId;
	private List<User> users;

	public long getChatId() {
		return chatId;
	}
	public void setChatId(long chatId) {
		this.chatId = chatId;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}
	public List<User> getUsers() {
		return users;
	}
}
