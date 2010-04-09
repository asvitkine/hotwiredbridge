package hotwiredbridge.wired;

public class UserStatusChangeEvent extends WiredEvent {
	private User user;

	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
}
