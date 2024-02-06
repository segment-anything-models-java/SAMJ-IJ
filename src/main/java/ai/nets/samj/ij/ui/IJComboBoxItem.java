package ai.nets.samj.ij.ui;


import ai.nets.samj.gui.components.ComboBoxItem;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;

public class IJComboBoxItem extends ComboBoxItem {
	
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public IJComboBoxItem(int uniqueID, Object seq) {
		super(uniqueID, seq);
	}
	
	public IJComboBoxItem() {
		super();
	}

	@Override
	public String getImageName() {
		return ((ImagePlus) this.getValue()).getTitle();
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getImageasImgLib2() {
		Img<?> img = ImageJFunctions.wrap((ImagePlus) this.getValue());
		return Cast.unchecked(Views.permute(img, 1, 2));
	}


}
