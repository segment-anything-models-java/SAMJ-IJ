package sc.fiji.samj.communication;

import io.bioimage.samj.EfficientSamJ;
import io.bioimage.samj.SamEnvManager;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import org.scijava.log.Logger;
import java.awt.Polygon;
import java.io.IOException;
import java.util.List;

public class PromptsToEfficientSamJ implements PromptsToNetAdapter {

	private static final Polygon EMPTY_POLYGON = new Polygon(new int[0], new int[0], 0);
	private static final String LONG_NAME = "EfficientSamJ";

	private final EfficientSamJ efficientSamJ;
	private final Logger log;

	public PromptsToEfficientSamJ(final RandomAccessibleInterval<?> image,
	                              final Logger log)
	throws IOException, RuntimeException, InterruptedException {
		this.log = log;
		efficientSamJ = EfficientSamJ.initializeSam(SamEnvManager.create(), (RandomAccessibleInterval)image);
	}

	@Override
	public String getNetName() {
		return "E.SAM";
	}

	@Override
	public Polygon fetch2dSegmentation(List<Localizable> listOfPoints2D) {
		log.info(LONG_NAME+": NOT SUPPORTED YET");
		return EMPTY_POLYGON;
	}

	@Override
	public Polygon fetch2dSegmentation(Localizable lineStartPoint2D, Localizable lineEndPoint2D) {
		log.info(LONG_NAME+": NOT SUPPORTED YET");
		return EMPTY_POLYGON;
	}

	@Override
	public Polygon fetch2dSegmentation(Interval boundingBox2D) {
		try {
			//order to processBox() should be: x0,y0, x1,y1
			final int bbox[] = {
				(int)boundingBox2D.min(0),
				(int)boundingBox2D.min(1),
				(int)boundingBox2D.max(0),
				(int)boundingBox2D.max(1)
			};
			return efficientSamJ.processBox(bbox);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(LONG_NAME+", providing empty result because of some trouble: "+e.getMessage());
			e.printStackTrace();
			return EMPTY_POLYGON;
		}
	}

	@Override
	public void notifyUiHasBeenClosed() {
		log.info(LONG_NAME+": OKAY, I'm closing myself...");
		efficientSamJ.close();
	}
}
