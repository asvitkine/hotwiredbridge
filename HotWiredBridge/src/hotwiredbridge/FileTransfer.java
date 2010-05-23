package hotwiredbridge;

import wired.event.FileInfo;

public class FileTransfer {
	private int hotlineTransferId;
	private String wiredTransferId;
	private FileInfo fileInfo;
	private boolean upload;
	private HotWiredBridge hotWiredBridge;

	public FileTransfer(HotWiredBridge hotWiredBridge, int hotlineTransferId, String wiredTransferId,
			FileInfo fileInfo, boolean upload) {
		this.hotWiredBridge = hotWiredBridge;
		this.hotlineTransferId = hotlineTransferId;
		this.wiredTransferId = wiredTransferId;
		this.fileInfo = fileInfo;
		this.upload = upload;
	}

	public HotWiredBridge getHotWiredBridge() {
		return hotWiredBridge;
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