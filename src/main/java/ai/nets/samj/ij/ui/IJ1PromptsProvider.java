/*-
 * #%L
 * Plugin to help image annotation with SAM-based Deep Learning models
 * %%
 * Copyright (C) 2024 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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

/**
 * Implementation of SAMJ {@link PromptsResultsDisplay} used to communicate with the SAMJ GUI and SAMJ models.
 * This class sends the prompts generated in ImageJ to SAMJ.
 * 
 * @author Vladimir Ulman
 * @author Carlos Garcia
 */
public class IJ1PromptsProvider implements PromptsResultsDisplay, MouseListener, KeyListener, WindowListener {
	/**
	 * The image being processed
	 */
	private final ImagePlus activeImage;
	/**
	 * SAM-based model selected to process the image
	 */
	private SAMModel promptsToNet;
	/**
	 * Instance of the ROI manager to save the ROIs created
	 */
	private final RoiManager roiManager;
	/**
	 * Whether to add the ROIs created to the ROI manager or not
	 */
	private boolean isAddingToRoiManager = true;
	/**
	 * Counter of the ROIs created
	 */
	private int promptsCreatedCnt = 0;
	/**
	 * Canvas of the image selected. Used to record the prompts drawn by the user
	 */
	private final ImageCanvas activeCanvas;
	/**
	 * Window of the selected image. Used to record the prompts drawn by the user
	 */
	private final ImageWindow activeWindow;
	/**
	 * TODO
	 * Logger, not used yet
	 */
	private final Logger lag;
	/**
	 * Whether the prompts being used are the bounding boxes
	 */
	private boolean isRect = false;
	/**
	 * Whether the prompts used are the points
	 */
	private boolean isPoints = false;
	/**
	 * Whether the prompt being used is the freehand
	 */
	private boolean isFreehand = false;
	/**
	 * A list to save several ROIs that are being created for the same prompt.
	 * Whenever the prompt is sent to the model, this list is emptied
	 */
	private List<Roi> temporalROIs = new ArrayList<Roi>();
	/**
	 * A list to save several ROIs that are being created from the same prompt.
	 * This list saves only the "negative" ROIs, those that are not part of the instance of interest,
	 * but part of the background.
	 * Whenever the prompt is sent to the model, this list is emptied. 
	 */
	private List<Roi> temporalNegROIs = new ArrayList<Roi>();
	/**
	 * For the point prompts, whether if hte user is collecting several prompts (pressing the ctrl key)
	 * or just one
	 */
	private boolean isCollectingPoints = false;
	/**
	 * All the points being collected that reference the instance of interest
	 */
	private List<Localizable> collectedPoints = new ArrayList<Localizable>();
	/**
	 * All the points being collected that reference the background (ctrl + alt)
	 */
	private List<Localizable> collecteNegPoints = new ArrayList<Localizable>();
	/**
	 * The number of words per line in the error message dialogs
	 */
	private static int WORDS_PER_LINE_ERR_MSG = 7;
	
	/**
	 * Instance of SAMJ {@link PromptsResultsDisplay} that sends the prompts over the image of interest
	 * to SAMJ.
	 * @param imagePlus
	 * 	the image of interest that is being annotated/processed
	 * @param log
	 * 	logging of the events of the GUI and network
	 */
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
	/**
	 * {@inheritDoc}
	 * 
	 * Get the ImageJ {@link ImagePlus} that is going to be processed by the wanted model
	 * and convert it to a {@link RandomAccessibleInterval} that fulfills the conditions of the model
	 */
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
	/**
	 * {@inheritDoc}
	 */
	public void notifyException(SAMJException type, Exception ex) {
		String msg = ex.getMessage();
		String nMessage = "";
		if (msg != null) {
			String[] msgArr = msg.split(" ");
			int lines = msgArr.length / WORDS_PER_LINE_ERR_MSG;
			int firstEnd = 0;
			for (int i = 1; i < lines + 1; i ++) {
				String lastWord = msgArr[Math.min(WORDS_PER_LINE_ERR_MSG * i, msgArr.length - 1)];
				int end = msg.substring(firstEnd).indexOf(lastWord) + lastWord.length();
				nMessage += msg.substring(firstEnd, firstEnd + end) + System.lineSeparator();
				firstEnd = end + firstEnd;
			}
			nMessage += msg.substring(firstEnd, msg.length());
		}
		if (SAMJException.ENCODING == type) {
			IJ.error("Error with the image being used", "Make sure that the image being used is 2D 1-channel or 3-channels.");
		} else if (SAMJException.ENCODING == type) {
			IJ.error("Error providing or processing the prompts", nMessage);
		} else {
			IJ.error(nMessage);
		}
		
	}

	
	@Override
	/**
	 * {@inheritDoc}
	 * 
	 */
	public void switchToThisNet(final SAMModel promptsToNetAdapter) {
		this.promptsToNet = promptsToNetAdapter;
		this.registerListeners();
	}
	
