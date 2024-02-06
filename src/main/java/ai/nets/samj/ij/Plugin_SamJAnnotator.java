package ai.nets.samj.ij;

import net.imagej.ImageJPlugin;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;

import ai.nets.samj.communication.model.SAMModels;
import ai.nets.samj.gui.SAMJDialog;
import ai.nets.samj.ui.SAMJLogger;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GUI;
import ai.nets.samj.ij.ui.IJ1PromptsProvider;
import ai.nets.samj.ij.ui.IJSamMethods;

// TODO I (Carlos) don't know how to develop in IJ2 @Plugin(type = Command.class, menuPath = "Plugins>SAMJ>Annotator")
//TODO I (Carlos) don't know how to develop in IJ2 public class Plugin_SamJAnnotator implements Command {
public class Plugin_SamJAnnotator implements ImageJPlugin {
	final static long MAX_IMAGE_SIZE_IN_BYTES = ((long)4)<<30; //4 GB

	// TODO I (Carlos) don't know how to develop in IJ2 @Parameter
	//private LogService logService = new LogService();

	// TODO I (Carlos) don't know how to develop in IJ2 @Override
	public void run() {

		// TODO I (Carlos) don't know how to develop in IJ2 final Logger log = logService.subLogger("SAMJ");
		try {
			//ask the user to isolate current time point from a time-lapse (multi-frame) image
			//...yes, for now, to make our life here easier...
			//(also cosider the CLI variant, where we must load image ourselves, do we also want to extract
			// ourselves a particular frame from a time-lapse image? ...no, we don't!)
			/*
			if (imagePlus.getNFrames() > 1) {
				IJ.showMessage("SAMJ Annotator", "The input image should not be a time-lapse (multi-frame) sequence.\n"
						+ "Here, the input image "+imagePlus.getTitle()+" contains "+imagePlus.getNFrames()+" frames.");
				return;
			}
			*/
			/*
			int imgNChannels = imagePlus.getNChannels();
			int imgNSlices = imagePlus.getNSlices();
			log.debug("Input image configuration: "+imgNChannels+"*"+imgNSlices);
			//NB: 3 is a prime number, can be "decomposed" only into 1*3 or 3*1,
			//which are exactly the two wanted configurations...
			if (imgNChannels*imgNSlices != 3) {
				//pity, an unwanted configuration, can we do something about it?
				if ((imgNChannels > 1 && imgNSlices > 1) || (imgNChannels*imgNSlices == 2)) {
					//we can't...;
					//either both channels and slices exist -> don't know in which order to "serialize" them;
					//or, only one exists but in the length of two not knowing which of the two to duplicate to get three
					IJ.showMessage("SAMJ Annotator", "The input image must be of the configuration exactly 1*3 or 3*1 channels*stack slices.\n"
							+ "Here, the input image "+imagePlus.getTitle()+" is "+imgNChannels+"*"+imgNSlices+".");
					return;
				}
				//if we got here, the input image is either:
				// - single channel and single slice (2D) -- conf.: 1 * 1 -> duplicate
				// - 4+ channel, single slice (2D)   -- 4+ * 1  -> "shorten" the 4+ into (exactly) 3
				// - single channel, 4+ slices (2D) --- 1 * 4+
				IJ.showMessage("SAMJ Annotator", "The input image must be of the configuration exactly 1*3 or 3*1 channels*stack slices.\n"
						+ "Going to wrap around, or create a copy, of the input but in the 1*3 configuration channels*slack slices.");
				imagePlus = obtainThreeChannelFloatTypeImage(imagePlus);
			}

			if (imagePlus.getType() != ImagePlus.GRAY32) {
				imagePlus = obtainFloatTypeImage(imagePlus);
			}

			double imgSize = imagePlus.getSizeInBytes();
			if (imgSize > MAX_IMAGE_SIZE_IN_BYTES) {
				IJ.showMessage("SAMJ Annotator", "The size of an image "+imagePlus.getTitle()+" exceeds 4 GB limit");
				return;
			}
			*/

			//get list of recognized installations of SAM(s)
			final SAMModels availableModels = new SAMModels();
			
			// TODO I (Carlos) don't know how to develop in IJ2 Logger guiSublogger = log.subLogger("PromptsResults window");
			SAMJLogger guilogger = new SAMJLogger() {
	            @Override
	            public void info(String text) {System.out.println(text);}
	            @Override
	            public void warn(String text) {System.out.println(text);}
	            @Override
	            public void error(String text) {System.out.println(text);}
	        };

	     // TODO I (Carlos) don't know how to develop in IJ2 Logger networkSublogger = log.subLogger("Networks window");
			SAMJLogger networkLogger = new SAMJLogger() {
	            @Override
	            public void info(String text) {System.out.println("network -- " + text);}
	            @Override
	            public void warn(String text) {System.out.println("network -- " + text);}
	            @Override
	            public void error(String text) {System.out.println("network -- " + text);}
	        };
			
	        SAMJDialog samjDialog = new SAMJDialog( availableModels, new IJSamMethods(), guilogger, networkLogger);
			//create the GUI adapter between the user inputs/prompts and SAMJ outputs
			samjDialog.setPromptsProvider((obj) -> {return new IJ1PromptsProvider((ImagePlus) obj, null);});// TODO log.subLogger("PromptsResults window"));});
			
			JDialog dialog = new JDialog(new JFrame(), "");
			dialog.add(samjDialog);
			dialog.pack();
			dialog.setResizable(false);
			dialog.setModal(false);
			dialog.setVisible(true);
			GUI.center(dialog);
		} catch (RuntimeException e) {
			//TODO log.error("SAMJ error: "+e.getMessage());
			e.printStackTrace();
		}
	}


