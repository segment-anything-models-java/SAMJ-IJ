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
package ai.nets.samj.ij;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;

import javax.swing.SwingUtilities;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.gui.MainGUI;
import ai.nets.samj.ui.SAMJLogger;
import ij.IJ;
import ij.ImageJ;
import ij.Macro;
import ij.gui.GUI;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import ai.nets.samj.ij.ui.Consumer;
import ai.nets.samj.models.AbstractSamJ.BatchCallback;

// TODO I (Carlos) don't know how to develop in IJ2 @Plugin(type = Command.class, menuPath = "Plugins>SAMJ>Annotator")
//TODO I (Carlos) don't know how to develop in IJ2 public class Plugin_SamJAnnotator implements Command {

/**
 * ImageJ plugin that implements the SAMJ default GUI that can help annotating images 
 * using SAM-based Deep Learning models
 * @author Carlos Garcia
 * @author Vladimir Ulman
 */
public class SAMJ_Annotator implements PlugIn {
	
	private String macroModel;
	
	private String macroMaskPrompt;
	
	private String macroExport;
	
	final static long MAX_IMAGE_SIZE_IN_BYTES = ((long)4)<<30; //4 GB
	
	private final static String MACRO_INFO = "https://github.com/segment-anything-models-java/SAMJ-IJ/blob/main/README.md#macros";
	
	final static String MACRO_RECORD_COMMENT = ""
	        + System.lineSeparator()
	        + "// Note: SAMJ macros are supported only in BatchSAMize mode with preset prompts." + System.lineSeparator()
	        + "// The macro recording feature will capture the command 'run(\"SAMJ Annotator\");', but executing it will have no effect." + System.lineSeparator()
	        + "// To record something, please click the 'SAMJ BatchSAMize' button." + System.lineSeparator()
	        + "// For more information, visit:" + System.lineSeparator()
	        + "// " + MACRO_INFO + System.lineSeparator()
	        + System.lineSeparator();
	/**
	 * Optional keys to run SAMJ run with a macro or in headless mode
	 */
	private final static String[] macroOptionalKeys = new String[] {"model=", "maskPrompt=", "export="};
	
	private static Consumer MACRO_CONSUMER;
	
