package ui;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import segmentation.sam.SimulationSAM;
import ui.components.HTMLPane;
import ui.components.PanelImage;

public class SAMDialog extends JDialog implements ActionListener, MouseListener {

	private int count = 0;
	private String url = "http://github.com/dasv74";

	private JButton bnEncode = new JButton("Encode");
	private JButton bnClose = new JButton("Close");
	private JButton bnHelp = new JButton("Help");
	private JButton bnClear = new JButton("Clear All");
	private HTMLPane infoPanel = new HTMLPane(400, 70);
	private HTMLPane runningPanel = new HTMLPane(400, 100);
	private ImagePlus imp;

	public SAMDialog(ImagePlus imp) {
		super(new JFrame(), "SAMJ");
		this.imp = imp;
		JPanel pn1 = new JPanel(new GridLayout(1, 4));
		pn1.add(bnHelp);
		pn1.add(bnEncode);
		pn1.add(bnClear);
		pn1.add(bnClose);
		JPanel pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(initInfo());
		pn.add(initRunning());
		pn.add(pn1);
		
		
		bnClear.addActionListener(this);
		bnClose.addActionListener(this);
		bnHelp.addActionListener(this);
		bnEncode.addActionListener(this);
		add(pn);
		pack();
		this.setModal(false);
		this.setVisible(true);
		GUI.center(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnEncode) {
			startRoiManager();
			ImageCanvas canvas = imp.getCanvas();
			canvas.addMouseListener(this);
			runningPanel.clear();
			runningPanel.append("p", "The image is well encoded.");
			runningPanel.append("p", "Provide a ROI as prompt to SAM.");
			imp.killRoi();
		}
		if (e.getSource() == bnHelp) {
			help();
		}
		if (e.getSource() == bnClose) {
			if (imp != null) {
				ImageCanvas canvas = imp.getCanvas();
				if (canvas != null) {
					canvas.removeMouseListener(this);
				}
			}
			RoiManager roiManager = startRoiManager();
			roiManager.close();

			dispose();
		}
		if (e.getSource() == bnClear) {
			RoiManager roiManager = startRoiManager();
			roiManager.close();
			runningPanel.clear();
			runningPanel.append("p", "The image is well encoded.");
			runningPanel.append("p", "Provide a ROI as prompt to SAM.");
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		Roi roi = imp.getRoi();
		if (roi != null) {
			RoiManager roiManager = startRoiManager();
			if (count == 0)
				runningPanel.clear();
			Rectangle rect = roi.getBounds();
			runningPanel.append("<small>(" + rect.x + ", " + rect.y + ", " + rect.width + ", " + rect.height + ")");
			double chrono = System.nanoTime();
			SimulationSAM sam = new SimulationSAM(imp, rect);
			sam.run();
			PolygonRoi proi = sam.getResultAsRoi();
			chrono = System.nanoTime() - chrono;
			runningPanel.append(" len: " +  proi.getContainedPoints().length + " score: " + String.format("%3.3f", sam.getResultScore() ));
			runningPanel.append(" time: " + String.format("%3.3f", (chrono / 1000000)) + " ms</small><br>");			//new PolygonRoi(p, PolygonRoi.POLYGON);
			proi.setName("SAM-" + (++count));
			roiManager.add(proi, 1);
			imp.setRoi(proi);
			imp.updateAndRepaintWindow();
		}
	}
	

	@Override
	public void mouseEntered(MouseEvent e) {
		// Handle mouse enter event
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// Handle mouse exit event
	}


	public JScrollPane initInfo() {
		infoPanel.append("h2", "Segment Any Model");
		infoPanel.append("small", "link URL");
		infoPanel.append("small", "SAM run only on 2D RGB images");
		infoPanel.append("small", "The initial SAM encoding is a slow process ");
		return new JScrollPane(infoPanel);
	}

	public JScrollPane initRunning() {
		runningPanel.append("p", "The image is not yet encoded.");
		runningPanel.append("p", "Input image: " + imp.getWidth() + "x" + imp.getHeight());
		runningPanel.append("small", "The initial SAM encoding is a slow process ");
		return new JScrollPane(runningPanel);
	}


	private RoiManager startRoiManager() {
		RoiManager roiManager = RoiManager.getInstance();
		if (roiManager == null) {
			roiManager = new RoiManager();
		}
		roiManager.setVisible(true);
		roiManager.setTitle("SAM Roi Manager");
		Prefs.useNamesAsLabels = true;
		roiManager.setEditMode(imp, true);
		return roiManager;
	}

	public boolean help() {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(new URL(url).toURI());
				return true;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}


}
