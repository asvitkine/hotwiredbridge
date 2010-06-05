package hotwiredbridge;

import wired.event.FileInfo;

public class FileTransfer {
	private int hotlineTransferId;
	private String wiredTransferId;
	private FileInfo fileInfo;
	private long fileOffset;
	private boolean upload;
	private String wiredChecksum;
	private HotWiredBridge hotWiredBridge;

	public FileTransfer(HotWiredBridge hotWiredBridge, int hotlineTransferId, String wiredTransferId,
			FileInfo fileInfo, long fileOffset, boolean upload, String wiredChecksum) {
		this.hotWiredBridge = hotWiredBridge;
		this.hotlineTransferId = hotlineTransferId;
		this.wiredTransferId = wiredTransferId;
		this.fileInfo = fileInfo;
		this.fileOffset = fileOffset;
		this.upload = upload;
		this.wiredChecksum = wiredChecksum;
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
	public boolean isPartial() {
		return fileOffset != 0;
	}
	public String getWiredChecksum() {
		return wiredChecksum;
	}
	public long getFileOffset() {
		return fileOffset;
	}
	public boolean isUpload() {
		return upload;
	}
	public boolean isDownload() {
		return !upload;
	}
}