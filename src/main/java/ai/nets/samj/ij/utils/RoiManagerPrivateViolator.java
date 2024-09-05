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
