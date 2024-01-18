package sc.fiji.samj.ui;

import net.imglib2.RandomAccessibleInterval;
import sc.fiji.samj.communication.PromptsToNetAdapter;
import java.util.List;
import java.awt.Polygon;

public interface PromptsResultsDisplay {

	void switchToThisImg(final RandomAccessibleInterval<?> newImage);
	void switchToThisNet(final PromptsToNetAdapter promptsToNetAdapter);
	void notifyNetToClose();

	List<Polygon> getPolygonsFromRoiManager();

	void enableAddingToRoiManager(boolean shouldBeAdding);
	boolean isAddingToRoiManager();

	void switchToUsingRectangles();
	void switchToUsingLines();
	void switchToUsingPoints();
}