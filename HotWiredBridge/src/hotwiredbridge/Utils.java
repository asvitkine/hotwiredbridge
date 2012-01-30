package hotwiredbridge;

import java.io.*;

public class Utils {
	public static byte[] fileToByteArray(File file) throws IOException {
		long length = file.length();
		if (length == 0)
			return new byte[0];

		int offset = 0;
		InputStream in = new FileInputStream(file);
		byte[] bytes = new byte[(int) length];

		while (offset < bytes.length) {
			int nread = in.read(bytes, offset, bytes.length-offset);
			if (nread < 0)
				break;
			offset += nread;
		}

		if (offset < bytes.length)
			throw new IOException("Error reading file '" + file.getName() + "' completely.");
		in.close();

		return bytes;
	}
}
