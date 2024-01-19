package sc.fiji.samj.communication.model;

import net.imglib2.RandomAccessibleInterval;
import org.scijava.log.Logger;
import sc.fiji.samj.communication.PromptsToFakeSamJ;
import sc.fiji.samj.communication.PromptsToNetAdapter;

public class MicroSAM implements SAMModel {

	@Override
	public String getName() {
		return "Micro SAM";
	}

	@Override
	public String getDescription() {
		return "Bla bla Micro SAM";
	}

	@Override
	public boolean isInstalled() {
		return false;
	}

	@Override
	public PromptsToNetAdapter instantiate(final RandomAccessibleInterval<?> image, final Logger useThisLoggerForIt) {
		useThisLoggerForIt.error("Sorry, MicroSAM network is actually not installed...");
		return null;
	}
}
