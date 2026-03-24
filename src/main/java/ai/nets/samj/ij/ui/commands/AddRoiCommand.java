/*-
 * #%L
 * Plugin to help image annotation with SAM-based Deep Learning models
 * %%
 * Copyright (C) 2024 - 2026 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ai.nets.samj.ij.ui.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.ij.utils.RoiManagerPrivateViolator;
import ij.gui.PolygonRoi;
import ij.plugin.frame.RoiManager;

/**
 * Command that adds one or more SAM-generated polygon ROIs to the ROI manager
 * and records enough state to undo the addition later.
 */
public class AddRoiCommand implements Command {
	private RoiManager roiManager;
	private final List<Mask> polys;
	private List<PolygonRoi> rois;
	private boolean isAddingToRoiManager = true;
	private String shape = "";
	private int promptCount = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
	private String modelName = "";
  
	/**
	 * Creates a command that will add the provided masks as polygon ROIs.
	 *
	 * @param roiManager the ROI manager that receives the generated ROIs
	 * @param polys the masks to convert into ImageJ polygon ROIs
	 */
	public AddRoiCommand(RoiManager roiManager, List<Mask> polys) {
		this.roiManager = roiManager;
		this.polys = polys;
	}
	
	/**
	 * Sets the prompt type label used in ROI names created by this command.
	 *
	 * @param shape the prompt description, such as {@code point} or {@code rect}
	 */
	public void setPromptShape(String shape) {
		this.shape = shape;
	}
	
	/**
	 * Sets the prompt sequence number used to name the generated ROIs.
	 *
	 * @param promptCount the prompt counter to embed in ROI names
	 */
	public void setPromptCount(int promptCount) {
		this.promptCount = promptCount;
	}
	
	/**
	 * Sets the model name suffix used in ROI names created by this command.
	 *
	 * @param modelName the SAM model name associated with the generated masks
	 */
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * @param addToRoiManager {@code true} to add ROIs to the ROI manager,
	 *          {@code false} to keep the command in-memory only
	 */
	public void setAddingToRoiManager(boolean addToRoiManager) {
		this.isAddingToRoiManager = addToRoiManager;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * @return the polygon ROIs created from the command masks
	 */
	public List<PolygonRoi> getImageJRois(){
		return rois;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * @return the masks that will be added by this command
	 */
	public List<Mask> getMasks(){
		return polys;
	}
  
	/**
	 * Converts the stored masks to polygon ROIs, names them, and optionally adds
	 * them to the ROI manager.
	 */
	@Override
	public void execute() {
		rois = new ArrayList<PolygonRoi>();
		int resNo = 1;
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
			rois.add(pRoi);
			if (isAddingToRoiManager) roiManager.addRoi(pRoi);;
		}
	}
  
	/**
	 * Removes the ROIs previously added by this command from the ROI manager.
	 */
	@Override
	public void undo() {
		try {
			for (PolygonRoi rr2 : rois) {
		    	for (int n = this.roiManager.getCount() - 1; n >= 0; n --) {
		    		PolygonRoi rr = (PolygonRoi) roiManager.getRoi(n);
	    			if (!Arrays.equals(rr.getXCoordinates(), rr2.getXCoordinates()))
	    				continue;
	    			if (!Arrays.equals(rr.getYCoordinates(), rr2.getYCoordinates()))
	    				continue;
		    		RoiManagerPrivateViolator.deleteRoiAtPosition(this.roiManager, n);
		    		break;		    		
		    	}
				
			}
		} catch (Exception ex) {
    		ex.printStackTrace();
    	}
	}
}
