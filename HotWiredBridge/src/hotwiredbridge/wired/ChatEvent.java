package hotwiredbridge.wired;

public class ChatEvent extends WiredEvent {
	private int userId;
	private int chatId;
	private boolean emote;
	private String message;

	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getChatId() {
		return chatId;
	}
	public void setChatId(int chatId) {
		this.chatId = chatId;
	}

	public boolean isEmote() {
		return emote;
	}
	public void setEmote(boolean emote) {
		this.emote = emote;
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
