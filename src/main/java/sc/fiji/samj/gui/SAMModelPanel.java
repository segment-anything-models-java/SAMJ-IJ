package sc.fiji.samj.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import ij.IJ;
import sc.fiji.samj.communication.model.SAMModel;
import sc.fiji.samj.communication.model.SAMModels;
import sc.fiji.samj.gui.components.GridPanel;
import sc.fiji.samj.gui.components.HTMLPane;

public class SAMModelPanel extends JPanel implements ActionListener {
	
	private HTMLPane info = new HTMLPane(400, 70);
	
	private JButton bnInstall = new JButton("Install");
	private JButton bnUninstall = new JButton("Uninstall");
	
	private ArrayList<JRadioButton> rbModels = new ArrayList<JRadioButton>();
	private SAMModels models;
	
	public SAMModelPanel(SAMModels models) {
		super();
		this.models = models;
		JToolBar pnToolbarModel = new JToolBar();
		pnToolbarModel.setFloatable(false);
		pnToolbarModel.setLayout(new GridLayout(1, 2));
		pnToolbarModel.add(bnInstall);
		pnToolbarModel.add(bnUninstall);
		
		ButtonGroup group = new ButtonGroup();
		for(SAMModel model : models) {
			JRadioButton rb = new JRadioButton(model.getName(), model.isInstalled());
			rbModels.add(rb);
			rb.addActionListener(this);
			group.add(rb);
		}
	
		JPanel pnManageModel = new JPanel(new BorderLayout());
		pnManageModel.add(pnToolbarModel, BorderLayout.NORTH);
		pnManageModel.add(new JScrollPane(info), BorderLayout.CENTER);
		
		GridPanel pnModel = new GridPanel(true);
		int col = 1;
		for(JRadioButton rb : rbModels)
			pnModel.place(1, col++, 1, 1, rb);
		
		pnModel.place(2, 1, 4, 1, pnManageModel);
		
		add(pnModel);
		info.append("p", "Description of the model");
		info.append("p", "Link to source");
		bnInstall.addActionListener(this);
		bnUninstall.addActionListener(this);
		
		updateInterface();
	}
	
	private void updateInterface() {
		for(int i=0; i<rbModels.size(); i++) {
			bnInstall.setEnabled(rbModels.get(i).isSelected());
			bnUninstall.setEnabled(!rbModels.get(i).isSelected());
			info.clear();
			info.append("p", models.get(i).getDescription());
		}
	}
	
	public SAMModel getSelectedModel() {
		for(int i=0; i<rbModels.size(); i++) {
			if (rbModels.get(i).isSelected())
				return models.get(i);
		}
		return null;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == bnInstall) {
			IJ.log("TODO: call the installation of ");
		}
		if (e.getSource() == bnUninstall) {
			IJ.log("TODO: call the uninstallation of ");
		}
		updateInterface();
	}
}


