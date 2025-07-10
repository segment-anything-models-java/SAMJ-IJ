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

import java.util.List;

import ai.nets.samj.annotation.Mask;
import ij.gui.PolygonRoi;


public interface Command {
	public void execute();
	
	public void undo();
	
	public void setAddingToRoiManager(boolean addToRoiManager);
	
	public List<PolygonRoi> getImageJRois();
	
	public List<Mask> getMasks();
}