	private final static BatchCallback MACRO_CALLBACK = new BatchCallback() {
		@Override
		public void setTotalNumberOfRois(int nRois) {}
		@Override
		public void updateProgress(int n) {}

		@Override
		public void drawRoi(List<Mask> masks) {
			SwingUtilities.invokeLater(() -> MACRO_CONSUMER.addPolygonsFromGUI(masks));
			
		}

		@Override
		public void deletePointPrompt(List<int[]> promptList) {
			SwingUtilities.invokeLater(() -> promptList.forEach(proi -> MACRO_CONSUMER.deletePointRoi(proi)));
		}

		@Override
		public void deleteRectPrompt(List<int[]> promptList) {
			SwingUtilities.invokeLater(() -> promptList.stream()
					.map(rect -> new Rectangle(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]))
					.forEach(roi -> MACRO_CONSUMER.deleteRectRoi(roi)));
		}
    	
    };

	// TODO I (Carlos) don't know how to develop in IJ2 @Parameter
	//private LogService logService = new LogService();

	/**
	 * Run the plugin
	 * @throws InterruptedException if there is any thread interruption error
	 * @throws IOException if there is any file error
	 */
	public void run() throws IOException, InterruptedException {
		if (Recorder.record)
			Recorder.recordString(MACRO_RECORD_COMMENT);

		// TODO I (Carlos) don't know how to develop in IJ2 final Logger log = logService.subLogger("SAMJ");
		try {
			
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


			SwingUtilities.invokeLater(() -> {
				MainGUI samjDialog = new MainGUI(new Consumer());
				GUI.center(samjDialog);
			});
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	/**
	 * method for tesitng during development
	 * @param args
	 * 	nothing
	 * @throws InterruptedException if there is any thread related error
	 * @throws IOException if there is any file related error
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		ImageJ ij = new ImageJ();
		new SAMJ_Annotator().run();
	}


	@Override
	public void run(String arg) {
		boolean isMacro = IJ.isMacro();
		boolean isHeadless = GraphicsEnvironment.isHeadless();
		try {
			if (isMacro) {
				runMacro();
			} else if (isHeadless) {
			} else {
				run();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void runMacro() throws IOException, RuntimeException, InterruptedException {
		if (Macro.getOptions() == null)
			return;
		parseCommand();
		
		if (macroModel == null && macroExport.equals("true")) {
			macroExport();
		} else if (macroModel != null && macroExport.equals("true")) {
			macroRunSAMJ();
			macroExport();
		} else if (macroModel != null ) {
			macroRunSAMJ();
		}
	}
	
	private void macroExport() {
		if (MACRO_CONSUMER == null)
			throw new IllegalArgumentException("In order to be able to export annotations to mask, "
					+ "some annotations with the SAMJ Macro should have been done first.");
		MACRO_CONSUMER.exportImageLabeling();
	}

	private < T extends RealType< T > & NativeType< T > > 
	void macroRunSAMJ() throws IOException, RuntimeException, InterruptedException {
		MACRO_CONSUMER = new Consumer();
		MACRO_CONSUMER.setFocusedImage(MACRO_CONSUMER.getFocusedImage());
		SAMModel selected = MainGUI.DEFAULT_MODEL_LIST.stream()
				.filter(mm -> mm.getName().equals(macroModel)).findFirst().orElse(null);
		if (selected == null)
			throw new IllegalArgumentException("Specified model does not exist. Please, for more info visit: "
					+ MACRO_INFO);
		MACRO_CONSUMER.setModel(selected);
		RandomAccessibleInterval<T> rai = MACRO_CONSUMER.getFocusedImageAsRai();
		selected.setImage(rai, null);
    	List<int[]> pointPrompts = MACRO_CONSUMER.getPointRoisOnFocusImage();
    	List<Rectangle> rectPrompts = MACRO_CONSUMER.getRectRoisOnFocusImage();
    	
		selected.processBatchOfPrompts(pointPrompts, rectPrompts, rai, MACRO_CALLBACK);
    	pointPrompts.stream().forEach(pp -> MACRO_CONSUMER.deletePointRoi(pp));
    	rectPrompts.stream().forEach(pp -> MACRO_CONSUMER.deleteRectRoi(pp));
		
		selected.closeProcess();
	}
	
	private void parseCommand() {
		String macroArg = Macro.getOptions();

		macroModel = parseArg(macroArg, macroOptionalKeys[0], false);
		macroMaskPrompt = parseArg(macroArg, macroOptionalKeys[1], false);
		macroExport = parseArg(macroArg, macroOptionalKeys[2], false);
		if (macroModel == null && macroExport == null)
			throw new IllegalArgumentException("SAMJ macro requires the parameter 'model' to be"
					+ " specified to make annotations or the parameter 'export' if the "
					+ "user wants to export already annotated masks. More info at: " + MACRO_INFO);
		if (macroExport == null)
			macroExport = "false";
		macroExport = macroExport.toLowerCase().equals("false") ? "false" : macroExport;
		macroExport = macroExport.toLowerCase().equals("true") ? "true" : macroExport;
		if (!macroExport.equals("false") && !macroExport.equals("true"))
			throw new IllegalArgumentException("The SAMJ macro argument 'export' can only be true or false."
					+ " For more info: " + MACRO_INFO);
	}
	
	private static String parseArg(String macroArg, String arg, boolean required) {
		int modelFolderInd = macroArg.indexOf(arg);
		if (modelFolderInd == -1 && required)
			throw new IllegalArgumentException("SAMJ macro requires to the variable '" + arg + "'. "
					+ "For more info, please visit: " + MACRO_INFO);
		else if (modelFolderInd == -1)
			return null;
		int modelFolderInd2 = macroArg.indexOf(arg + "[");
		int endInd = macroArg.indexOf(" ", modelFolderInd);
		String value;
		if (modelFolderInd2 != -1) {
			endInd = macroArg.indexOf("] ", modelFolderInd2);
			value = macroArg.substring(modelFolderInd2 + arg.length() + 1, endInd);
		} else {
			value = macroArg.substring(modelFolderInd + arg.length(), endInd);
		}
		if (value.equals("null") || value.equals(""))
			value = null;
		return value;
	}
}
