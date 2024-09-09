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

public class RoiManagerPrivateViolator {
	
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
