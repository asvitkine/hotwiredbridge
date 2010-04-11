package hotwiredbridge.hotline;

import hotwiredbridge.wired.WiredClient;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.freeshell.gbsmith.rescafe.MacBinaryHeader;
import org.freeshell.gbsmith.rescafe.cicnParser;
import org.freeshell.gbsmith.rescafe.resourcemanager.*;

public class IconDatabase {
	private Map<Integer, String> iconStrings;
	private Map<String, Integer> stringIcons;

	public IconDatabase() {
		iconStrings = new HashMap<Integer, String>();
		stringIcons = new HashMap<String, Integer>();
	}
	
	public void loadIconsFromResourceFile(File file) throws IOException {
		ResourceModel model = loadResources(file);
		ResourceType icons = model.getResourceType("cicn");
		for (Resource r : icons.getResArray()) {
			try {
				cicnParser parser = new cicnParser();
				parser.read(new ByteArrayInputStream(r.getData()));
				BufferedImage icon = createBufferedImageFromImage(parser.getIcon());
				BufferedImage mask = createBufferedImageFromImage(parser.getMask());
				applyMask(icon, mask);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageIO.write(icon, "png", out);
				Integer iconIndex = new Integer(r.getID());
				String iconString = WiredClient.bytesToBase64(out.toByteArray()); 
				iconStrings.put(iconIndex, iconString);
				stringIcons.put(iconString, iconIndex);
			} catch (Exception e) {
				//e.printStackTrace();
			}
		}
	}
	
	public String getBase64FromIconIndex(int icon) {
		return iconStrings.get(icon);
	}
	
	public int getIconIndexFromBase64(String base64) {
		Integer icon = stringIcons.get(base64);
		return icon == null ? 0 : icon;
	}

	private static BufferedImage createBufferedImageFromImage(Image image) {
		int w = image.getWidth(null);
		int h = image.getHeight(null);
		BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bufferedImage.createGraphics();
		g.drawImage(image, 0, 0, w, h, null);
		g.dispose();
		return bufferedImage;
	}
	
	private static void applyMask(BufferedImage image, BufferedImage mask) {
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				if (mask.getRGB(x, y) == -1) {
					image.setRGB(x, y, 0);
				}
			}
		}
	}

	private static ResourceModel loadResources(File file) throws IOException {
		ResourceModel model = new ResourceModel(file.getName());
		RandomAccessFile raf = new RandomAccessFile(file.getPath(), "r");
		MacBinaryHeader mbh = new MacBinaryHeader();
		mbh.read(raf);
		if (mbh.validate()) {
			raf.seek(mbh.getResForkOffset());
			model.read(raf, mbh.getResForkOffset());
		} else {
			raf = new RandomAccessFile(file.getPath() + "/rsrc", "r");
			model.read(raf);
		}
		return model;
	}
}
