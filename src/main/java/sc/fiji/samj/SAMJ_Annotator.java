package sc.fiji.samj;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import sc.fiji.samj.communication.model.SAMModels;
import sc.fiji.samj.gui.SAMJDialog;

public class SAMJ_Annotator implements PlugIn {

	public static void main(String args[]) {
		new ImageJ();
		new SAMJ_Annotator().run("");
	}

	@Override
	public void run(String arg0) {
		SAMModels availableModels = new SAMModels();
		ImagePlus imp = IJ.createImage("Test", 512, 512, 1, 8);
		
		imp.show();
		new SAMJDialog(imp, availableModels);
	}
}

