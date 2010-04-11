package hotwiredbridge.wired;

public class WiredErrorEvent extends WiredEvent {
	private String message;
	private int errorCode;

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}
}
