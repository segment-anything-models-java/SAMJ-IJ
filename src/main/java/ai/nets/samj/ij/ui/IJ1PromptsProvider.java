package ai.nets.samj.ij.ui;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.CompositeConverter;
import ij.plugin.OverlayLabels;
import ij.plugin.frame.RoiManager;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;

import org.scijava.log.Logger;

import ai.nets.samj.communication.model.EfficientSAM;
import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.ui.PromptsResultsDisplay;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class IJ1PromptsProvider implements PromptsResultsDisplay, MouseListener, KeyListener, WindowListener {

	//remember provided arguments
	private final ImagePlus activeImage;
	private SAMModel promptsToNet; //NB: user may want to use different networks along the way
	private final RoiManager roiManager;
	private boolean isAddingToRoiManager = true;
	private int promptsCreatedCnt = 0;
	

	//shortcuts...
	private final ImageCanvas activeCanvas;
	private final ImageWindow activeWindow;

	private final Logger lag;
	
	private boolean isRect = false;
	private boolean isPoints = false;
	private boolean isFreehand = false;

	private List<Roi> temporalROIs = new ArrayList<Roi>();
	private List<Roi> temporalNegROIs = new ArrayList<Roi>();
	
	public IJ1PromptsProvider(final ImagePlus imagePlus,
	                          final Logger log) {
		this.promptsToNet = null;
		this.roiManager = startRoiManager();
		activeImage = imagePlus;
		this.lag = log;

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
		roiManager.reset();
		roiManager.setVisible(true);
		roiManager.setTitle("SAM Roi Manager");
		Prefs.useNamesAsLabels = true;
		roiManager.setEditMode(activeImage, true);
		return roiManager;
	}

	@Override
	public RandomAccessibleInterval<?> giveProcessedSubImage(SAMModel selectedModel) {
		//the IJ1 image operates always on the full image
		if (selectedModel.getName().equals(EfficientSAM.FULL_NAME)) {
			Img<?> image = ImageJFunctions.wrap(activeImage.getType() == 4 ? CompositeConverter.makeComposite(activeImage) : activeImage);
			return Cast.unchecked(Views.permute(image, 0, 1));
		} else {
			Img<?> image = ImageJFunctions.wrap(activeImage.getType() == 4 ? CompositeConverter.makeComposite(activeImage) : activeImage);
			return Cast.unchecked(Views.permute(image, 0, 1));
			//return Cast.unchecked(ImageJFunctions.wrap(activeImage));
		}
	}

	@Override
	public void switchToThisNet(final SAMModel promptsToNetAdapter) {
		this.promptsToNet = promptsToNetAdapter;
		this.registerListeners();
	}
	@Override
	public void notifyNetToClose() {
		//TODO log.info("Image window: Stopping service, stopping network");
		deRegisterListeners();
		if (promptsToNet != null) promptsToNet.notifyUiHasBeenClosed();
		this.promptsToNet = null;
	}

	@Override
	public List<Polygon> getPolygonsFromRoiManager() {
		//TODO log.error("Sorry, retrieving collected Polygons is not yet implemented.");
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
		if (!this.isRect && !this.isPoints && !this.isFreehand) return;
		final Roi roi = activeImage.getRoi();
		if (roi == null) {
			//TODO log.info("Image window: There's no ROI...");
			return;
		}

		if (promptsToNet == null) {
			//TODO log.warn("Please, choose some SAM implementation first before we can be sending prompts to it.");
			activeImage.deleteRoi();
			return;
		}

		switch (roi.getType()) {
			case Roi.RECTANGLE:
				if (!isRect) break;
				//TODO log.info("Image window: rectangle...");
				//
				final Rectangle rectBounds = roi.getBounds();
				final Interval rectInterval = new FinalInterval(
						new long[] { rectBounds.x, rectBounds.y },
						new long[] { rectBounds.x+rectBounds.width-1, rectBounds.y+rectBounds.height-1 } );
				addToRoiManager(promptsToNet.fetch2dSegmentation(rectInterval), "rect");
				break;

			case Roi.FREELINE:
				if (!isFreehand) break;
				// TODO this is not a real mask prompt, it is just taking
				// TODO all the points in a line and using them, modify it for a true mask
				if (e.isControlDown() && e.isAltDown()) {
					temporalNegROIs.add(roi);
					roi.setStrokeColor(Color.red);
					isCollectingPoints = true;
					Iterator<java.awt.Point> it = roi.iterator();
					while (it.hasNext()) {
						java.awt.Point p = it.next();
						collecteNegPoints.add(new Point(p.x,p.y)); 
					}
					addTemporalRois();
				} else if (e.isControlDown()) {
					temporalROIs.add(roi);
					isCollectingPoints = true;
					Iterator<java.awt.Point> it = roi.iterator();
					while (it.hasNext()) {
						java.awt.Point p = it.next();
						collectedPoints.add(new Point(p.x,p.y)); 
					}
					addTemporalRois();
				} else {
					isCollectingPoints = false;
					Iterator<java.awt.Point> it = roi.iterator();
					while (it.hasNext()) {
						java.awt.Point p = it.next();
						collectedPoints.add(new Point(p.x,p.y)); 
					}
					submitAndClearPoints();
				}
				//TODO log.info("Image window: line... from "+p1+" to "+p2);
				//addToRoiManager(promptsToNet.fetch2dSegmentation(p1,p2), "line");
				break;
			case Roi.POINT:
				if (!isPoints) break;
				if (e.isControlDown() && e.isAltDown()) {
					roi.setFillColor(Color.red);
					//add point to the list only
					isCollectingPoints = true;
					Iterator<java.awt.Point> iterator = roi.iterator();
					java.awt.Point p = iterator.next();
					while (iterator.hasNext()) p = iterator.next();
					collecteNegPoints.add( new Point(p.x,p.y) ); //NB: add ImgLib2 Point
					//TODO log.info("Image window: collecting points..., already we have: "+collectedPoints.size());
				} else if (e.isControlDown()) {
					//add point to the list only
					isCollectingPoints = true;
					Iterator<java.awt.Point> iterator = roi.iterator();
					java.awt.Point p = iterator.next();
					while (iterator.hasNext()) p = iterator.next();
					collectedPoints.add( new Point(p.x,p.y) ); //NB: add ImgLib2 Point
					//TODO log.info("Image window: collecting points..., already we have: "+collectedPoints.size());
				} else {
					isCollectingPoints = false;
					//collect this last one
					Iterator<java.awt.Point> iterator = roi.iterator();
					java.awt.Point p = iterator.next();
					while (iterator.hasNext()) p = iterator.next();
					collectedPoints.add( new Point(p.x,p.y) );
					submitAndClearPoints();
				}
				break;

			default:
				//TODO log.info("Image window: unsupported ROI type");
		}

		if (!isCollectingPoints) activeImage.deleteRoi();
	}
	
	private void addTemporalRois() {
		//Overlay overlay = activeCanvas.getOverlay();
		Overlay overlay = OverlayLabels.createOverlay();
		for (Roi rr : this.roiManager.getRoisAsArray())
			overlay.add(rr);
		this.temporalROIs.stream().forEach(r -> overlay.add(r));
		this.temporalNegROIs.stream().forEach(r -> overlay.add(r));
		activeCanvas.setShowAllList(overlay);
		this.activeImage.draw();
	}

	void addToRoiManager(final List<Polygon> polys, final String promptShape) {
		promptsCreatedCnt++;
		int resNo = 1;
		for (Polygon p : polys) this.addToRoiManager(p, resNo++, promptShape);
	}

	void addToRoiManager(final Polygon p, final int resultNumber, final String promptShape) {
		final PolygonRoi pRoi = new PolygonRoi(p, PolygonRoi.POLYGON);
		pRoi.setName(promptsCreatedCnt+"."+resultNumber+"-"+promptShape+"-"+promptsToNet.getName());
		if (isAddingToRoiManager) roiManager.addRoi(pRoi);
	}

	private boolean isCollectingPoints = false;
	private List<Localizable> collectedPoints = new ArrayList<Localizable>();
	private List<Localizable> collecteNegPoints = new ArrayList<Localizable>();

	private void submitAndClearPoints() {
		if (promptsToNet == null) return;
		if (collectedPoints.size() == 0) return;

		//TODO log.info("Image window: Processing now points, this count: "+collectedPoints.size());
		isCollectingPoints = false;
		activeImage.deleteRoi();
		addToRoiManager(promptsToNet.fetch2dSegmentation(collectedPoints, collecteNegPoints),
				(collectedPoints.size() > 1 ? "points" : "point"));
		collectedPoints = new ArrayList<Localizable>();
		collecteNegPoints = new ArrayList<Localizable>();
		temporalROIs = new ArrayList<Roi>();
		temporalNegROIs = new ArrayList<Roi>();
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
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
		this.isRect = true;
		this.isPoints = false;
		this.isFreehand = false;
	}
	@Override
	public void switchToUsingBrush() {
		IJ.setTool(Toolbar.FREELINE);
		this.isRect = false;
		this.isPoints = false;
		this.isFreehand = true;
	}
	@Override
	public void switchToUsingPoints() {
		IJ.setTool(Toolbar.POINT);
		this.isRect = true;
		this.isPoints = true;
		this.isFreehand = false;
	}

	@Override
	public void switchToNone() {
		this.isRect = false;
		this.isPoints = false;
		this.isFreehand = false;
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

	@Override
	public Object getFocusedImage() {
		return this.activeImage;
	}

	@Override
	public void improveExistingMask(File mask) {
		try {
			ImagePlus imp = IJ.openImage(mask.getAbsolutePath());
			List<Polygon> pols = this.promptsToNet.fetch2dSegmentationFromMask(Cast.unchecked(ImageJFunctions.wrap(imp)));
			addToRoiManager(pols, "existing-mask"); 
		} catch (Exception ex) {
			throw new IllegalArgumentException("The file selected does not correspond to an image.");
		}
	}
}
