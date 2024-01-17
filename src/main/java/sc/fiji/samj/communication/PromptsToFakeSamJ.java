package sc.fiji.samj.communication;

import net.imglib2.Interval;
import net.imglib2.Localizable;
import java.awt.Polygon;
import java.util.List;

public class PromptsToFakeSamJ implements PromptsToNetAdapter {
	private static final Polygon EMPTY_POLYGON = new Polygon(new int[0], new int[0], 0);

	@Override
	public Polygon fetch2dSegmentation(List<Localizable> listOfPoints2D) {
		System.out.println("NOT IMPLEMENTED YET");
		return EMPTY_POLYGON;
	}

	@Override
	public Polygon fetch2dSegmentation(Localizable lineStartPoint2D, Localizable lineEndPoint2D) {
		System.out.println("NOT IMPLEMENTED YET");
		return EMPTY_POLYGON;
	}

	@Override
	public Polygon fetch2dSegmentation(Interval boundingBox2D) {
		int x1 = (int)boundingBox2D.min(0);
		int y1 = (int)boundingBox2D.min(1);
		int h = (int)boundingBox2D.dimension(0);
		int w = (int)boundingBox2D.dimension(1);
		final int[] xCoords = new int[8];
		final int[] yCoords = new int[8];
		xCoords[0] = x1 + rand(w * 0.2);
		yCoords[0] = y1 + rand(w * 0.2);
		xCoords[1] = x1 + w / 2;
		yCoords[1] = y1 + rand(w * 0.1);
		xCoords[2] = x1 + w - rand(w * 0.2);
		yCoords[2] = y1 + rand(w * 0.1);
		xCoords[3] = x1 + w - rand(w * 0.2);
		yCoords[3] = y1 + h / 2;
		xCoords[4] = x1 + w - rand(w * 0.2);
		yCoords[4] = y1 + h - rand(w * 0.2);
		xCoords[5] = x1 + w / 2;
		yCoords[5] = y1 + h - rand(w * 0.3);
		xCoords[6] = x1 + rand(w * 0.2);
		yCoords[6] = y1 + h - rand(w * 0.2);
		xCoords[7] = x1 + rand(w * 0.2);
		yCoords[7] = y1 + h / 2;
		//score = Math.random();
		return new Polygon(xCoords,yCoords, xCoords.length);
	}

	private int rand(double a) {
		return (int) (Math.random() * a);
	}

	@Override
	public void NotifyUiHasBeenClosed() {
		System.out.println("FAKE SAM: OKAY, I'm closing myself...");
	}
}