package sc.fiji.samj.communication.model;

import net.imglib2.RandomAccessibleInterval;
import org.scijava.log.Logger;
import sc.fiji.samj.communication.PromptsToNetAdapter;
import sc.fiji.samj.communication.PromptsToEfficientSamJ;
import java.io.IOException;

public class EfficientSAM implements SAMModel {
	private static final String FULL_NAME = "Efficient SAM";

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
	public PromptsToNetAdapter instantiate(final RandomAccessibleInterval<?> image, final Logger useThisLoggerForIt) {
		try {
			return new PromptsToEfficientSamJ(image,useThisLoggerForIt);
		} catch (IOException | InterruptedException | RuntimeException e) {
			useThisLoggerForIt.error(FULL_NAME+" experienced an error: "+e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}