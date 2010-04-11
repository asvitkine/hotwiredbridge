package hotwiredbridge.wired;

public class FileInfoEvent extends WiredEvent {
	private FileInfo fileInfo;
	private String checksum;
	private String comment;

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public FileInfo getFileInfo() {
		return fileInfo;
	}

	public void setFileInfo(FileInfo fileInfo) {
		this.fileInfo = fileInfo;
	}
}
