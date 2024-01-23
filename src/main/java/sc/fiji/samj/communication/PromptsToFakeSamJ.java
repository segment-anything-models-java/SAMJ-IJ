package sc.fiji.samj.communication;

import net.imglib2.Interval;
import net.imglib2.Localizable;
import org.scijava.log.Logger;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

public class PromptsToFakeSamJ implements PromptsToNetAdapter {
	private final Logger log;
	private final String fakeNetworkName;

	public PromptsToFakeSamJ(final Logger log, final String fakeSamName) {
		this.log = log;
		this.fakeNetworkName = fakeSamName;
	}

	@Override
	public String getNetName() {
		return fakeNetworkName;
	}

	@Override
	public List<Polygon> fetch2dSegmentation(List<Localizable> listOfPoints2D) {
		final int[] xCoords = new int[8];
		final int[] yCoords = new int[8];
		//a small rectangle around the first point
		int x = listOfPoints2D.get(0).getIntPosition(0);
		int y = listOfPoints2D.get(0).getIntPosition(1);
		int w = 6;
		x -= w/2;
		y -= w/2;
		createFakeRectangle(x,y,w,w, xCoords,yCoords,0.0);
		//score = Math.random();
		List<Polygon> retList = new ArrayList<>(1);
		retList.add( new Polygon(xCoords,yCoords, xCoords.length) );
		return retList;
	}

	@Override
	public List<Polygon> fetch2dSegmentation(Localizable lineStartPoint2D, Localizable lineEndPoint2D) {
		int x1 = lineStartPoint2D.getIntPosition(0);
		int y1 = lineStartPoint2D.getIntPosition(1);
		int w = lineEndPoint2D.getIntPosition(0) -x1 +1;
		int h = lineEndPoint2D.getIntPosition(1) -y1 +1;
		final int[] xCoords = new int[8];
		final int[] yCoords = new int[8];
		createFakeRectangle(x1,y1,w,h, xCoords,yCoords,0.0);
		//score = Math.random();
		List<Polygon> retList = new ArrayList<>(1);
		retList.add( new Polygon(xCoords,yCoords, xCoords.length) );
		return retList;
	}

	@Override
	public List<Polygon> fetch2dSegmentation(Interval boundingBox2D) {
		int x1 = (int)boundingBox2D.min(0);
		int y1 = (int)boundingBox2D.min(1);
		int w = (int)boundingBox2D.dimension(0);
		int h = (int)boundingBox2D.dimension(1);
		final int[] xCoords = new int[8];
		final int[] yCoords = new int[8];
		createFakeRectangle(x1,y1,w,h, xCoords,yCoords,0.1);
		//score = Math.random();
		List<Polygon> retList = new ArrayList<>(1);
		retList.add( new Polygon(xCoords,yCoords, xCoords.length) );
		return retList;
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
		log.info("FAKE SAM "+fakeNetworkName+": OKAY, I'm closing myself...");
	}
}