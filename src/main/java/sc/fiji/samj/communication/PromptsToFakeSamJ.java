package sc.fiji.samj.communication;

import net.imglib2.Interval;
import net.imglib2.Localizable;
import org.scijava.log.Logger;
import java.awt.Polygon;
import java.util.List;

public class PromptsToFakeSamJ implements PromptsToNetAdapter {
	private static final Polygon EMPTY_POLYGON = new Polygon(new int[0], new int[0], 0);
	private final Logger log;

	public PromptsToFakeSamJ(final Logger log) {
		this.log = log;
	}

	@Override
	public String getNetName() {
		return "FakeSam";
	}

	@Override
	public Polygon fetch2dSegmentation(List<Localizable> listOfPoints2D) {
		log.info("FAKE SAM: LIST OF POINTS NOT IMPLEMENTED YET");
		return EMPTY_POLYGON;
	}

	@Override
	public Polygon fetch2dSegmentation(Localizable lineStartPoint2D, Localizable lineEndPoint2D) {
		int x1 = lineStartPoint2D.getIntPosition(0);
		int y1 = lineStartPoint2D.getIntPosition(1);
		int w = lineEndPoint2D.getIntPosition(0) -x1 +1;
		int h = lineEndPoint2D.getIntPosition(1) -y1 +1;
		final int[] xCoords = new int[8];
		final int[] yCoords = new int[8];
		createFakeRectangle(x1,y1,w,h, xCoords,yCoords,0.0);
		//score = Math.random();
		return new Polygon(xCoords,yCoords, xCoords.length);
	}

	@Override
	public Polygon fetch2dSegmentation(Interval boundingBox2D) {
		int x1 = (int)boundingBox2D.min(0);
		int y1 = (int)boundingBox2D.min(1);
		int w = (int)boundingBox2D.dimension(0);
		int h = (int)boundingBox2D.dimension(1);
		final int[] xCoords = new int[8];
		final int[] yCoords = new int[8];
		createFakeRectangle(x1,y1,w,h, xCoords,yCoords,0.1);
		//score = Math.random();
		return new Polygon(xCoords,yCoords, xCoords.length);
	}

	private void createFakeRectangle(final int x1,
	                                 final int y1,
	                                 final int w,
	                                 final int h,
	                                 final int[] xCoords,
	                                 final int[] yCoords,
	                                 final double variability) {
		xCoords[0] = x1 + rand(w * variability);
		yCoords[0] = y1 + rand(w * variability);
		xCoords[1] = x1 + w / 2;
		yCoords[1] = y1 + rand(w * variability);
		xCoords[2] = x1 + w - rand(w * variability);
		yCoords[2] = y1 + rand(w * variability);
		xCoords[3] = x1 + w - rand(w * variability);
		yCoords[3] = y1 + h / 2;
		xCoords[4] = x1 + w - rand(w * variability);
		yCoords[4] = y1 + h - rand(w * variability);
		xCoords[5] = x1 + w / 2;
		yCoords[5] = y1 + h - rand(w * variability);
		xCoords[6] = x1 + rand(w * variability);
		yCoords[6] = y1 + h - rand(w * variability);
		xCoords[7] = x1 + rand(w * variability);
		yCoords[7] = y1 + h / 2;
	}

	private int rand(double a) {
		return (int) (Math.random() * a);
	}

	@Override
	public void notifyUiHasBeenClosed() {
		log.info("FAKE SAM: OKAY, I'm closing myself...");
	}
}