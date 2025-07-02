package ai.nets.samj.ij.ui.commands;

import java.util.List;

import ai.nets.samj.annotation.Mask;
import ij.gui.PolygonRoi;
import ij.gui.Roi;


public interface Command {
	public void execute();
	
	public void undo();
	
	public void setAddingToRoiManager(boolean addToRoiManager);
	
	public List<PolygonRoi> getImageJRois();
	
	public List<Mask> getMasks();
}
