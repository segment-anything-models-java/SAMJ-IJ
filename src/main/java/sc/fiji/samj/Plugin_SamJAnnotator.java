package sc.fiji.samj;

import ij.ImagePlus;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.samj.communication.model.SAMModels;
import sc.fiji.samj.gui.SAMJDialog;
import sc.fiji.samj.ui.PromptsResultsDisplay;
import sc.fiji.samj.ui.ij.IJ1PromptsProvider;

@Plugin(type = Command.class, menuPath = "Plugins>SAMJ>Annotator")
public class Plugin_SamJAnnotator implements Command {
	@Parameter
	private ImagePlus imagePlus;

	@Parameter
	private LogService logService;

	@Override
	public void run() {
		if (imagePlus == null) {
			logService.info("No image available.");
			return;
		}

		//TODO: test the size of the image

		try {
			final Logger log = logService.subLogger("SAMJ on "+imagePlus.getTitle());

			//get list of recognized installations of SAM(s)
			final SAMModels availableModels = new SAMModels();

			//create the GUI adapter between the user inputs/prompts and SAMJ outputs
			final PromptsResultsDisplay display = new IJ1PromptsProvider(imagePlus, log.subLogger("PromptsResults window"));

			new SAMJDialog(display, availableModels, log);
		} catch (RuntimeException e) {
			logService.error("SAMJ error: "+e.getMessage());
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
	}
}