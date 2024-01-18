package sc.fiji.samj.gui.icons;

import java.awt.Dimension;
import java.awt.Image;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

public class ButtonIcon extends JButton {
	
	public ButtonIcon(String text, String filename) {
		super();
		try {
			URL url = ButtonIcon.class.getResource(filename);
			if (url != null) {
				ImageIcon img = new ImageIcon(url, "") ;  
				Image image = img.getImage();
				Image scaled = image.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
				setIcon(new ImageIcon(scaled));
				setBorder(BorderFactory.createEtchedBorder());
				setOpaque(false);
				setContentAreaFilled(false);
				setPreferredSize(new Dimension(58, 58));
				setVerticalTextPosition(SwingConstants.BOTTOM);
				setHorizontalTextPosition(SwingConstants.CENTER);
				setText(text);
				//setBorderPainted(false);
			}
		} 
		catch (Exception ex) {
			setText(text);
		}
	}
	


}
