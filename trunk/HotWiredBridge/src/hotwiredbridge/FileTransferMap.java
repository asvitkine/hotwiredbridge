package hotwiredbridge;

import java.util.Map;
import java.util.HashMap;

public class FileTransferMap {
	private Map<String, FileTransfer> wMap;
	private Map<Integer, FileTransfer> hMap;
	private int nextHotlineTransferId;

	public FileTransferMap() {
		wMap = new HashMap<String, FileTransfer>();
		hMap = new HashMap<Integer, FileTransfer>();
		nextHotlineTransferId = 1;
	}

	public synchronized FileTransfer createDownloadTransfer(String wiredTransferId, String path) {
		FileTransfer fileTransfer = new FileTransfer(nextHotlineTransferId++, wiredTransferId, path, false);
		wMap.put(wiredTransferId, fileTransfer);
		hMap.put(fileTransfer.getHotlineTransferId(), fileTransfer);
		return fileTransfer;
	}

	public synchronized FileTransfer createUploadTransfer(String wiredTransferId, String path) {
		FileTransfer fileTransfer = new FileTransfer(nextHotlineTransferId++, wiredTransferId, path, false);
		wMap.put(wiredTransferId, fileTransfer);
		hMap.put(fileTransfer.getHotlineTransferId(), fileTransfer);
		return fileTransfer;
	}

	public synchronized FileTransfer getTransferByWiredId(String wiredTransferId) {
		return wMap.get(wiredTransferId);
	}

	public synchronized FileTransfer getTransferByHotlineId(int hotlineTransferId) {
		return hMap.get(hotlineTransferId);
	}

	public static class FileTransfer {
		private int hotlineTransferId;
		private String wiredTransferId;
		private String path;
		private boolean upload;

		private FileTransfer(int hotlineTransferId, String wiredTransferId, String path, boolean upload) {
			this.hotlineTransferId = hotlineTransferId;
			this.wiredTransferId = wiredTransferId;
			this.path = path;
			this.upload = upload;
		}

		public int getHotlineTransferId() {
			return hotlineTransferId;
		}
		public String getWiredTransferId() {
			return wiredTransferId;
		}
		public String getPath() {
			return path;
		}
		public boolean isUpload() {
			return upload;
		}
		public boolean isDownload() {
			return !upload;
		}
	}
}
