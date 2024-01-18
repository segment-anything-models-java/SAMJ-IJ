package sc.fiji.samj.communication.model;

import org.scijava.log.Logger;
import sc.fiji.samj.communication.PromptsToFakeSamJ;
import sc.fiji.samj.communication.PromptsToNetAdapter;

public class EfficientSAM implements SAMModel {
	private static final String FULL_NAME = "Efficient SAM";
	private static final String SHORT_NAME = "E.SAM";

	@Override
	public String getName() {
		return FULL_NAME;
	}

	@Override
	public String getDescription() {
		return "Bla bla Efficient SAM";
	}

	@Override
	public boolean isInstalled() {
		return true;
	}

	@Override
	public PromptsToNetAdapter instantiate(final Logger useThisLoggerForIt) {
		return new PromptsToFakeSamJ(useThisLoggerForIt, SHORT_NAME);
	}
}