	@Override
	/**
	 * {@inheritDoc}
	 */
	public void notifyNetToClose() {
		//TODO log.info("Image window: Stopping service, stopping network");
		deRegisterListeners();
		if (promptsToNet != null) promptsToNet.notifyUiHasBeenClosed();
		this.promptsToNet = null;
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public List<Polygon> getPolygonsFromRoiManager() {
		//TODO log.error("Sorry, retrieving collected Polygons is not yet implemented.");
		//TODO: we would use the TODO infrastructure for this, as this infrastructure
		//      is probably going to memorize both inputs and their outputs... and outpus
		//      is what this method is after
		return Collections.emptyList();
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public void enableAddingToRoiManager(boolean shouldBeAdding) {
		this.isAddingToRoiManager = shouldBeAdding;
	}
	
	@Override
	/**
	 * {@inheritDoc}
	 */
	public boolean isAddingToRoiManager() {
		return this.isAddingToRoiManager;
	}

	/**
	 * Add the listeners to the new image once the image is selected
	 */
	private void registerListeners() {
		activeCanvas.addMouseListener(this);
		activeCanvas.addKeyListener(this);
		activeWindow.addWindowListener(this);
	}

	/**
	 * Remove the listeners from the image once the image is changed or de-selected
	 */
	private void deRegisterListeners() {
		activeCanvas.removeMouseListener(this);
		activeCanvas.removeKeyListener(this);
		activeWindow.removeWindowListener(this);
	}

	@Override
	/**
	 * Once the mouse is released, if there is any image active and prompt mode selected, process
	 * the new ROI and send it to the SAMJ model
	 */
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
				submitRectPrompt(rectInterval);
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

	/**
	 * Add the new roi to the ROI manager
	 * @param polys
	 * 	list of polygons that will be converted into polygon ROIs and sent to the ROI manager
	 * @param promptShape
	 * 	String giving information about which prompt was used to generate the ROI
	 */
	void addToRoiManager(final List<Polygon> polys, final String promptShape) {
		promptsCreatedCnt++;
		int resNo = 1;
		for (Polygon p : polys) this.addToRoiManager(p, resNo++, promptShape);
	}
	
	/**
	 * Add a single polygon to the ROI manager
	 * @param p
	 * 	the polygon to be added
	 * @param resultNumber
	 * 	this is for when various polygon are added at the same time from one prompt.
	 * 	It is the position in the list of polygons produced by SAM, if not just 0
	 * @param promptShape
	 * 	String giving information about which prompt was used to generate the ROI
	 */
	public void addToRoiManager(final Polygon p, final int resultNumber, final String promptShape) {
		final PolygonRoi pRoi = new PolygonRoi(p, PolygonRoi.POLYGON);
		pRoi.setName(promptsCreatedCnt+"."+resultNumber+"-"+promptShape+"-"+promptsToNet.getName());
		if (isAddingToRoiManager) roiManager.addRoi(pRoi);
	}

	/**
	 * Send the point prompts to SAM and clear the lists collecting them
	 */
	private void submitAndClearPoints() {
		if (promptsToNet == null) return;
		if (collectedPoints.size() == 0) return;

		//TODO log.info("Image window: Processing now points, this count: "+collectedPoints.size());
		isCollectingPoints = false;
		activeImage.deleteRoi();
		try {
			addToRoiManager(promptsToNet.fetch2dSegmentation(collectedPoints, collecteNegPoints),
					(collectedPoints.size() > 1 ? "points" : "point"));
		} catch (Exception ex) {
			this.notifyException(SAMJException.DECODING, ex);
		}
		collectedPoints = new ArrayList<Localizable>();
		collecteNegPoints = new ArrayList<Localizable>();
		temporalROIs = new ArrayList<Roi>();
		temporalNegROIs = new ArrayList<Roi>();
	}
	
	private void submitRectPrompt(Interval rectInterval) {
		try {
			addToRoiManager(promptsToNet.fetch2dSegmentation(rectInterval), "rect");
		} catch (Exception ex) {
			this.notifyException(SAMJException.DECODING, ex);
		}
	}

	@Override
	/**
	 * Monitor when the control key is being released for the point prompts.
	 * Whenever it is released and the point prompt is selected, the points that have already been drawn 
	 * are sent to SAMJ
	 */
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
			submitAndClearPoints();
		}
	}

	@Override
	/**
	 * when the plugin is closed, close everythng
	 */
	public void windowClosed(WindowEvent e) {
		roiManager.close();
		notifyNetToClose();
	}

	@Override
	/**
	 * Select the bounding box tool to send bounding box prompts to SAMJ
	 */
	public void switchToUsingRectangles() {
		IJ.setTool(Toolbar.RECT_ROI);
		this.isRect = true;
		this.isPoints = false;
		this.isFreehand = false;
	}
	
	@Override
	/**
	 * Select the brush tool to send freehand line prompts to SAMJ
	 */
	public void switchToUsingBrush() {
		IJ.setTool(Toolbar.FREELINE);
		this.isRect = false;
		this.isPoints = false;
		this.isFreehand = true;
	}
	
	@Override
	/**
	 * Select the points tool to send point prompts to SAMJ
	 */
	public void switchToUsingPoints() {
		IJ.setTool(Toolbar.POINT);
		this.isRect = true;
		this.isPoints = true;
		this.isFreehand = false;
	}

	@Override
	/**
	 * Do not send any prompt of the ROIs generated to SAMJ
	 */
	public void switchToNone() {
		this.isRect = false;
		this.isPoints = false;
		this.isFreehand = false;
	}

	@Override
	/**
	 * {@inheritDoc}
	 * 
	 * Get the selected ImageJ {@link ImagePlus} as an Object
	 */
	public Object getFocusedImage() {
		return this.activeImage;
	}

	@Override
	/**
	 * {@inheritDoc}
	 * 
	 * Use an existing mask as the prompt. Not working well yet. TODO
	 */
	public void improveExistingMask(File mask) {
		try {
			ImagePlus imp = IJ.openImage(mask.getAbsolutePath());
			List<Polygon> pols = this.promptsToNet.fetch2dSegmentationFromMask(Cast.unchecked(ImageJFunctions.wrap(imp)));
			addToRoiManager(pols, "existing-mask"); 
		} catch (Exception ex) {
			throw new IllegalArgumentException("The file selected does not correspond to an image.");
		}
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
