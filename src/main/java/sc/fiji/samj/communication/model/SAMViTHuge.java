package sc.fiji.samj.communication.model;

import net.imglib2.RandomAccessibleInterval;
import org.scijava.log.Logger;
import sc.fiji.samj.communication.PromptsToNetAdapter;

public class SAMViTHuge implements SAMModel {
	
	@Override
	public String getName() {
		return "ViT Huge";
	}

	@Override
	public String getDescription() {
		return "SAM Official ViT Huge";
	}

	@Override
	public boolean isInstalled() {
		return false;
	}

	@Override
	public PromptsToNetAdapter instantiate(final RandomAccessibleInterval<?> image, final Logger useThisLoggerForIt) {
		useThisLoggerForIt.error("Sorry, ViT Huge network is actually not installed...");
		return null;
	}
}
