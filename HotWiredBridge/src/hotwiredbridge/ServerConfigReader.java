package hotwiredbridge;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfigReader {
	public static ServerConfig read(String path) throws IOException {
		ServerConfig config = new ServerConfig();
		Properties properties = new Properties();
		FileInputStream in = new FileInputStream(path);
		try {
			properties.load(in);
			config.setWiredHost(properties.getProperty("WIRED_HOST"));
			config.setWiredPort(Integer.valueOf(properties.getProperty("WIRED_PORT")));
			config.setIconsFilePath(properties.getProperty("ICON_FILE_PATH"));
			config.setParallelIconLoading(Boolean.valueOf(properties.getProperty("ICON_FILE_PARALLEL_LOAD")));
			config.setAppleVolumesFilePath(properties.getProperty("APPLE_VOLUMES_FILE_PATH"));
			config.setCertificatesFilePath(properties.getProperty("CERTIFICATES_FILE_PATH"));
			config.setCertificatesFilePassphrase(properties.getProperty("CERTIFICATES_FILE_PASSPHRASE"));
			config.setHotlinePort(Integer.valueOf(properties.getProperty("HOTLINE_PORT")));
			config.setIgnoreSigTerm(Boolean.valueOf(properties.getProperty("IGNORE_SIGTERM")));
		} finally {
			in.close();
		}
		return config;
	}
}
