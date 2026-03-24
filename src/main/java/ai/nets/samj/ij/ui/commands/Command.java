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

import java.util.List;

import ai.nets.samj.annotation.Mask;
import ij.gui.PolygonRoi;

/**
 * Command abstraction used to track ROI additions and deletions so they can be
 * replayed or reverted from the annotation history.
 */
public interface Command {
	/**
	 * Applies the command to the current ROI state.
	 */
	public void execute();
	
	/**
	 * Reverts the effect of a previously executed command.
	 */
	public void undo();
	
	/**
	 * Enables or disables synchronization of the command with the ImageJ ROI manager.
	 *
	 * @param addToRoiManager {@code true} to update the ROI manager when the command
	 *          executes or undoes, {@code false} otherwise
	 */
	public void setAddingToRoiManager(boolean addToRoiManager);
	
	/**
	 * Returns the ImageJ polygon ROIs managed by this command.
	 *
	 * @return the polygon ROIs created or removed by the command
	 */
	public List<PolygonRoi> getImageJRois();
	
	/**
	 * Returns the SAM masks represented by this command.
	 *
	 * @return the masks associated with the command
	 */
	public List<Mask> getMasks();
}
