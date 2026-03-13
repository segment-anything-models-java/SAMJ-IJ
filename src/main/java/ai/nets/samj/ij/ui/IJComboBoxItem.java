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
package ai.nets.samj.ij.ui;


import ai.nets.samj.gui.components.ComboBoxItem;
import ij.ImageListener;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

/**
 * Implementation of the SAMJ interface {@link ComboBoxItem} that provides the SAMJ GUI
 * an item for the combobox that references ImageJ {@link ImagePlus}
 * 
 * @author Carlos Garcia
 */
public class IJComboBoxItem extends ComboBoxItem {
	
	private boolean imageClosed = false;
	
	/**
	 * 
	 * Combobox item that contains an Object associated to a unique identifier.
	 * For ImageJ the object is an ImageJ {@link ImagePlus}
	 * @param seq
	 * 	the object of interest, whihc in the case of ImageJ is and {@link ImagePlus}
	 */
	public IJComboBoxItem(Object seq) {
		super(seq);
		ImagePlus.addImageListener(new ImageListener() {

			@Override
			public void imageOpened(ImagePlus imp) {}
			@Override
			public void imageUpdated(ImagePlus imp) {}

			@Override
			public void imageClosed(ImagePlus imp) {
				if (!imp.equals(seq))
					return;
				imageClosed = true;
			}
    		
    	});
	}
	
	/**
	 * Create an empty {@link ComboBoxItem}. Its id is -1
	 */
	public IJComboBoxItem() {
		super();
	}
	
	@Override
	public Object getValue() {
		if (imageClosed)
			return null;
		else
			return super.getValue();
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
	public <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getImageAsImgLib2() {
		ImagePlus imp = (ImagePlus) this.getValue();
		boolean isColorRGB = imp.getType() == ImagePlus.COLOR_RGB;
		if (!isColorRGB)
			return ImageJFunctions.wrap(imp);

		RandomAccessibleInterval<ARGBType> rgba = ImagePlusAdapter.wrapRGBA(imp);
		RandomAccessibleInterval<UnsignedByteType> red = Converters.convert(
				rgba,
				(in, out) -> out.set(ARGBType.red(in.get())),
				new UnsignedByteType());
		RandomAccessibleInterval<UnsignedByteType> green = Converters.convert(
				rgba,
				(in, out) -> out.set(ARGBType.green(in.get())),
				new UnsignedByteType());
		RandomAccessibleInterval<UnsignedByteType> blue = Converters.convert(
				rgba,
				(in, out) -> out.set(ARGBType.blue(in.get())),
				new UnsignedByteType());

		RandomAccessibleInterval<UnsignedByteType> rgb = Views.stack(red, green, blue);
		int channelAxis = rgb.numDimensions() - 1;
		while (channelAxis > 2) {
			rgb = Views.permute(rgb, channelAxis, channelAxis - 1);
			channelAxis--;
		}
		return (RandomAccessibleInterval<T>) rgb;
	}

	@Override
	public int getCurrentFrame() {
		return ((ImagePlus) this.getValue()).getFrame() - 1;
	}

	@Override
	public int getCurrentSlice() {
		return ((ImagePlus) this.getValue()).getCurrentSlice() - 1;
	}

	@Override
	public int getNFrames() {
		return ((ImagePlus) this.getValue()).getNFrames();
	}

	@Override
	public int getNSlices() {
		return ((ImagePlus) this.getValue()).getNSlices();
	}


}
