package hotwiredbridge.wired;

import java.util.List;

public class UserListEvent extends WiredEvent {
	private int chatId;
	private List<User> users;

	public int getChatId() {
		return chatId;
	}
	public void setChatId(int chatId) {
		this.chatId = chatId;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}
	public List<User> getUsers() {
		return users;
	}
}
