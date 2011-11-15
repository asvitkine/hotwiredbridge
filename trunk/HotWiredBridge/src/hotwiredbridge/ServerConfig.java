package hotwiredbridge;

public class ServerConfig {
	private int hotlinePort;
	private String wiredHost;
	private int wiredPort;
	private String certificatesFilePath;
	private String certificatesFilePassphrase;
	private String iconsFilePath;
	private boolean parallelIconLoading;
	private String appleVolumesFilePath;
	private boolean ignoreSigTerm;

	public String getAppleVolumesFilePath() {
		return appleVolumesFilePath;
	}
	public void setAppleVolumesFilePath(String appleVolumesFilePath) {
		this.appleVolumesFilePath = appleVolumesFilePath;
	}
	public int getHotlinePort() {
		return hotlinePort;
	}
	public void setHotlinePort(int hotlinePort) {
		this.hotlinePort = hotlinePort;
	}
	public String getWiredHost() {
		return wiredHost;
	}
	public void setWiredHost(String wiredHost) {
		this.wiredHost = wiredHost;
	}
	public int getWiredPort() {
		return wiredPort;
	}
	public void setWiredPort(int wiredPort) {
		this.wiredPort = wiredPort;
	}
	public String getCertificatesFilePath() {
		return certificatesFilePath;
	}
	public void setCertificatesFilePath(String certificatesFilePath) {
		this.certificatesFilePath = certificatesFilePath;
	}
	public String getCertificatesFilePassphrase() {
		return certificatesFilePassphrase;
	}
	public void setCertificatesFilePassphrase(String certificatesFilePassphrase) {
		this.certificatesFilePassphrase = certificatesFilePassphrase;
	}
	public String getIconsFilePath() {
		return iconsFilePath;
	}
	public void setIconsFilePath(String iconsFilePath) {
		this.iconsFilePath = iconsFilePath;
	}
	public boolean getIgnoreSigTerm() {
		return ignoreSigTerm;
	}
	public void setIgnoreSigTerm(boolean ignoreSigTerm) {
		this.ignoreSigTerm = ignoreSigTerm;
	}
	public void setParallelIconLoading(boolean parallelIconLoading) {
		this.parallelIconLoading = parallelIconLoading;
	}
	public boolean getParallelIconLoading() {
		return parallelIconLoading;
	}
}
