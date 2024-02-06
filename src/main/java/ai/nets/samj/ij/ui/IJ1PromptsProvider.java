package ai.nets.samj.ij.ui;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.frame.RoiManager;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.scijava.log.Logger;
import ai.nets.samj.communication.PromptsToNetAdapter;
import ai.nets.samj.ui.PromptsResultsDisplay;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class IJ1PromptsProvider implements PromptsResultsDisplay, MouseListener, KeyListener, WindowListener {

	//remember provided arguments
	private final ImagePlus activeImage;
	private PromptsToNetAdapter promptsToNet; //NB: user may want to use different networks along the way
	private final RoiManager roiManager;
	private boolean isAddingToRoiManager = true;
	private int promptsCreatedCnt = 0;

	//shortcuts...
	private final ImageCanvas activeCanvas;
	private final ImageWindow activeWindow;

	private final Logger log;

	public IJ1PromptsProvider(final ImagePlus imagePlus,
	                          final Logger log) {
		this.promptsToNet = null;
		this.roiManager = startRoiManager();
		this.activeImage = imagePlus;
		this.log = log;

		activeCanvas = activeImage.getCanvas();
		activeWindow = activeImage.getWindow();

		//make sure we start with no ROIs at all
		activeImage.killRoi();
		registerListeners();
	}

	private RoiManager startRoiManager() {
		RoiManager roiManager = RoiManager.getInstance();
		if (roiManager == null) {
			roiManager = new RoiManager();
		}
		roiManager.setVisible(true);
		roiManager.setTitle("SAM Roi Manager");
		Prefs.useNamesAsLabels = true;
		roiManager.setEditMode(activeImage, true);
		return roiManager;
	}

	@Override
	public void switchToThisImg(final RandomAccessibleInterval<?> newImage) {
		log.error("Sorry, switching to new image is not yet implemented.");
	}

	@Override
	public RandomAccessibleInterval<?> giveProcessedSubImage() {
		//the IJ1 image operates always on the full image
		return ImageJFunctions.wrap(activeImage);
	}

	@Override
	public void switchToThisNet(final PromptsToNetAdapter promptsToNetAdapter) {
		this.promptsToNet = promptsToNetAdapter;
	}
	@Override
	public void notifyNetToClose() {
		log.info("Image window: Stopping service, stopping network");
		deRegisterListeners();
		if (promptsToNet != null) promptsToNet.notifyUiHasBeenClosed();
		this.promptsToNet = null;
	}

	@Override
	public List<Polygon> getPolygonsFromRoiManager() {
		log.error("Sorry, retrieving collected Polygons is not yet implemented.");
		//TODO: we would use the TODO infrastructure for this, as this infrastructure
		//      is probably going to memorize both inputs and their outputs... and outpus
		//      is what this method is after
		return Collections.emptyList();
	}

	@Override
	public void enableAddingToRoiManager(boolean shouldBeAdding) {
		this.isAddingToRoiManager = shouldBeAdding;
	}
	@Override
	public boolean isAddingToRoiManager() {
		return this.isAddingToRoiManager;
	}

	private void registerListeners() {
		activeCanvas.addMouseListener(this);
		activeCanvas.addKeyListener(this);
		activeWindow.addWindowListener(this);
	}

	private void deRegisterListeners() {
		activeCanvas.removeMouseListener(this);
		activeCanvas.removeKeyListener(this);
		activeWindow.removeWindowListener(this);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		final Roi roi = activeImage.getRoi();
		if (roi == null) {
			log.info("Image window: There's no ROI...");
			return;
		}

		if (promptsToNet == null) {
			log.warn("Please, choose some SAM implementation first before we can be sending prompts to it.");
			activeImage.deleteRoi();
			return;
		}

		switch (roi.getType()) {
			case Roi.RECTANGLE:
				log.info("Image window: rectangle...");
				//
				final Rectangle rectBounds = roi.getBounds();
				final Interval rectInterval = new FinalInterval(
						new long[] { rectBounds.x, rectBounds.y },
						new long[] { rectBounds.x+rectBounds.width-1, rectBounds.y+rectBounds.height-1 } );
				addToRoiManager(promptsToNet.fetch2dSegmentation(rectInterval), "rect");
				break;

			case Roi.LINE:
				Iterator<java.awt.Point> it = roi.iterator();
				java.awt.Point pit = it.next(); //NB: since Roi != null, the point for sure exists...
				Point p1 = new Point(pit.x, pit.y);
				while (it.hasNext()) pit = it.next(); //find the last point on the line
				Point p2 = new Point(pit.x, pit.y);
				log.info("Image window: line... from "+p1+" to "+p2);
				//
				addToRoiManager(promptsToNet.fetch2dSegmentation(p1,p2), "line");
				break;

			case Roi.POINT:
				if (e.isShiftDown()) {
					//add point to the list only
					isCollectingPoints = true;
					java.awt.Point p = roi.iterator().next();  //NB: since Roi != null, the point for sure exists...
					collectedPoints.add( new Point(p.x,p.y) ); //NB: add ImgLib2 Point
					log.info("Image window: collecting points..., already we have: "+collectedPoints.size());
				} else {
					isCollectingPoints = false;
					//collect this last one
					java.awt.Point p = roi.iterator().next(); //NB: since Roi != null, the point for sure exists...
					collectedPoints.add( new Point(p.x,p.y) );
					submitAndClearPoints();
				}
				break;

			default:
				log.info("Image window: unsupported ROI type");
		}

		if (!isCollectingPoints) activeImage.deleteRoi();
	}

	void addToRoiManager(final List<Polygon> polys, final String promptShape) {
		promptsCreatedCnt++;
		int resNo = 1;
		for (Polygon p : polys) this.addToRoiManager(p, resNo++, promptShape);
	}

	void addToRoiManager(final Polygon p, final int resultNumber, final String promptShape) {
		final PolygonRoi pRoi = new PolygonRoi(p, PolygonRoi.POLYGON);
		pRoi.setName(promptsCreatedCnt+"."+resultNumber+"-"+promptShape+"-"+promptsToNet.getNetName());
		if (isAddingToRoiManager) roiManager.addRoi(pRoi);
	}

	private boolean isCollectingPoints = false;
	private final List<Localizable> collectedPoints = new ArrayList<>(100);

	private void submitAndClearPoints() {
		if (promptsToNet == null) return;
		if (collectedPoints.size() == 0) return;

		log.info("Image window: Processing now points, this count: "+collectedPoints.size());
		isCollectingPoints = false;
		activeImage.deleteRoi();
		addToRoiManager(promptsToNet.fetch2dSegmentation(collectedPoints),
				(collectedPoints.size() > 1 ? "points" : "point"));
		collectedPoints.clear();
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			submitAndClearPoints();
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {
		roiManager.close();
		notifyNetToClose();
	}

	@Override
	public void switchToUsingRectangles() {
		IJ.setTool(Toolbar.RECT_ROI);
	}
	@Override
	public void switchToUsingLines() {
		IJ.setTool(Toolbar.LINE);
	}
	@Override
	public void switchToUsingPoints() {
		IJ.setTool(Toolbar.POINT);
	}

	// ===== unused events =====
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void windowOpened(WindowEvent e) {}
	@Override
	public void windowClosing(WindowEvent e) {}
	@Override
	public void windowIconified(WindowEvent e) {}
	@Override
	public void windowDeiconified(WindowEvent e) {}
	@Override
	public void windowActivated(WindowEvent e) {}
	@Override
	public void windowDeactivated(WindowEvent e) {}
	@Override
	public void keyTyped(KeyEvent e) {
	}
	@Override
	public void keyPressed(KeyEvent e) {
	}
}
