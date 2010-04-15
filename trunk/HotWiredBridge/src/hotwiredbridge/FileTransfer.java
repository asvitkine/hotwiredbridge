package hotwiredbridge;

import hotwiredbridge.wired.FileInfo;

public class FileTransfer {
	private int hotlineTransferId;
	private String wiredTransferId;
	private FileInfo fileInfo;
	private boolean upload;

	public FileTransfer(int hotlineTransferId, String wiredTransferId, FileInfo fileInfo, boolean upload) {
		this.hotlineTransferId = hotlineTransferId;
		this.wiredTransferId = wiredTransferId;
		this.fileInfo = fileInfo;
		this.upload = upload;
	}

	public int getHotlineTransferId() {
		return hotlineTransferId;
	}
	public String getWiredTransferId() {
		return wiredTransferId;
	}
	public FileInfo getFileInfo() {
		return fileInfo;
	}
	public boolean isUpload() {
		return upload;
	}
	public boolean isDownload() {
		return !upload;
	}
}