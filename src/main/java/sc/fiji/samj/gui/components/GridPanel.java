package gui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GridPanel extends JPanel {

	private static final long	serialVersionUID	= 1L;
	private GridBagLayout		layout				= new GridBagLayout();
	private GridBagConstraints	constraint			= new GridBagConstraints();
	private int					defaultSpace		= 3;

	/**
	 * Constructor.
	 */
	public GridPanel() {
		super();
		setLayout(layout);
		setBorder(BorderFactory.createEtchedBorder());
	}

	/**
	 * Constructor.
	 */
	public GridPanel(int defaultSpace) {
		super();
		setLayout(layout);
		this.defaultSpace = defaultSpace;
		setBorder(BorderFactory.createEtchedBorder());
	}

	/**
	 * Constructor.
	 */
	public GridPanel(boolean border) {
		super();
		setLayout(layout);
		if (border) {
			setBorder(BorderFactory.createEtchedBorder());
		}
	}

	/**
	 * Constructor.
	 */
	public GridPanel(String title) {
		super();
		setLayout(layout);
		setBorder(BorderFactory.createTitledBorder(title));
	}

	/**
	 * Constructor.
	 */
	public GridPanel(boolean border, int defaultSpace) {
		super();
		setLayout(layout);
		this.defaultSpace = defaultSpace;
		if (border) {
			setBorder(BorderFactory.createEtchedBorder());
		}
	}

	/**
	 * Constructor.
	 */
	public GridPanel(String title, int defaultSpace) {
		super();
		setLayout(layout);
		this.defaultSpace = defaultSpace;
		setBorder(BorderFactory.createTitledBorder(title));
	}

	/**
	 * Specify the defaultSpace.
	 */
	public void setSpace(int defaultSpace) {
		this.defaultSpace = defaultSpace;
	}

	/**
	 * Place a component in the northwest of the cell.
	 */
	public void place(int row, int col, String label) {
		place(row, col, 1, 1, defaultSpace, new JLabel(label));
	}

	/**
	 * Place a component in the northwest of the cell.
	 */
	public void place(int row, int col, int space, String label) {
		place(row, col, 1, 1, space, new JLabel(label));
	}

	/**
	 * Place a component in the northwest of the cell.
	 */
	public void place(int row, int col, int width, int height, String label) {
		place(row, col, width, height, defaultSpace, new JLabel(label));
	}

	/**
	 * Place a component in the northwest of the cell.
	 */
	public void place(int row, int col, JComponent comp) {
		place(row, col, 1, 1, defaultSpace, comp);
	}

	/**
	 * Place a component in the northwest of the cell.
	 */
	public void place(int row, int col, int space, JComponent comp) {
		place(row, col, 1, 1, space, comp);
	}

	/**
	 * Place a component in the northwest of the cell.
	 */
	public void place(int row, int col, int width, int height, JComponent comp) {
		place(row, col, width, height, defaultSpace, comp);
	}

	/**
	 * Place a component in the northwest of the cell.
	 */
	public void place(int row, int col, int width, int height, int space, JComponent comp) {
		if (comp == null)
			return;
		constraint.gridx = col;
		constraint.gridy = row;
		constraint.gridwidth = width;
		constraint.gridheight = height;
		constraint.anchor = GridBagConstraints.NORTHWEST;
		constraint.insets = new Insets(space, space, space, space);
		constraint.fill = GridBagConstraints.HORIZONTAL;
		layout.setConstraints(comp, constraint);
		add(comp);
	}

	/**
	 * Place a component in the northwest of the cell.
	 */
	public void place(int row, int col, int width, int height, int spaceHorizontal, int spaceVertical, JComponent comp) {
		if (comp == null)
			return;
		constraint.gridx = col;
		constraint.gridy = row;
		constraint.gridwidth = width;
		constraint.gridheight = height;
		constraint.anchor = GridBagConstraints.NORTHWEST;
		constraint.insets = new Insets(spaceVertical, spaceHorizontal, spaceHorizontal, spaceVertical);
		constraint.fill = GridBagConstraints.HORIZONTAL;
		layout.setConstraints(comp, constraint);
		add(comp);
	}
}
