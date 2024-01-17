package sc.fiji.samj.ui.ij;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import sc.fiji.samj.communication.PromptsToNetAdapter;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PromptsProvider implements MouseListener, KeyListener, WindowListener {

	//remember provided arguments
	private final ImagePlus activeImage;
	private final PromptsToNetAdapter promptsToNet;
	private final RoiManager roiManager;

	//shortcuts...
	private final ImageCanvas activeCanvas;
	private final ImageWindow activeWindow;

	public PromptsProvider(final ImagePlus imagePlus,
	                       final PromptsToNetAdapter promptsToNetAdapter,
	                       final RoiManager roiManager) {
		this.promptsToNet = promptsToNetAdapter;
		this.roiManager = roiManager;
		this.activeImage = imagePlus;

		activeCanvas = activeImage.getCanvas();
		activeWindow = activeImage.getWindow();

		//make sure we start with no ROIs at all
		activeImage.killRoi();
		registerListeners();
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
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {
		final Roi roi = activeImage.getRoi();
		if (roi == null) {
			System.out.println("Image window: There's no ROI...");
			return;
		}

		switch (roi.getType()) {
			case Roi.RECTANGLE:
				System.out.println("Image window: rectangle...");
				//
				final Rectangle rectBounds = roi.getBounds();
				final Interval rectInterval = new FinalInterval(
						new long[] { rectBounds.x, rectBounds.y },
						new long[] { rectBounds.x+rectBounds.width-1, rectBounds.y+rectBounds.height-1 } );
				roiManager.addRoi( convertToPolygonRoi( promptsToNet.fetch2dSegmentation(rectInterval) ) );
				break;

			case Roi.LINE:
				Iterator<Point> it = roi.iterator();
				Point pit = it.next(); //NB: since Roi != null, the point for sure exists...
				net.imglib2.Point p1 = new net.imglib2.Point(pit.x, pit.y);
				while (it.hasNext()) pit = it.next(); //find the last point on the line
				net.imglib2.Point p2 = new net.imglib2.Point(pit.x, pit.y);
				System.out.println("Image window: line... from "+p1+" to "+p2);
				//
				roiManager.addRoi( convertToPolygonRoi( promptsToNet.fetch2dSegmentation(p1,p2) ) );
				break;

			case Roi.POINT:
				if (e.isShiftDown()) {
					//add point to the list
					isCollectingPoints = true;
					collectedPoints.add(new Double(10)); //TODO
					System.out.println("Image window: collecting points..., already we have: "+collectedPoints.size());
				} else {
					isCollectingPoints = false;
					collectedPoints.add(new Double(10)); //TODO
					//TODO submit them
					collectedPoints.clear();
				}
				break;
			default:
				System.out.println("Image window: unsupported ROI type");
		}

		if (!isCollectingPoints) activeImage.deleteRoi();
	}

	PolygonRoi convertToPolygonRoi(final Polygon p) {
		return new PolygonRoi(p, PolygonRoi.POLYGON);
	}

	private boolean isCollectingPoints = false;
	private final List<Object> collectedPoints = new ArrayList<>(100);

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void windowClosed(WindowEvent e) {
		System.out.println("Image window: Window closed, notify that nothing will ever arrive...");
		deRegisterListeners();
	}

	// ===== unused window events =====
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

	// ===== KEYS =====
	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
}
