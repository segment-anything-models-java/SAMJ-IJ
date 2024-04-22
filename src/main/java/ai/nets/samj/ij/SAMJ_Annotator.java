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

import ai.nets.samj.communication.model.SAMModels;
import ai.nets.samj.gui.SAMJDialog;
import ai.nets.samj.ij.ui.IJSamjLogger;
import ai.nets.samj.ui.SAMJLogger;
import ij.ImagePlus;
import ai.nets.samj.ij.ui.IJ1PromptsProvider;
import ai.nets.samj.ij.ui.IJSamMethods;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * ImageJ plugin that implements the SAMJ default GUI that can help annotating images 
 * using SAM-based Deep Learning models
 * @author Carlos Garcia
 * @author Vladimir Ulman
 */
@Plugin(type = Command.class, menuPath = "Plugins>SAMJ>Annotator")
public class SAMJ_Annotator implements Command {

	@Parameter
	private LogService logService;

	public void run() {
		try {
			//get list of recognized installations of SAM(s)
			//TODO: why this is not part of SAMJ (application agnostic package)
			final SAMModels availableModels = new SAMModels();

			//get our own IJ2 loggers
			final SAMJLogger guiLogger = new IJSamjLogger(logService.subLogger("SAMJ"));
			final SAMJLogger networkLogger = new IJSamjLogger(logService.subLogger("Networks window"), "network -- ");

			final SAMJDialog samjDialog
					= new SAMJDialog(availableModels, new IJSamMethods(), guiLogger, networkLogger);
			//create the GUI adapter between the user inputs/prompts and SAMJ outputs
			samjDialog.setPromptsProvider(
					  (obj) -> new IJ1PromptsProvider((ImagePlus) obj, logService.subLogger("PromptsResults window"))
			);
			//samjDialog.close();
		} catch (RuntimeException e) {
			logService.error("SAMJ error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * method for tesitng during development
	 * @param args
	 * 	nothing
	 */
	public static void main(String[] args) {
		ImageJ ij = new ImageJ();
		new SAMJ_Annotator().run();
	}
}
