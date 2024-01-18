package tools;

import java.awt.Desktop;
import java.net.URL;

public class Tools {

	static public boolean help() {
		String url = "https://segment-anything.com"; 
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(new URL(url).toURI());
				return true;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
}
