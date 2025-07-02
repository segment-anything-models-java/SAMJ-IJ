package ai.nets.samj.ij.ui.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.ij.utils.RoiManagerPrivateViolator;
import ij.gui.PolygonRoi;
import ij.plugin.frame.RoiManager;

public class AddRoiCommand implements Command {
	private RoiManager roiManager;
	private final List<Mask> polys;
	private boolean isAddingToRoiManager = true;
	private String shape = "";
	private int promptCount = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
	private String modelName = "";
  
	public AddRoiCommand(RoiManager roiManager, List<Mask> polys) {
		this.roiManager = roiManager;
		this.polys = polys;
	}
	
	public void setPromptShape(String shape) {
		this.shape = shape;
	}
	
	public void setPromptCount(int promptCount) {
		this.promptCount = promptCount;
	}
	
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
	
	public void setAddingToRoiManager(boolean addToRoiManager) {
		this.isAddingToRoiManager = addToRoiManager;
	}
  
	@Override
	public void execute() {
		int resNo = 1;
		List<PolygonRoi> undoRois = new ArrayList<PolygonRoi>();
		for (Mask m : polys) {
			final PolygonRoi pRoi = new PolygonRoi(m.getContour(), PolygonRoi.POLYGON);
			String name = promptCount + "." + (resNo ++) + "_"+shape + "_" + modelName;
			if (shape.equals("") && modelName.equals(""))
				name = "" + promptCount;
			else if (modelName.equals(""))
				name = promptCount + "." + (resNo) + "_"+shape;
			else if (shape.equals(""))
				name = promptCount + "." + (resNo) + "_"+modelName;
				
			pRoi.setName(name);
			m.setName(name);
			if (isAddingToRoiManager) roiManager.addRoi(pRoi);;
			undoRois.add(pRoi);
		}
	}
  
	@Override
	public void undo() {
		try {
	    	for (int n = this.roiManager.getCount() - 1; n >= 0; n --) 
	    		RoiManagerPrivateViolator.deleteRoiAtPosition(this.roiManager, n);
		} catch (Exception ex) {
    		ex.printStackTrace();
    	}
	}
}