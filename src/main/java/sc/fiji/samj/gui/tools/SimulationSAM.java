package sc.fiji.samj.gui.tools;

import java.awt.Polygon;
import java.awt.Rectangle;

import ij.ImagePlus;
import ij.gui.PolygonRoi;

/**
 * This class simulates a segmentation in a bounding box.
 * The input a bounding box (Rectangle)
 * The output is a PolygonRoi and a score
 * 
 * @author dsage
 *
 */
public class SimulationSAM {

	private ImagePlus imp;
	private Rectangle rect;
	private int[][] roi;
	private double score = 0;
	
	public SimulationSAM(ImagePlus imp, Rectangle rect) {
		this.imp = imp;
		this.rect = rect;
		roi = new int[2][4];
		// Initialization of the results by the initial Rectangle
		roi[0][0] = rect.x;
		roi[1][0] = rect.y;
		roi[0][1] = rect.x + rect.width;
		roi[1][1] = rect.y;
		roi[0][2] = rect.x + rect.width;
		roi[1][2] = rect.y + rect.height;
		roi[0][3] = rect.x;
		roi[1][3] = rect.y + rect.height;
	}

	public int[][] getResultAsArray() {
		return roi;
	}
	
	public double getResultScore() {
		return score;
	}
	
	public PolygonRoi getResultAsRoi() {
		Polygon p = new Polygon();
		for(int i = 0; i<roi[0].length; i++)
			p.addPoint(roi[0][i], roi[1][i]);
		return new PolygonRoi(p, PolygonRoi.POLYGON);
	}	
	
	public void run() {
		int x1 = rect.x;
		int y1 = rect.y;
		int h = rect.height;
		int w = rect.width;
		roi = new int[2][8];
		roi[0][0] = x1 + rand(w * 0.2);
		roi[1][0] = y1 + rand(w * 0.2);
		roi[0][1] = x1 + w / 2;
		roi[1][1] = y1 + rand(w * 0.1);
		roi[0][2] = x1 + w - rand(w * 0.2);
		roi[1][2] = y1 + rand(w * 0.1);
		roi[0][3] = x1 + w - rand(w * 0.2);
		roi[1][3] = y1 + h / 2;
		roi[0][4] = x1 + w - rand(w * 0.2);
		roi[1][4] = y1 + h - rand(w * 0.2);
		roi[0][5] = x1 + w / 2;
		roi[1][5] = y1 + h - rand(w * 0.3);
		roi[0][6] = x1 + rand(w * 0.2);
		roi[1][6] = y1 + h - rand(w * 0.2);
		roi[0][7] = x1 + rand(w * 0.2);
		roi[1][7] = y1 + h / 2;
		score = Math.random();
	}

	private int rand(double a) {
		return (int) (Math.random() * a);
	}
}
