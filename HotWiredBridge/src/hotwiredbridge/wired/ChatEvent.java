package hotwiredbridge.wired;

public class ChatEvent extends WiredEvent {
	private long userId;
	private long chatId;
	private boolean emote;
	private String message;

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
