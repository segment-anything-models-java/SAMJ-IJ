package ai.nets.samj.ij.ui;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.ij.utils.RoiManagerPrivateViolator;
import ai.nets.samj.models.AbstractSamJ;
import ai.nets.samj.ui.ConsumerInterface;
import ij.IJ;
import ij.IJEventListener;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.CompositeConverter;
import ij.plugin.OverlayLabels;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.RoiManager;
import io.bioimage.modelrunner.system.PlatformDetection;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 * 
 * @author Carlos Garcia Lopez de Haro
 */
public class Consumer extends ConsumerInterface implements MouseListener, KeyListener, WindowListener, IJEventListener {
	/**
	 * The image being processed
	 */
	private ImagePlus activeImage;
	/**
	 * Canvas of the image selected. Used to record the prompts drawn by the user
	 */
	private ImageCanvas activeCanvas;
	/**
	 * Window of the selected image. Used to record the prompts drawn by the user
	 */
	private ImageWindow activeWindow;
	/**
	 * Instance of the ROI manager to save the ROIs created
	 */
	private RoiManager roiManager;
	/**
	 * Whether to add the ROIs created to the ROI manager or not
	 */
	private boolean isAddingToRoiManager = true;
	/**
	 * Counter of the ROIs created
	 */
	private int promptsCreatedCnt = 0;
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
	 * Save lists of rois that have been added at the same time to delete them if necessary
	 */
    private Stack<List<PolygonRoi>> undoStack = new Stack<>();
    /**
     * Save lists of polygons deleted at the same time to undo their deleting
     */
    private Stack<List<PolygonRoi>> redoStack = new Stack<>();
    /**
     * List of the annotated masks on an image
     */
    private Stack<List<Mask>> annotatedMask = new Stack<List<Mask>>();
    /**
     * List that keeps track of the annotated masks
     */
    private Stack<List<Mask>> redoAnnotatedMask = new Stack<List<Mask>>();
    /**
     * Tracks if Ctrl+Z has already been handled
     */
    private boolean undoPressed = false;
    /**
     * Tracks if Ctrl+Y has already been handled
     */
    private boolean redoPressed = false;
    /**
     * Whether the SAMJ specific listeners are registered or not.
     */
    private boolean registered = false;
    
    public Consumer() {
    	IJ.addEventListener(this);
    }

	@Override
	/**
	 * {@inheritDoc}
	 * 
	 * GEt the list of open images in ImageJ
	 */
	public List<ComboBoxItem> getListOfOpenImages() {
		return Arrays.stream(WindowManager.getImageTitles())
				.map(title -> new IJComboBoxItem((Object) WindowManager.getImage(title)))
				.collect(Collectors.toList());
	}
	
	@Override
	public void addPolygonsFromGUI(List<Mask> masks) {
		// TODO improve the naming
		this.addToRoiManager(masks, "batch");
	}

	@Override
	public List<Polygon> getPolygonsFromRoiManager() {
		return Arrays.stream(roiManager.getRoisAsArray()).map(i -> i.getPolygon()).collect(Collectors.toList());
	}

	@Override
	public void enableAddingToRoiManager(boolean shouldBeAdding) {
		this.isAddingToRoiManager = shouldBeAdding;
	}

	@Override
	public void exportImageLabeling() {
		if (Recorder.record)
			Recorder.recordString("run(\"SAMJ Annotator\", \"export=true\");" + System.lineSeparator());
		int width = activeImage.getWidth();
		int height = activeImage.getHeight();
		List<Mask> masks = new ArrayList<Mask>();
		this.annotatedMask.stream().forEach(mm -> masks.addAll(mm));
		RandomAccessibleInterval<UnsignedShortType> raiMask = Mask.getMask(width, height, masks);
		ImagePlus impMask = ImageJFunctions.show(raiMask);
		impMask.setTitle(activeImage.getTitle() + "-labeling");
		impMask.getProcessor().setMinAndMax(0, annotatedMask.size());
	}

	@Override
	public void activateListeners() {
		if (registered) return;
		activeCanvas.addMouseListener(this);
		activeCanvas.addKeyListener(this);
		activeWindow.addWindowListener(this);
		activeWindow.addKeyListener(this);

		activeCanvas.removeKeyListener(IJ.getInstance());
		activeWindow.removeKeyListener(IJ.getInstance());
		registered = true;
	}

