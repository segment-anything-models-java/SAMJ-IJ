package ai.nets.samj.ij.ui;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.ui.ConsumerInterface;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class Consumer extends ConsumerInterface {
	/**
	 * The image being processed
	 */
	private final ImagePlus activeImage;
	/**
	 * Canvas of the image selected. Used to record the prompts drawn by the user
	 */
	private final ImageCanvas activeCanvas;
	/**
	 * Window of the selected image. Used to record the prompts drawn by the user
	 */
	private final ImageWindow activeWindow;
	/**
	 * Instance of the ROI manager to save the ROIs created
	 */
	private final RoiManager roiManager;
	/**
	 * Whether to add the ROIs created to the ROI manager or not
	 */
	private boolean isAddingToRoiManager = true;
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

	@Override
	public List<ComboBoxItem> getListOfOpenImages() {
		// TODO Auto-generated method stub
		return null;
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
		SwingUtilities.invokeLater(() -> {
			IJ.addEventListener(this);
			activeCanvas.removeKeyListener(IJ.getInstance());
			activeWindow.removeKeyListener(IJ.getInstance());
			activeCanvas.addMouseListener(this);
			activeCanvas.addKeyListener(this);
			activeWindow.addWindowListener(this);
			activeWindow.addKeyListener(this);
		});
		registered = true;
	}

	@Override
	public void deactivateListeners() {
		if (!registered) return;
		SwingUtilities.invokeLater(() -> {
			IJ.removeEventListener(this);
			activeCanvas.removeMouseListener(this);
			activeCanvas.removeKeyListener(this);
			activeWindow.removeWindowListener(this);
			activeWindow.removeKeyListener(this);
			
			activeWindow.addKeyListener(IJ.getInstance());
			activeCanvas.addKeyListener(IJ.getInstance());
		});
		registered = false;
	}

	@Override
	public void setFocusedImage(Object image) {
		activeImage = (ImagePlus) image;
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

}
