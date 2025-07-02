package ai.nets.samj.ij.ui.commands;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import ai.nets.samj.annotation.Mask;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

public class DeleteRoiCommand implements Command {
	private RoiManager roiManager;
	private final List<Mask> polys;
	private List<Roi> rois;
	private boolean isAddingToRoiManager = true;
	private String shape = "";
	private int promptCount = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
	private String modelName = "";
  
	public DeleteRoiCommand(RoiManager roiManager, List<Mask> polys) {
		this.roiManager = roiManager;
		this.polys = polys;
	}
	
	public void execute() {
		model.remove(mask);
	}
	
	public void undo() {
		model.add(mask);
	}
	
	@Override
	public void setAddingToRoiManager(boolean addToRoiManager) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public List<Roi> getImageJRois() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Mask> getMasks(){
		return polys;
	}
}
