package sc.fiji.samj;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import sc.fiji.samj.ui.SAMDialog;

public class SAMJ_ implements PlugIn {


	public static void main(String args[]) {
		new ImageJ();
		new SAMJ_().run("");
	}

	@Override
	public void run(String arg0) {
		ImagePlus imp = IJ.createImage("Test", 512, 512, 1, 8);
		imp.show();
		new SAMDialog(imp);
	}
}