	@Override
	public void deactivateListeners() {
		if (!registered) return;
		System.out.println(activeCanvas == null);
		activeCanvas.removeMouseListener(this);
		activeCanvas.removeKeyListener(this);
		activeWindow.removeWindowListener(this);
		activeWindow.removeKeyListener(this);
		
		activeWindow.addKeyListener(IJ.getInstance());
		activeCanvas.addKeyListener(IJ.getInstance());
		registered = false;
	}

	@Override
	public boolean isValidPromptSelected() {
		return Toolbar.getToolName().equals("rectangle")
				 || Toolbar.getToolName().equals("point")
				 || Toolbar.getToolName().equals("multipoint");
	}

	@Override
	public void setFocusedImage(Object image) {
		boolean changed = activeImage != (ImagePlus) image;
		if (!changed) {
			WindowManager.setCurrentWindow(activeWindow);
			return;
		}
		activeImage = (ImagePlus) image;
		this.activeCanvas = this.activeImage.getCanvas();
		this.activeWindow = this.activeImage.getWindow();
		if (this.isAddingToRoiManager)
			this.roiManager = startRoiManager();
	}

	@Override
	public void deselectImage() {
		activeImage = null;
		this.activeCanvas = null;
		this.activeWindow = null;
	}

	@Override
	public Object getFocusedImage() {
		return IJ.getImage();
	}

