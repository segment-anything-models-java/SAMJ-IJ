package sc.fiji.samj.gui.tools;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

public class Files {

	public static String getWorkingDirectory() {
		return System.getProperty("user.dir");
	}

	public static String getHomeDirectory() {
		return FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath() + File.separator;	
	}
	
	public static String getDesktopDirectory() {
		return getHomeDirectory() + "Desktop" + File.separator;
	}
	
	public static File browseFile(String path) {
		JFileChooser fc = new JFileChooser(); 
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		File dir = new File(path);
		if (dir.exists())
			fc.setCurrentDirectory(dir);
		
		int ret = fc.showOpenDialog(null); 
		if (ret == JFileChooser.APPROVE_OPTION) {
			File file = new File(fc.getSelectedFile().getAbsolutePath());
			if (file.exists())
				return file;
		}
		return null;
	}
	
	public static File browseDirectory(String path) {
		JFileChooser fc = new JFileChooser(); 
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		File dir = new File(path);
		if (dir.exists())
			fc.setCurrentDirectory(dir);

		int ret = fc.showOpenDialog(null); 
		if (ret == JFileChooser.APPROVE_OPTION) {
			File file = new File(fc.getSelectedFile().getAbsolutePath());
			if (file.exists())
				return file;
		}
		return null;
	}
}
