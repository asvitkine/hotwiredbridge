package hotwiredbridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class FileTypeAndCreatorMap {
	public static final int TYPE = 0;
	public static final int CREATOR = 1;

	private Map<String, String[]> map;

	public FileTypeAndCreatorMap() {
		map = new HashMap<String, String[]>();		
	}

	public String[] getCodesFor(String filename) {
		int index = filename.lastIndexOf('.');
		String ext;
		if (index != -1) {
			ext = filename.substring(index + 1);
		} else {
			ext = "";
		}
		String[] codes = map.get(ext);
		if (codes != null) {
			return new String[] {codes[0], codes[1]};
		}
		return new String[] {"????", "????"};
	}

	public void loadFromAppleVolumesFile(File file) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String line = in.readLine();
		while (line != null) {
			line = line.trim();
			if (!line.startsWith("#") && line.length() > 0) {
				SimpleScanner ss = new SimpleScanner(line);
				String ext = ss.readNext();
				String type = ss.readNext();
				String creator = ss.readNext();
				map.put(ext.substring(1), new String[] {type, creator});
			}
			line = in.readLine();
		}

		System.out.printf("INFO: %d mappings loaded.\n", map.size());
	}

	public static class SimpleScanner {
		private String string;
		private int index;

		public SimpleScanner(String string) {
			this.string = string;
			this.index = 0;
		}

		private void skipAll(char c) {
			while (index < string.length() && string.charAt(index) == c) {
				index++;
			}
		}

		private void skipUntil(char c) {
			while (index < string.length() && string.charAt(index) != c) {
				index++;
			}
		}

		public String readNext() {
			if (index == string.length()) {
				return null;
			}

			int start = index;
			String result;
			if (string.charAt(index) == '"') {
				index++;
				skipUntil('"');
				result = string.substring(start + 1, index - 1);
			} else {
				skipUntil(' ');
				result = string.substring(start, index);				
			}
			skipAll(' ');
			return result;
		}
	}
}
