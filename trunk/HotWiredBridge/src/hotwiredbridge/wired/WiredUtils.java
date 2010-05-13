package hotwiredbridge.wired;

import java.security.MessageDigest;

public class WiredUtils {
	public static String SHA1(String text) {
		try {
			return SHA1(text.getBytes("iso-8859-1"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String SHA1(byte[] bytes) {
		try {
			MessageDigest d = MessageDigest.getInstance("SHA-1");
			d.update(bytes, 0, bytes.length);
			return bytesToHex(d.digest());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static final char[] HEX_CHARS = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};
	public static String bytesToHex(byte[] bytes) {
		StringBuilder buf = new StringBuilder(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];
			buf.append(HEX_CHARS[(b & 0xF0) >> 4]);
			buf.append(HEX_CHARS[b & 0x0F]);
		}
		return buf.toString();
	}

	private static final char[] BASE64_CHARS = {
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
		'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
		'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
		'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
	};
	public static String bytesToBase64(byte[] bytes) {
		int numFullGroups = bytes.length / 3;
		int numBytesInPartialGroup = bytes.length - 3 * numFullGroups;
		int resultLen = 4*((bytes.length + 2)/3);
		StringBuilder sb = new StringBuilder(resultLen);

		// Translate all full groups from byte array elements to Base64
		int inCursor = 0;
		for (int i = 0; i < numFullGroups; i++) {
			int byte0 = bytes[inCursor++] & 0xff;
			int byte1 = bytes[inCursor++] & 0xff;
			int byte2 = bytes[inCursor++] & 0xff;
			sb.append(BASE64_CHARS[byte0 >> 2]);
			sb.append(BASE64_CHARS[(byte0 << 4)&0x3f | (byte1 >> 4)]);
			sb.append(BASE64_CHARS[(byte1 << 2)&0x3f | (byte2 >> 6)]);
			sb.append(BASE64_CHARS[byte2 & 0x3f]);
		}

		// Translate partial group if present
		if (numBytesInPartialGroup != 0) {
			int byte0 = bytes[inCursor++] & 0xff;
			sb.append(BASE64_CHARS[byte0 >> 2]);
			if (numBytesInPartialGroup == 1) {
				sb.append(BASE64_CHARS[(byte0 << 4) & 0x3f]);
				sb.append("==");
			} else {
				int byte1 = bytes[inCursor++] & 0xff;
				sb.append(BASE64_CHARS[(byte0 << 4)&0x3f | (byte1 >> 4)]);
				sb.append(BASE64_CHARS[(byte1 << 2)&0x3f]);
				sb.append('=');
			}
		}
		return sb.toString();
	}
}
