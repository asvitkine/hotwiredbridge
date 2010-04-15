package hotwiredbridge.wired;

public class TransferReadyEvent extends WiredEvent {
	private String path;
	private long offset;
	private String hash;

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public long getOffset() {
		return offset;
	}
	public void setOffset(long offset) {
		this.offset = offset;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
}
