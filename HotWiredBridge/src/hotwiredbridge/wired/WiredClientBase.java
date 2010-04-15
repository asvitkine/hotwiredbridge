package hotwiredbridge.wired;

import java.io.IOException;
import java.io.OutputStream;

public class WiredClientBase {
	public static final byte EOT = 4;
	public static final byte FS = 28;
	public static final byte GS = 29;
	public static final byte RS = 30;
	public static final byte SP = 32;

	protected OutputStream out;

	public WiredClientBase(OutputStream out) {
		this.out = out;
	}
	
	protected WiredClientBase send(String str) throws IOException {
		out.write(str.getBytes("UTF-8"));
		return this;
	}

	protected WiredClientBase send(boolean b) throws IOException {
		return send(b ? "1" : "0");
	}

	protected WiredClientBase send(byte b) throws IOException {
		out.write(b);
		return this;
	}
}
