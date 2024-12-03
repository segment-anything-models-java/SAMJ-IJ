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

import java.io.IOException;

import javax.swing.SwingUtilities;

import ai.nets.samj.gui.MainGUI;
import ai.nets.samj.ui.SAMJLogger;
import ij.ImageJ;
import ij.gui.GUI;
import ij.plugin.PlugIn;
import ai.nets.samj.ij.ui.Consumer;

// TODO I (Carlos) don't know how to develop in IJ2 @Plugin(type = Command.class, menuPath = "Plugins>SAMJ>Annotator")
//TODO I (Carlos) don't know how to develop in IJ2 public class Plugin_SamJAnnotator implements Command {

/**
 * ImageJ plugin that implements the SAMJ default GUI that can help annotating images 
 * using SAM-based Deep Learning models
 * @author Carlos Garcia
 * @author Vladimir Ulman
 */
public class SAMJ_Annotator implements PlugIn {
	final static long MAX_IMAGE_SIZE_IN_BYTES = ((long)4)<<30; //4 GB

	// TODO I (Carlos) don't know how to develop in IJ2 @Parameter
	//private LogService logService = new LogService();

	/**
	 * Run the plugin
	 * @throws InterruptedException if there is any thread interruption error
	 * @throws IOException if there is any file error
	 */
	public void run() throws IOException, InterruptedException {

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
		try {
			run();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
