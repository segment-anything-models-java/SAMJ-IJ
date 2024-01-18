package gui;

import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

public class SAMRoiManager {

	private ImagePlus imp;
	
	public SAMRoiManager(ImagePlus imp) {
		this.imp = imp;
		getRoiManager();
	}
	
	public RoiManager getRoiManager() {
		RoiManager roiManager  = RoiManager.getInstance();
		if (roiManager == null) {
			roiManager = new RoiManager();
		}
		roiManager.setEditMode(imp, true);
		roiManager.setVisible(true);
		roiManager.setTitle("SAM Roi Manager");
		Prefs.useNamesAsLabels = true;
		return roiManager;
	}
	
	public void close() {
		getRoiManager().close();
	}
	
	public void add(Roi roi) {
		getRoiManager().add(roi, 1);
	}
	
	public int getCount() {
		return getRoiManager().getCount();
	}
	
}
