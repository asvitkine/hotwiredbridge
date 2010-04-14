package hotwiredbridge.wired;

public class FileInfo {
	public static final int TYPE_FILE = 0;
	public static final int TYPE_FOLDER = 1;
	public static final int TYPE_UPLOADS_FOLDER = 2;
	public static final int TYPE_DROPBOX_FOLDER = 3;

	private String path;
	private String name;
	private long type;
	private long size;
	private long creationDate;
	private long modificationDate;
	private String icon;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public long getType() {
		return type;
	}
	public void setType(long type) {
		this.type = type;
	}
	public boolean isDirectory() {
		return type != TYPE_FILE;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public long getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}
	public long getModificationDate() {
		return modificationDate;
	}
	public void setModificationDate(long modificationDate) {
		this.modificationDate = modificationDate;
	}
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
}
