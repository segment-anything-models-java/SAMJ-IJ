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
package ai.nets.samj.ij.utils;

import java.awt.EventQueue;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;

import javax.swing.DefaultListModel;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

/**
 * Reflection-based helper for invoking private ROI manager operations that are
 * needed by the plugin but not exposed by ImageJ's public API.
 */
public class RoiManagerPrivateViolator {
	
	/**
	 * Removes the ROI at a given position from an initialized {@link RoiManager}
	 * by calling its internal deletion routines.
	 *
	 * @param roiM the ROI manager instance to modify
	 * @param position the zero-based ROI index to delete
	 * @throws NoSuchFieldException if an expected private field is not present in
	 *          the current ImageJ implementation
	 * @throws SecurityException if reflective access to the ROI manager internals
	 *          is denied
	 * @throws NoSuchMethodException if an expected private helper method is not
	 *          present in the current ImageJ implementation
	 * @throws IllegalAccessException if reflective access to a private member is
	 *          not permitted
	 * @throws IllegalArgumentException if the reflected member is invoked with an
	 *          invalid argument
	 * @throws InvocationTargetException if the invoked ImageJ method throws an
	 *          exception while deleting the ROI
	 */
	@SuppressWarnings("unchecked")
	public static void deleteRoiAtPosition(RoiManager roiM, int position) throws NoSuchFieldException, SecurityException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Objects.requireNonNull(roiM, "Please provide an initialized RoiManager.");
		int nRois = roiM.getCount();
		if (position >= nRois) return;

        Field roisPrivate = RoiManager.class.getDeclaredField("rois");
        roisPrivate.setAccessible(true);
        Field listModelPrivate = RoiManager.class.getDeclaredField("listModel");
        listModelPrivate.setAccessible(true);

        // Access private method
        Method deleteOnEDTPrivate = RoiManager.class.getDeclaredMethod("deleteOnEDT", int.class);
        deleteOnEDTPrivate.setAccessible(true);
        Method updateShowAllPrivate = RoiManager.class.getDeclaredMethod("updateShowAll");
        updateShowAllPrivate.setAccessible(true);
        
        ArrayList<Roi> rois = ((ArrayList<Roi>) roisPrivate.get(roiM));
        DefaultListModel<String> listModel = ((DefaultListModel<String>) listModelPrivate.get(roiM));
        

		if (EventQueue.isDispatchThread()) {
			rois.remove(position);
			listModel.remove(position);
		} else {
			deleteOnEDTPrivate.invoke(roiM, position);
		}
		updateShowAllPrivate.invoke(roiM);
	}
}
