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

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.ij.utils.RoiManagerPrivateViolator;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

public class DeleteRoiCommand implements Command {
	private RoiManager roiManager;
	private final List<Mask> polys;
	private final List<PolygonRoi> rois;
	private boolean isAddingToRoiManager = true;
  
	public DeleteRoiCommand(RoiManager roiManager, List<Mask> polys) {
		this.roiManager = roiManager;
		this.polys = polys;
		rois = new ArrayList<PolygonRoi>();
		for (Mask m : polys) {
			PolygonRoi roi = new PolygonRoi(m.getContour(), PolygonRoi.POLYGON);
			roi.setName(m.getName());
			rois.add(roi);
		}
	}
	
	public void execute() {
		if (!isAddingToRoiManager)
			return;
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
	
	public void undo() {
		for (Roi m : rois) {
			if (isAddingToRoiManager) roiManager.addRoi(m);;
		}
	}
	
	@Override
	public void setAddingToRoiManager(boolean addToRoiManager) {
		this.isAddingToRoiManager = addToRoiManager;
	}
	
	@Override
	public List<PolygonRoi> getImageJRois() {
		return rois;
	}
	
	@Override
	public List<Mask> getMasks(){
		return polys;
	}
}
