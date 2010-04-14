package hotwiredbridge.wired;

public class ChatCreatedEvent extends WiredEvent {
	private long chatId;

	public long getChatId() {
		return chatId;
	}

	public void setChatId(long chatId) {
		this.chatId = chatId;
	}
}
