package ai.nets.samj.ij.ui;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.proteovir.utils.Mask;

import ij.ImageListener;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.plugin.OverlayLabels;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class RoiManagerIJ implements RoiManagerConsumer, RoiListener, ImageListener {
	
	private ImagePlus imp;
	
	private int currentFrame;
	
	private int currentSlice;
	
	private boolean isDragging = false;
	
	private boolean isModifying = false;
		
	private Roi modRoi;
	
	private List<Mask> maskList;
	
	private List<Roi> roiList;
	
	private BiConsumer<Integer,Polygon> modifyRoiCallback;
	
	private Consumer<Integer> selectedCallback;
	
	public RoiManagerIJ() {
		Roi.addRoiListener(this);
		ImagePlus.addImageListener(this);
	}
	
	public void setImage(Object imp) {
		if (imp.equals(this.imp))
			return;
		this.imp = (ImagePlus) imp;
		this.currentFrame = this.imp.getFrame() - 1;
		this.currentSlice = this.imp.getCurrentSlice() - 1;
		this.imp.getCanvas().addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				isDragging = true;
			}

			@Override
			public void mouseMoved(MouseEvent e) {}
		});
		this.imp.getCanvas().addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				if (isDragging && isModifying)
					modifyRoi();
				isDragging = false;
				isModifying = false;
			}

			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			
		});
	}

	public void setRois(List<Mask> rois) {
		Overlay overlay = newOverlay();
		this.maskList = rois;
		roiList = new ArrayList<Roi>();
		int slice = 0;
		int frame = 0;
		if (imp != null) {
			slice = imp.getCurrentSlice() - 1;
			frame = imp.getFrame() - 1;
		}
		for (Mask mm : rois) {
			PolygonRoi roi = new PolygonRoi(mm.getContour(), PolygonRoi.POLYGON);
			roi.setName(mm.getName());
			roiList.add(roi);
			if (mm.getSlice() != slice || mm.getFrame() != frame)
				continue;
			overlay.add(roi);
		}
		//imp.deleteRoi();
		setOverlay(overlay);
	}

	public void changeOverlay(List<Mask> rois) {
		Overlay overlay = newOverlay();
		int slice = 0;
		int frame = 0;
		if (imp != null) {
			slice = imp.getCurrentSlice() - 1;
			frame = imp.getFrame() - 1;
		}
		for (Mask mm : rois) {
			PolygonRoi roi = new PolygonRoi(mm.getContour(), PolygonRoi.POLYGON);
			roi.setName(mm.getName());
			if (mm.getSlice() != slice || mm.getFrame() != frame)
				continue;
			overlay.add(roi);
		}
		//imp.deleteRoi();
		setOverlay(overlay);
	}

	public void setRois(List<Mask> rois, int ind) {
		ImagePlus imp = WindowManager.getCurrentImage();
		this.maskList = rois;
		Overlay overlay = newOverlay();
		int i = 0;
		roiList = new ArrayList<Roi>();
		int slice = 0;
		int frame = 0;
		if (imp != null) {
			slice = imp.getCurrentSlice() - 1;
			frame = imp.getFrame() - 1;
		}
		for (Mask mm : rois) {
			PolygonRoi roi = new PolygonRoi(mm.getContour(), PolygonRoi.POLYGON);
			roi.setName(mm.getName());
			roiList.add(roi);
			if (i == ind && mm.getSlice() == slice && mm.getFrame() == frame) {
				imp.setRoi(roi);
			} else if (mm.getSlice() == slice && mm.getFrame() == frame) {
				overlay.add(roi);
			}
			i ++;
		}
		setOverlay(overlay);
	}

	@Override
	public void setSelected(Mask mm) {
		if (mm == null && imp == null)
			return;
		if (mm == null) {
			imp.deleteRoi();
			return;
		}
		Roi setRoi = null;
		for (int i = 0; i < this.roiList.size(); i ++) {
			Roi roi = this.roiList.get(i);
			if (roi.getName().equals(mm.getName())) {
				setRoi = roi;
				this.currentFrame = this.maskList.get(i).getFrame();
				this.currentSlice = this.maskList.get(i).getSlice();
				imp.setPosition(0, currentSlice + 1, currentFrame + 1);
				this.changeOverlay(maskList);
				break;
			}
		}
		if (setRoi == null) {
			setRoi = new PolygonRoi(mm.getContour(), PolygonRoi.POLYGON);
			setRoi.setName(mm.getName());
		}
		imp.setRoi(setRoi, true);
	}

	@Override
	public void exportMask() {
		int width = imp.getWidth();
		int height = imp.getHeight();
		RandomAccessibleInterval<UnsignedShortType> raiMask = Mask.getMask(width, height, maskList);
		ImagePlus impMask = ImageJFunctions.show(raiMask);
		impMask.setTitle(imp.getTitle() + "-labeling");
		impMask.getProcessor().setMinAndMax(0, maskList.size());
	}

	private Overlay newOverlay() {
		Overlay overlay = OverlayLabels.createOverlay();
		overlay.drawLabels(true);
		if (overlay.getLabelFont()==null && overlay.getLabelColor()==null) {
			overlay.setLabelColor(Color.white);
			overlay.drawBackgrounds(true);
		}
		overlay.drawNames(Prefs.useNamesAsLabels);
		return overlay;
	}

	private void setOverlay(Overlay overlay) {
		if (imp==null)
			return;
		ImageCanvas ic = imp.getCanvas();
		if (ic==null) {
			if (imp.getOverlay()==null)
				imp.setOverlay(overlay);
			return;
		} else {
			imp.setOverlay(overlay);
		}
		ic.setShowAllList(overlay);
		imp.draw();
	}
	
	public void deleteAllRois() {
		
	}
	
	private void modifyRoi() {
		for (int i = 0; i < this.roiList.size(); i ++) {
			Roi roi2 = this.roiList.get(i);
			if (modRoi.getName() != null && modRoi.getName().equals(roi2.getName())) {
				modifyRoiCallback.accept(i, modRoi.getPolygon());
				break;
			}
		}
		modRoi = null;
	}
	
	private void findSelectedRoi() {
		Roi roi = imp.getRoi();
		if (roi == null || roi.getName() == null || !(roi instanceof PolygonRoi))
			return;
		PolygonRoi polRoi = (PolygonRoi) roi;
		int ind = -1;
		for (Roi roi2 : roiList) {
			ind ++;
			if (!(roi2 instanceof PolygonRoi))
				continue;
			PolygonRoi m = (PolygonRoi) roi2;
			if (!Arrays.equals(polRoi.getXCoordinates(), m.getXCoordinates())
					|| !Arrays.equals(polRoi.getYCoordinates(), m.getYCoordinates()))
				continue;
			this.selectedCallback.accept(ind);
			return;
		}
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
		if (this.imp != null && imp.getID() == this.imp.getID() 
				&& (imp.getCurrentSlice() - 1 != this.currentSlice || imp.getFrame() - 1 != this.currentFrame)) {
			this.setRois(maskList);
			this.currentSlice = imp.getCurrentSlice() - 1;
			this.currentFrame = imp.getFrame() - 1;
			this.imp.deleteRoi();
		}
	}

	@Override
	public void roiModified(ImagePlus imp, int id) {
		if (imp == null || !imp.equals(this.imp))
			return;
		if (id == RoiListener.CREATED && imp.getRoi().getName() == null) {
			return;
		} else if (id == RoiListener.CREATED) {
			findSelectedRoi();
			return;
		}
		if (id != RoiListener.MODIFIED && id != RoiListener.MOVED)
			return;
		if (!isDragging)
			return;
		if (modRoi != null)
			return;
		modRoi = imp.getRoi();
		isModifying = true;
	}

	@Override
	public void setModifyRoiCallback(BiConsumer<Integer,Polygon> modifyRoiCallback) {
		this.modifyRoiCallback = modifyRoiCallback;
		
	}

	@Override
	public void setSelectedCallback(Consumer<Integer> selectedCallback) {
		this.selectedCallback = selectedCallback;
		
	}

	@Override
	public void imageOpened(ImagePlus imp) {
	}

	@Override
	public void imageClosed(ImagePlus imp) {
	}
}
