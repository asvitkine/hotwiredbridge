package hotwiredbridge.wired;

public class ChatCreatedEvent extends WiredEvent {
	private int chatId;

	public int getChatId() {
		return chatId;
	}

	public void setChatId(int chatId) {
		this.chatId = chatId;
	}
}
