/*-
 * #%L
 * Plugin to help image annotation with SAM-based Deep Learning models
 * %%
 * Copyright (C) 2024 SAMJ developers.
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

public class AddRoiCommand implements Command {
	private RoiManager roiManager;
	private final List<Mask> polys;
	private List<PolygonRoi> rois;
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
	
	public List<PolygonRoi> getImageJRois(){
		return rois;
	}
	
	public List<Mask> getMasks(){
		return polys;
	}
  
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