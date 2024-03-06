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

/**
 * Implementation of the SAMJ interface {@link ComboBoxItem} that provides the SAMJ GUI
 * an item for the combobox that references ImageJ {@link ImagePlus}
 * 
 * @author Carlos Garcia
 */
public class IJComboBoxItem extends ComboBoxItem {
	
	/**
	 * 
	 * Combobox item that contains an Object associated to a unique identifier.
	 * For ImageJ the object is an ImageJ {@link ImagePlus}
	 * @param uniqueID
	 * 	unique indentifier of the combobox element to avoid confusion when there are two images
	 * 	with the same name
	 * @param seq
	 * 	the object of interest, whihc in the case of ImageJ is and {@link ImagePlus}
	 */
	public IJComboBoxItem(int uniqueID, Object seq) {
		super(uniqueID, seq);
	}
	
	/**
	 * Create an empty {@link ComboBoxItem}. Its id is -1
	 */
	public IJComboBoxItem() {
		super();
	}

	@Override
	/**
	 * {@inheritDoc}
	 * 
	 * For ImageJ is the name of the ImageJ {@link ImagePlus}
	 */
	public String getImageName() {
		return ((ImagePlus) this.getValue()).getTitle();
	}

	@Override
	/**
	 * {@inheritDoc}
	 * 
	 * Convert the {@link ImagePlus} into a {@link RandomAccessibleInterval}
	 */
	public <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getImageasImgLib2() {
		Img<?> img = ImageJFunctions.wrap((ImagePlus) this.getValue());
		return Cast.unchecked(Views.permute(img, 1, 2));
	}


}
