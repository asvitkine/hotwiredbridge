package hotwiredbridge.hotline;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class IconReader {
	private static class Bounds {
		private int x1, y1, x2, y2;
		public Bounds(int x1, int y1, int x2, int y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}
		public int width() {
			return x2-x1;
		}
		public int height() {
			return y2-y1;
		}
	}

	private static Bounds readBounds(DataInputStream in) throws IOException {
		int y1 = in.readUnsignedShort();
		int x1 = in.readUnsignedShort();
		int y2 = in.readUnsignedShort();
		int x2 = in.readUnsignedShort();
		return new Bounds(x1, y1, x2, y2);
	}

	private final static int getARGB(byte[] colors, int alpha) {
		return (alpha << 24) | ((colors[0]&0xff) << 16) | ((colors[1]&0xff) << 8) | (colors[2]&0xff);
	}

	public static BufferedImage readIcon(InputStream stream) throws IOException {
		DataInputStream in = new DataInputStream(stream);
		in.skip(4);
		int iconRowBytes = in.readUnsignedShort() & 0x1fff;
		Bounds iconBounds = readBounds(in);
		in.skip(18);
		int bpp = in.readUnsignedShort();
		in.skip(20);
		int maskRowBytes = in.readUnsignedShort();
		Bounds maskBounds = readBounds(in);
		in.skip(4);
		int bitmapRowBytes = in.readUnsignedShort();
		Bounds bitmapBounds = readBounds(in);

		in.skip(4);
		byte[] maskData = new byte[maskRowBytes * maskBounds.height()];
		in.readFully(maskData);

		in.skip(6 + bitmapRowBytes * bitmapBounds.height());
		int ctSize = in.readUnsignedShort() + 1;

		Map<Integer, byte[]> colors = new HashMap<Integer, byte[]>(ctSize);
		for (int i = 0; i < ctSize; i++) {
			int colorIndex = in.readUnsignedShort();
			byte[] color = new byte[3];
			color[0] = (byte)(in.readUnsignedShort() >> 8);
			color[1] = (byte)(in.readUnsignedShort() >> 8);
			color[2] = (byte)(in.readUnsignedShort() >> 8);
			colors.put(colorIndex, color);
		}

		byte[] iconData = new byte[iconRowBytes * iconBounds.height()];
		in.readFully(iconData);

		int w = iconBounds.width();
		int h = iconBounds.height();
		int ppb = 8/bpp;
		int bitmask = (1 << bpp) - 1;

		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int maskShift = 7 - (x % 8);
				int mask = (maskData[y * maskRowBytes + x / 8] & (1 << maskShift)) >> maskShift;
				int shift = bpp * (ppb - (x % ppb) - 1);
				int value = (iconData[y * iconRowBytes + x / ppb] & (bitmask << shift)) >> shift;
				image.setRGB(x, y, getARGB(colors.get(value), mask*0xFF));
			}
		}

		return image;
	}
}
