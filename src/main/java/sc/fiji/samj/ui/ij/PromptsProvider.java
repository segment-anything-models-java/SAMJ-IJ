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
import net.imglib2.Point;
import org.scijava.log.Logger;
import sc.fiji.samj.communication.PromptsToNetAdapter;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PromptsProvider implements MouseListener, KeyListener, WindowListener {

	//remember provided arguments
	private final ImagePlus activeImage;
	private final PromptsToNetAdapter promptsToNet;
	private final RoiManager roiManager;

	//shortcuts...
	private final ImageCanvas activeCanvas;
	private final ImageWindow activeWindow;

	private final Logger log;

	public PromptsProvider(final ImagePlus imagePlus,
	                       final PromptsToNetAdapter promptsToNetAdapter,
	                       final RoiManager roiManager,
	                       final Logger log) {
		this.promptsToNet = promptsToNetAdapter;
		this.roiManager = roiManager;
		this.activeImage = imagePlus;
		this.log = log;

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
	public void mouseReleased(MouseEvent e) {
		final Roi roi = activeImage.getRoi();
		if (roi == null) {
			log.info("Image window: There's no ROI...");
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
				roiManager.addRoi( convertToPolygonRoi( promptsToNet.fetch2dSegmentation(rectInterval) ) );
				break;

			case Roi.LINE:
				Iterator<java.awt.Point> it = roi.iterator();
				java.awt.Point pit = it.next(); //NB: since Roi != null, the point for sure exists...
				Point p1 = new Point(pit.x, pit.y);
				while (it.hasNext()) pit = it.next(); //find the last point on the line
				Point p2 = new Point(pit.x, pit.y);
				log.info("Image window: line... from "+p1+" to "+p2);
				//
				roiManager.addRoi( convertToPolygonRoi( promptsToNet.fetch2dSegmentation(p1,p2) ) );
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

	PolygonRoi convertToPolygonRoi(final Polygon p) {
		return new PolygonRoi(p, PolygonRoi.POLYGON);
	}

	private boolean isCollectingPoints = false;
	private final List<Localizable> collectedPoints = new ArrayList<>(100);

	private void submitAndClearPoints() {
		if (collectedPoints.size() == 0) return;

		log.info("Image window: Processing now points, this count: "+collectedPoints.size());
		isCollectingPoints = false;
		activeImage.deleteRoi();
		promptsToNet.fetch2dSegmentation(collectedPoints);
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
		log.info("Image window: Window closed, notify that nothing will ever arrive...");
		deRegisterListeners();
		promptsToNet.notifyUiHasBeenClosed();
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
