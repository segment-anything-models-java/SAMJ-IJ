package ai.nets.samj.ij.ui;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.ui.ExternalMethodsInterface;
import ij.WindowManager;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class IJSamMethods implements ExternalMethodsInterface {
	
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Override
	public <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getImageMask(File file) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ComboBoxItem> getListOfOpenImages() {
		return Arrays.stream(WindowManager.getImageTitles())
				.map(title -> new IJComboBoxItem(WindowManager.getImage(title).getID(), (Object) WindowManager.getImage(title)))
				.collect(Collectors.toList());
	}

}
