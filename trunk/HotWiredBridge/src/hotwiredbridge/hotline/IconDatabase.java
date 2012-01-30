package hotwiredbridge.hotline;

import wired.WiredUtils;

import hotwiredbridge.ServerApp;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.freeshell.gbsmith.rescafe.MacBinaryHeader;
import org.freeshell.gbsmith.rescafe.resourcemanager.*;

public class IconDatabase {
	private Map<Integer, String> iconStrings;
	private Map<String, Integer> stringIcons;

	public IconDatabase() {
		iconStrings = new HashMap<Integer, String>();
		stringIcons = new HashMap<String, Integer>();
	}
	
	public void loadIconsFromResourceFile(File file, ExecutorService pool) throws IOException {
		ResourceModel model = loadResources(file);
		ResourceType icons = model.getResourceType("cicn");
		final int[] iconStats = new int[2];
		for (final Resource r : icons.getResArray()) {
			iconStats[1]++;
			pool.execute(new Runnable() {
				public void run() {
					try {
						BufferedImage icon = IconReader.readIcon(new ByteArrayInputStream(r.getData()));
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						ImageIO.write(icon, "png", out);
						Integer iconIndex = new Integer(r.getID());
						String iconString = WiredUtils.bytesToBase64(out.toByteArray());
						synchronized (IconDatabase.this) {
							iconStrings.put(iconIndex, iconString);
							stringIcons.put(iconString, iconIndex);
							iconStats[0]++;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}

		try {
			pool.shutdown();
			pool.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ServerApp.log("INFO: %d / %d icons loaded.", iconStats[0], iconStats[1]);
	}
	
	public String getBase64FromIconIndex(int icon) {
		return iconStrings.get(icon);
	}
	
	public int getIconIndexFromBase64(String base64) {
		Integer icon = stringIcons.get(base64);
		return icon == null ? 0 : icon;
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
