package sc.fiji.samj.communication;

import net.imglib2.Interval;
import net.imglib2.Localizable;
import java.awt.Polygon;
import java.util.List;

public interface PromptsToNetAdapter {

	List<Polygon> fetch2dSegmentation(List<Localizable> listOfPoints2D);

	List<Polygon> fetch2dSegmentation(Localizable lineStartPoint2D, Localizable lineEndPoint2D);

	List<Polygon> fetch2dSegmentation(Interval boundingBox2D);

	String getNetName();

	void notifyUiHasBeenClosed();
}