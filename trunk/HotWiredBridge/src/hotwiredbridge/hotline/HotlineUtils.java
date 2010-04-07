package hotwiredbridge.hotline;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HotlineUtils {
	
	public static byte[] pack(String format, Object... args) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		try {
			for (int i = 0; i < format.length(); i++) {
				char c = format.charAt(i);
				if (c == 'n') {
					out.writeShort(((Number)args[i]).shortValue());
				} else if (c == 'N') {
					out.writeInt(((Number)args[i]).intValue());
				} else if (c == 'B') {
					out.write((byte[])args[i]);
				} else {
					throw new IllegalArgumentException();
				}
			}
			out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return stream.toByteArray();
	}
}
