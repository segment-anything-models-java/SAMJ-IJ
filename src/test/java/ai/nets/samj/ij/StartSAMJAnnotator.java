package ai.nets.samj.ij;

import net.imagej.ImageJ;

public class StartSAMJAnnotator {
	public static void main(String[] args) {
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(SAMJ_Annotator.class,true);
	}
}
