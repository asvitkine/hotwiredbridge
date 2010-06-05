package hotwiredbridge.hotline;

import java.io.*;
import java.util.*;

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
				} else if (c == 'D') {
					out.writeShort(1970);
					out.writeShort(0);
					long millis = ((Number)args[i]).longValue();
					out.writeInt((int)(millis / 1000));
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

	public static Object[] unpack(String format, byte[] data) {
		List<Object> objects = new ArrayList<Object>();
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		DataInputStream in = new DataInputStream(stream);
		try {
			for (int i = 0; i < format.length(); i++) {
				char c = format.charAt(i);
				if (c == 'n') {
					objects.add(new Integer(in.readShort()));
				} else if (c == 'N') {
					objects.add(new Integer(in.readInt()));
				} else {
					throw new IllegalArgumentException();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return objects.toArray();
	}

	public static String readTag(DataInputStream in) throws IOException {
		byte[] tag = new byte[4];
		in.readFully(tag);
		return new String(tag);
	}
}
