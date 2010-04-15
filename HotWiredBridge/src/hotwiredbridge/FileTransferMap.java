package hotwiredbridge;

import hotwiredbridge.wired.FileInfo;

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

	public synchronized FileTransfer createDownloadTransfer(String wiredTransferId, FileInfo fileInfo) {
		FileTransfer fileTransfer = new FileTransfer(nextHotlineTransferId++, wiredTransferId, fileInfo, false);
		wMap.put(wiredTransferId, fileTransfer);
		hMap.put(fileTransfer.getHotlineTransferId(), fileTransfer);
		return fileTransfer;
	}

	public synchronized FileTransfer createUploadTransfer(String wiredTransferId, FileInfo fileInfo) {
		FileTransfer fileTransfer = new FileTransfer(nextHotlineTransferId++, wiredTransferId, fileInfo, false);
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

}
