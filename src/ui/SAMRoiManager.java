package ui;
import java.awt.event.ActionEvent;

import ij.plugin.frame.RoiManager;

public class SAMRoiManager extends RoiManager {

	public void showRoi() {
		this.setTitle("SAM Roi Manager");
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		super.actionPerformed(event);
		String label = event.getActionCommand();
		if (label==null)
				return;
	}
}
