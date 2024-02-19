package ai.nets.samj.ij.ui;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.ui.UtilityMethods;
import ij.WindowManager;

public class IJSamMethods implements UtilityMethods {

	@Override
	public List<ComboBoxItem> getListOfOpenImages() {
		return Arrays.stream(WindowManager.getImageTitles())
				.map(title -> new IJComboBoxItem(WindowManager.getImage(title).getID(), (Object) WindowManager.getImage(title)))
				.collect(Collectors.toList());
	}

}
