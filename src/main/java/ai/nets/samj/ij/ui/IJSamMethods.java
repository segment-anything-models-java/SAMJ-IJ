package ai.nets.samj.ij.ui;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.ui.UtilityMethods;
import ij.WindowManager;

/**
 * Implementation of the {@link UtilityMethods} interface providing the methods needed by the SAMJ interface 
 * that are specific to ImageJ
 * 
 * @author Carlos Garcia
 */
public class IJSamMethods implements UtilityMethods {

	@Override
	/**
	 * {@inheritDoc}
	 * 
	 * GEt the list of open images in ImageJ
	 */
	public List<ComboBoxItem> getListOfOpenImages() {
		return Arrays.stream(WindowManager.getImageTitles())
				.map(title -> new IJComboBoxItem(WindowManager.getImage(title).getID(), (Object) WindowManager.getImage(title)))
				.collect(Collectors.toList());
	}

}
