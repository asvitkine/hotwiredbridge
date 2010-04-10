package hotwiredbridge.wired;

import java.util.List;

public class FileListEvent extends WiredEvent {
	private String path;
	private List<FileInfo> files;

	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public List<FileInfo> getFiles() {
		return files;
	}

	public void setFiles(List<FileInfo> files) {
		this.files = files;
	}
}