	/**
	 * The method assumes that the given input is guaranteed to be "convertable" into three channels.
	 * That means, no time-lapse, either 1*1, 4+*1 or 1*4+ configurations, no time-lapse.
	 *
	 * If the input image is already in FloatType, the method will construct Views around it.
	 * These are constructs that, instead of creating image copies, pretend to be a new image
	 * of the new given shape while remembering the access pattern to the underlying original image.
	 */
	<T extends NumericType<T> & NativeType<T> & RealType<T>>
	ImagePlus obtainThreeChannelFloatTypeImage(final ImagePlus imagePlus) {
		Img<T> img = ImageJFunctions.wrap(imagePlus);
		if (img.numDimensions() > 3) {
			throw new IllegalArgumentException("Don't know how to convert an image with so many dimensions "
					+Util.printCoordinates(img.dimensionsAsLongArray()));
		}

		if (img.firstElement() instanceof FloatType) {
			//input already in the target pixel type,
			//construct then only on-the-fly wrappers (no memory duplication)
			// TODO logService.debug("wrapping around input image of dims: "+Util.printCoordinates(img.dimensionsAsLongArray()));
			if (img.numDimensions() == 2 || img.dimension(2) == 1) {
				//essentially configuration 1*1
				RandomAccessibleInterval<T> input2dImg = img.numDimensions() == 3 ? Views.hyperSlice(img, 2, 0) : img;
				return ImageJFunctions.show( Views.stack(input2dImg,input2dImg,input2dImg), "Expanded wrap of "+imagePlus.getTitle());
			} else {
				//crop in dim=2
				RandomAccessibleInterval<T> newImg = Views.interval(img,
						new long[]{img.min(0), img.min(1), img.min(2)  },
						new long[]{img.max(0), img.max(1), img.min(2)+2});
				return ImageJFunctions.show(newImg, "Narrowed wrap of "+imagePlus.getTitle());
			}
		} else {
			//input in wrong pixel type, copy&convert
			// TODO logService.debug("copy&convert-ing around input image of dims: "+Util.printCoordinates(img.dimensionsAsLongArray()));
			Interval newDims = new FinalInterval(
					new long[] {img.min(0),img.min(1), 0},
					new long[] {img.max(0),img.max(1), 2});
			Img<FloatType> newImg = img.factory().imgFactory(new FloatType()).create(newDims);

			if (img.numDimensions() == 2 || img.dimension(2) == 1) {
				//essentially configuration 1*1
				LoopBuilder
						.setImages(
								//2D(!) input slice and 3D output as three (2D) slices
								img.numDimensions() == 3 ? Views.hyperSlice(img,2,0) : img,
								Views.hyperSlice(newImg,2,0),
								Views.hyperSlice(newImg,2,1),
								Views.hyperSlice(newImg,2,2) )
						.forEachPixel( (i,o1,o2,o3) -> {
							//converts to Float and duplicate to the three output slices
							float val = i.getRealFloat();
							o1.setReal(val);
							o2.setReal(val);
							o3.setReal(val);
						});
				return ImageJFunctions.show(newImg, "Expanded copy of "+imagePlus.getTitle());
			} else {
				//narrow in dim=2
				LoopBuilder
						.setImages(
								Views.interval(img, Intervals.translate(newDims,img.min(2),2)),
								newImg )
						.forEachPixel( (i,o) -> o.setReal(i.getRealFloat()));
				return ImageJFunctions.show(newImg, "Narrowed copy of "+imagePlus.getTitle());
			}
		}
	}

	<T extends NumericType<T> & NativeType<T> & RealType<T>>
	ImagePlus obtainFloatTypeImage(final ImagePlus imagePlus) {
		Img<T> img = ImageJFunctions.wrap(imagePlus);
		// TODO logService.debug("converting input image of dims: "+Util.printCoordinates(img.dimensionsAsLongArray()));
		Img<FloatType> newImg = img.factory().imgFactory(new FloatType()).create(img);
		LoopBuilder.setImages(img,newImg).forEachPixel((i,o) -> o.setReal(i.getRealFloat()));
		return ImageJFunctions.show(newImg, "Converted copy of "+imagePlus.getTitle());
	}


	public static void main(String[] args) {
		ImageJ ij = new ImageJ();
		new Plugin_SamJAnnotator().run();
	}
}