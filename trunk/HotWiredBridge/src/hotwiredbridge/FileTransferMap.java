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

	public synchronized FileTransfer createDownloadTransfer(HotWiredBridge hotWiredBridge, String wiredTransferId, FileInfo fileInfo) {
		FileTransfer fileTransfer = new FileTransfer(hotWiredBridge, nextHotlineTransferId++, wiredTransferId, fileInfo, false);
		wMap.put(wiredTransferId, fileTransfer);
		hMap.put(fileTransfer.getHotlineTransferId(), fileTransfer);
		return fileTransfer;
	}

	public synchronized FileTransfer createUploadTransfer(HotWiredBridge hotWiredBridge, FileInfo fileInfo) {
		FileTransfer fileTransfer = new FileTransfer(hotWiredBridge, nextHotlineTransferId++, null, fileInfo, true);
		hMap.put(fileTransfer.getHotlineTransferId(), fileTransfer);
		return fileTransfer;
	}

	public FileTransfer setWiredTransferIdForUploadTransfer(FileTransfer transfer, String wiredTransferId) {
		if (!transfer.isUpload())
			return null;
		FileTransfer newFileTransfer;
		synchronized(this) {
			newFileTransfer = new FileTransfer(transfer.getHotWiredBridge(),transfer.getHotlineTransferId(),
				wiredTransferId, transfer.getFileInfo(), false);
			wMap.put(wiredTransferId, newFileTransfer);
			hMap.put(newFileTransfer.getHotlineTransferId(), newFileTransfer);
			synchronized(transfer) {
				transfer.notify();
			}
		}
		return newFileTransfer;
	}

	public FileTransfer waitForWiredTransferIdForUploadTransfer(FileTransfer transfer) throws InterruptedException {
		if (!transfer.isUpload()) {
			return null;
		}
		synchronized(transfer) {
			do {
				FileTransfer newFileTransfer;
				synchronized(this) {
					newFileTransfer = hMap.get(transfer.getHotlineTransferId());
				}
				if (newFileTransfer != null && newFileTransfer.getWiredTransferId() != null) {
					return newFileTransfer;
				}
				transfer.wait();
			} while (true);
		}
	}


	public synchronized FileTransfer getTransferByWiredId(String wiredTransferId) {
		return wMap.get(wiredTransferId);
	}

	public synchronized FileTransfer getTransferByHotlineId(int hotlineTransferId) {
		return hMap.get(hotlineTransferId);
	}
}
