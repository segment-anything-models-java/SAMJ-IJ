package ui.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;


public class PanelImage extends JPanel {

	private Image	image;
	private int		w	= -1;
	private int		h	= -1;

	public PanelImage() {
		super();
	}

	public PanelImage(int w, int h) {
		super();
		image = null;
		this.w = w;
		this.h = h;
	}

	public PanelImage(String filename, int w, int h) {
		super();
		System.out.println("Working dir:  " + System.getProperty("user.dir"));
		File file = new File("celegans.jpg");
		System.out.println(file.toString());
		try {
			image = ImageIO.read(file);
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void setImage(BufferedImage image) {
		this.image = image;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image != null) {
			if (w < 0)
				g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
			else {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.drawImage(image, (getWidth()-w)/2, 0, w, h, null);
			}
		}
		else {
			g.setColor(Color.DARK_GRAY);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
	}

}
