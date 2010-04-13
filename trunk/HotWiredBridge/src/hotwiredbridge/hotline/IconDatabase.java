package hotwiredbridge.hotline;

import hotwiredbridge.wired.WiredClient;

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
		int iconsLoaded = 0;
		int iconsTotal = 0;
		for (Resource r : icons.getResArray()) {
			try {
				BufferedImage icon = IconReader.readIcon(new ByteArrayInputStream(r.getData()));
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageIO.write(icon, "png", out);
				Integer iconIndex = new Integer(r.getID());
				String iconString = WiredClient.bytesToBase64(out.toByteArray()); 
				iconStrings.put(iconIndex, iconString);
				stringIcons.put(iconString, iconIndex);
				iconsLoaded++;
			} catch (IOException e) {
				e.printStackTrace();
			}
			iconsTotal++;
		}
		
		System.out.printf("INFO: %d / %d icons loaded.\n", iconsLoaded, iconsTotal);
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
