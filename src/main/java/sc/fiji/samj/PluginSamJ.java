package sc.fiji.samj;

import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.frame.RoiManager;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.samj.communication.PromptsToFakeSamJ;
import sc.fiji.samj.communication.PromptsToNetAdapter;
import sc.fiji.samj.ui.ij.PromptsProvider;

@Plugin(type = Command.class, menuPath = "Plugins>SAMJ")
public class PluginSamJ implements Command {
	@Parameter
	private ImagePlus imagePlus;

	//TBA: QUERY WHATEVER IS NEEDED TO CONNECT TO SOME RUNNING SAM

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

			//get some implementation of SAM
			final PromptsToNetAdapter someSamImpl = new PromptsToFakeSamJ(log.subLogger("SAM fake Python side"));

			//get the Fiji's ROI manager
			final RoiManager roiManager = startRoiManager();

			//create the adapter between the user inputs and SAMJ outputs
			new PromptsProvider(imagePlus, someSamImpl, roiManager, log.subLogger("Prompts in image window"));

		} catch (RuntimeException e) {
			logService.error("SAMJ error: "+e.getMessage());
			e.printStackTrace();
		}
	}

	private RoiManager startRoiManager() {
		RoiManager roiManager = RoiManager.getInstance();
		if (roiManager == null) {
			roiManager = new RoiManager();
		}
		roiManager.setVisible(true);
		roiManager.setTitle("SAM Roi Manager");
		Prefs.useNamesAsLabels = true;
		roiManager.setEditMode(imagePlus, true);
		return roiManager;
	}

	public static void main(String[] args) {
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
	}
}