package hotwiredbridge.wired;

import java.util.List;

public class FileListEvent extends WiredEvent {
	private List<FileInfo> files;

	public List<FileInfo> getFiles() {
		return files;
	}

	public void setFiles(List<FileInfo> files) {
		this.files = files;
	}
}