	@Override
	public String getFocusedImageName() {
		return IJ.getImage().getTitle();
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getFocusedImageAsRai() {
		ImagePlus imp = IJ.getImage();
		boolean isColorRGB = imp.getType() == ImagePlus.COLOR_RGB;
		Img<T> image = ImageJFunctions.wrap(isColorRGB ? CompositeConverter.makeComposite(imp) : imp);
		return image;
	}

	@Override
	public List<Rectangle> getRectRoisOnFocusImage() {
		Roi roi = IJ.getImage().getRoi();
		List<Rectangle> list = getRectRoisFromRoiManager();
		if (roi == null)
			return list;
		if (roi.getType() != Roi.RECTANGLE)
			return list;
		if (list.stream().anyMatch(a -> a.equals(roi.getBounds())))
			return list;
		list.add(roi.getBounds());
		return list;
	}

	@Override
	public List<int[]> getPointRoisOnFocusImage() {
		Roi roi = IJ.getImage().getRoi();
		List<int[]> list = getPointRoisFromRoiManager();
		if (roi == null)
			return list;
		if (roi.getType() != Roi.POINT)
			return list;
		Iterator<java.awt.Point> it = roi.iterator();
		while (it.hasNext()) {
			java.awt.Point p = it.next();
			int[] arr = new int[] {(int) p.getX(), (int) p.getY()};
			if (list.stream().anyMatch(a -> Arrays.equals(a, arr)))
				continue;
			list.add(arr);
		}
		return list;
	}

	@Override
	public void deletePointRoi(int[] pp) {
		Roi[] roiManagerRois = RoiManager.getInstance().getRoisAsArray();
		int ii = -1;
		if (roiManagerRois != null) {
			ii ++;
			for (Roi managerRoi : roiManagerRois) {
				if (managerRoi.getType() != Roi.POINT)
					continue;
				PointRoi pRoi = (PointRoi) managerRoi;
				Iterator<java.awt.Point> iter = pRoi.iterator();
				while (iter.hasNext()) {
					java.awt.Point point = iter.next();
					if (point.x == pp[0] && point.y == pp[1]) {
						pRoi.deleteHandle(pp[0], pp[1]);
						if (!iter.hasNext()) {
							try {
								RoiManagerPrivateViolator.deleteRoiAtPosition(RoiManager.getInstance(), ii);
							} catch (NoSuchFieldException | SecurityException | NoSuchMethodException
									| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								e.printStackTrace();
							}
						}
						return;
					}
				}
			}
		}
		Roi roi = IJ.getImage().getRoi();
		if (roi != null && roi.getType() == Roi.POINT) {
			PointRoi pRoi = (PointRoi) roi;
			Iterator<java.awt.Point> iter = roi.iterator();
			while (iter.hasNext()) {
				java.awt.Point point = iter.next();
				if (point.x == pp[0] && point.y == pp[1]) {
					pRoi.deleteHandle(pp[0], pp[1]);
					return;
				}
			}
		}
	}

	@Override
	public void deleteRectRoi(Rectangle rect) {
		Roi[] roiManagerRois = RoiManager.getInstance().getRoisAsArray();
		if (roiManagerRois != null) {
			for (int i = 0; i < roiManagerRois.length; i ++) {
				Roi managerRoi = roiManagerRois[i];
				if (managerRoi.getType() != Roi.RECTANGLE)
					continue;
				if (managerRoi.getBounds().equals(rect)) {
					try {
						RoiManagerPrivateViolator.deleteRoiAtPosition(RoiManager.getInstance(), i);
					} catch (NoSuchFieldException | SecurityException | NoSuchMethodException | IllegalAccessException
							| IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
					}
					return;
				}
			}
		}
		Roi roi = IJ.getImage().getRoi();
		if (roi != null && roi.getType() == Roi.RECTANGLE) {
			if (roi.getBounds().equals(rect)) {
				activeImage.deleteRoi();
				return;
			}
		}
	}
	
	private List<Rectangle> getRectRoisFromRoiManager() {
		List<Rectangle> list = new ArrayList<Rectangle>();
		Roi[] rois = RoiManager.getInstance().getRoisAsArray();
		if (rois.length == 0)
			return list;
		list = Arrays.stream(rois).filter(rr -> rr.getType() == Roi.RECTANGLE)
				.map(rr -> {
					rr.setImage(activeImage);
					return rr.getBounds();
				}).collect(Collectors.toList());
		return list;
	}
	
	private List<int[]> getPointRoisFromRoiManager() {
		List<int[]> list = new ArrayList<int[]>();
		Roi[] rois = RoiManager.getInstance().getRoisAsArray();
		if (rois.length == 0)
			return list;
		List<Roi> roiList = Arrays.stream(rois).filter(rr -> rr.getType() == Roi.POINT).collect(Collectors.toList());
		for (Roi rr : roiList) {
			Iterator<java.awt.Point> it = rr.iterator();
			while (it.hasNext()) {
				java.awt.Point p = it.next();
				list.add(new int[] {(int) p.getX(), (int) p.getY()});
			}
			rr.setImage(activeImage);
		}
		return list;
	}

	@Override
	/**
	 * when the plugin is closed, close everything
	 */
	public void windowClosed(WindowEvent e) {
		roiManager.close();
		this.selectedModel.closeProcess();
		this.selectedModel = null;
		this.deactivateListeners();
		this.activeImage = null;
		this.activeCanvas = null;
		this.activeWindow = null;
	}


	@Override
	public void keyPressed(KeyEvent e) {
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z && this.undoStack.size() != 0 && !redoPressed) {
        	redoPressed = true;
        	try {
	        	List<PolygonRoi> redoList = undoStack.peek();
	        	int n = this.roiManager.getCount() - 1;
	        	for (PolygonRoi pol : redoList) RoiManagerPrivateViolator.deleteRoiAtPosition(this.roiManager, n --);
	        	undoStack.pop();
	        	redoStack.push(redoList);
	        	this.redoAnnotatedMask.push(this.annotatedMask.peek());
	        	this.annotatedMask.pop();
        	} catch (Exception ex) {
        	}
        } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y && this.redoStack.size() != 0 && !undoPressed) {
        	undoPressed = true;
        	List<PolygonRoi> redoList = redoStack.peek();
        	for (PolygonRoi pol : redoList) this.addToRoiManager(pol);
        	redoStack.pop();
        	undoStack.push(redoList);
        	this.annotatedMask.push(this.redoAnnotatedMask.peek());
        	this.redoAnnotatedMask.pop();
        }
        e.consume();
	}

	@Override
	/**
	 * Monitor when the control key is being released for the point prompts.
	 * Whenever it is released and the point prompt is selected, the points that have already been drawn 
	 * are sent to SAMJ
	 */
	public void keyReleased(KeyEvent e) {
		if ((e.getKeyCode() == KeyEvent.VK_CONTROL && !PlatformDetection.isMacOS()) 
				|| (e.getKeyCode() == KeyEvent.VK_META && PlatformDetection.isMacOS())) {
			submitAndClearPoints();
		}
	    if (e.getKeyCode() == KeyEvent.VK_Z) {
	        redoPressed = false;
	    }
	    if (e.getKeyCode() == KeyEvent.VK_Y) {
	        undoPressed = false;
	    }
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (activeImage.getRoi() == null)
			return;
		if (Toolbar.getToolName().equals("rectangle")) {
			annotateRect();
		} else if (Toolbar.getToolName().equals("point") || Toolbar.getToolName().equals("multipoint")) {
			annotatePoints(e);
		} else if (Toolbar.getToolName().equals("freeline")) {
			annotateBrush(e);
		} else {
			return;
		}
		if (!isCollectingPoints) activeImage.deleteRoi();
	}
	
	private void annotateRect() {
		final Roi roi = activeImage.getRoi();
		final Rectangle rectBounds = roi.getBounds();
		final Interval rectInterval = new FinalInterval(
				new long[] { rectBounds.x, rectBounds.y },
				new long[] { rectBounds.x+rectBounds.width-1, rectBounds.y+rectBounds.height-1 } );
		submitRectPrompt(rectInterval);
	}
	
	private void submitRectPrompt(Interval rectInterval) {
		try {
			addToRoiManager(this.selectedModel.fetch2dSegmentation(rectInterval), "rect");
		} catch (Exception ex) {
			ex.printStackTrace();;
		}
	}
	
	private void annotatePoints(MouseEvent e) {
		final Roi roi = activeImage.getRoi();
		// TODO think what to do with negative points
		if (e.isControlDown() && e.isAltDown() && false) {
			roi.setFillColor(Color.red);
			//add point to the list only
			isCollectingPoints = true;
			Iterator<java.awt.Point> iterator = roi.iterator();
			java.awt.Point p = iterator.next();
			while (iterator.hasNext()) p = iterator.next();
			collecteNegPoints.add( new Point(p.x,p.y) ); //NB: add ImgLib2 Point
			//TODO log.info("Image window: collecting points..., already we have: "+collectedPoints.size());
		} else if ((e.isControlDown() && !PlatformDetection.isMacOS()) || (e.isMetaDown() && PlatformDetection.isMacOS())) {
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
	}

	/**
	 * Send the point prompts to SAM and clear the lists collecting them
	 */
	private void submitAndClearPoints() {
		if (this.selectedModel == null) return;
		if (collectedPoints.size() == 0) return;

		//TODO log.info("Image window: Processing now points, this count: "+collectedPoints.size());
		isCollectingPoints = false;
		activeImage.deleteRoi();
		Rectangle zoomedRectangle = this.activeCanvas.getSrcRect();
		try {
			if (activeImage.getWidth() * activeImage.getHeight() > Math.pow(AbstractSamJ.MAX_ENCODED_AREA_RS, 2)
					|| activeImage.getWidth() > AbstractSamJ.MAX_ENCODED_SIDE || activeImage.getHeight() > AbstractSamJ.MAX_ENCODED_SIDE)
				addToRoiManager(selectedModel.fetch2dSegmentation(collectedPoints, collecteNegPoints, zoomedRectangle),
						(collectedPoints.size() > 1 ? "points" : "point"));
			else
				addToRoiManager(selectedModel.fetch2dSegmentation(collectedPoints, collecteNegPoints),
						(collectedPoints.size() > 1 ? "points" : "point"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		collectedPoints = new ArrayList<Localizable>();
		collecteNegPoints = new ArrayList<Localizable>();
		temporalROIs = new ArrayList<Roi>();
		temporalNegROIs = new ArrayList<Roi>();
	}
	
	private void annotateBrush(MouseEvent e) {
		final Roi roi = activeImage.getRoi();
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
			Rectangle rect = roi.getBounds();
			if (rect.height == 1) {
				for (int i = 0; i < rect.width; i ++) {
					collectedPoints.add(new Point(rect.x + i, rect.y)); 
				}
			} else if (rect.width == 1) {
				for (int i = 0; i < rect.height; i ++) {
					collectedPoints.add(new Point(rect.x, rect.y + i)); 
				}
			} else {
				Iterator<java.awt.Point> it = roi.iterator();
				while (it.hasNext()) {
					java.awt.Point p = it.next();
					collectedPoints.add(new Point(p.x,p.y)); 
				}
			}
			// TODO move this logic to SAMJ into the masks option
			if (collectedPoints.size() > 1 && collectedPoints.size() < 6)
				collectedPoints = Arrays.asList(new Localizable[] {collectedPoints.get(1)});
			else if (collectedPoints.size() > 1 && collectedPoints.size() < 50) {
				List<Localizable> newCollectedPoints = new ArrayList<Localizable>();
				while (newCollectedPoints.size() == 0) {
					for (Localizable pp : collectedPoints) {
						if (Math.random() < 0.2) newCollectedPoints.add(pp);
					}
				}
				collectedPoints = newCollectedPoints;
			} else if (collectedPoints.size() > 50) {
				List<Localizable> newCollectedPoints = new ArrayList<Localizable>();
				while (newCollectedPoints.size() < 10) {
					for (Localizable pp : collectedPoints) {
						if (Math.random() < Math.min(0.1, 50.0 / collectedPoints.size())) newCollectedPoints.add(pp);
					}
				}
				collectedPoints = newCollectedPoints;
			}
			submitAndClearPoints();
		}
	}

	private RoiManager startRoiManager() {
		RoiManager roiManager = RoiManager.getInstance();
		if (roiManager == null) {
			roiManager = new RoiManager();
		}
		// TODO what to do? roiManager.reset();
		roiManager.setVisible(true);
		roiManager.setTitle("SAM Roi Manager");
		Prefs.useNamesAsLabels = true;
		Roi imRoi = activeImage.getRoi();
		deleteOtherImageRois();
		roiManager.setEditMode(activeImage, true);
		activeImage.setRoi(imRoi);
		return roiManager;
	}
	
	private void deleteOtherImageRois() {
    	try {
        	int n = RoiManager.getInstance().getCount() - 1;
        	int originalSize = undoStack.size();
    		for (int i = 0; i < originalSize; i ++) {
	        	List<Mask> maskList = annotatedMask.peek();
	        	for (int j = maskList.size() - 1; j > -1; j --) {
	        		Polygon pol = maskList.get(j).getContour();
	        		for (int k = n; k > -1; k --) {
	    	        	Roi roi = this.roiManager.getRoi(k);
	    	        	Polygon roiPol = roi.getPolygon();
	    	        	if (pol.npoints != roiPol.npoints) continue;
	    	            boolean equal = IntStream.range(0, pol.npoints)
	    	                            .allMatch(ii -> pol.xpoints[ii] == roiPol.xpoints[ii] &&
	    	                            		pol.ypoints[ii] == roiPol.ypoints[ii]);
	    	        	if (equal) {
	    	        		RoiManagerPrivateViolator.deleteRoiAtPosition(this.roiManager, k);
	    	        		n --;
	    	        		break;
	    	        	}
	        			
	        		}
	        	}
        		undoStack.pop();
	        	annotatedMask.pop();
    		}
        	this.redoAnnotatedMask.clear();
        	this.redoStack.clear();
    	} catch (Exception ex) {
    	}
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
	 * Add a single polygon to the ROI manager
	 * @param pRoi
	 */
	public void addToRoiManager(final PolygonRoi pRoi ) {
		if (isAddingToRoiManager) roiManager.addRoi(pRoi);
	}

	/**
	 * Add the new roi to the ROI manager
	 * @param polys
	 * 	list of polygons that will be converted into polygon ROIs and sent to the ROI manager
	 * @param promptShape
	 * 	String giving information about which prompt was used to generate the ROI
	 */
	void addToRoiManager(final List<Mask> polys, final String promptShape) {
		if (this.roiManager.getCount() == 0 && undoStack.size() != 0)
			annotatedMask.clear();
			
		this.redoStack.clear();
		this.redoAnnotatedMask.clear();
		promptsCreatedCnt++;
		int resNo = 1;
		List<PolygonRoi> undoRois = new ArrayList<PolygonRoi>();
		for (Mask m : polys) {
			final PolygonRoi pRoi = new PolygonRoi(m.getContour(), PolygonRoi.POLYGON);
			pRoi.setName(promptsCreatedCnt + "." + (resNo ++) + "_"+promptShape + "_" + this.selectedModel.getName());
			this.addToRoiManager(pRoi);
			undoRois.add(pRoi);
		}
		this.undoStack.push(undoRois);
		this.annotatedMask.push(polys);
	}
	
	@Override
	public void eventOccurred(int eventID) {
		if (eventID != IJEventListener.TOOL_CHANGED || callback == null)
			return;
		boolean isvalid = IJ.getToolName().equals("rectangle") 
				|| IJ.getToolName().equals("point") 
				|| IJ.getToolName().equals("multipoint");
		this.callback.validPromptChosen(isvalid);
	}
	
	@Override
	/**
	 * {@inheritDoc}
	 * 
	 * For more info about how the macros work, please go to 
	 * https://github.com/segment-anything-models-java/SAMJ-IJ/blob/main/README.md#macros
	 */
	public void notifyBatchSamize(String modelName, String maskPrompt) {
		if (!Recorder.record)
			return;
		
		String formatedMacro = "run(\"SAMJ Annotator\", \"model=[%s]%s export=false\");" + System.lineSeparator();
		String formatedMaskPrompt = " maskPrompt=[%s]";
		String promptArg = maskPrompt == null ? "" : String.format(formatedMaskPrompt, maskPrompt);
		Recorder.recordString(String.format(formatedMacro, modelName, promptArg));
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
	public void keyTyped(KeyEvent e) {}

}
