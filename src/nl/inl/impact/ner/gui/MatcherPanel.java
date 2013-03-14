package nl.inl.impact.ner.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class MatcherPanel {

	private ImageIcon matcherImageIcon = new ImageIcon("./pics/impactLogo.gif");
	private GridBagConstraints matcherPanelConstraints = null;
	public Color bgColorPanel = new Color(176, 176, 173); // this matches the (grey) color of the logo
	private Font font = new Font("Lucida Sans", Font.ITALIC, 14);
	private String[] tableHeaders = { "Type", "Freq.", "Named Entity", "Variant" };
	
	//constructor. Declare the gridbagconstraints for all objects in the panel
	public MatcherPanel(){
		matcherPanelConstraints = new GridBagConstraints();
	}
	
	
	public void buildConstraints(GridBagConstraints gbc, int gx, int gy,
			int gw, int gh, int wx, int wy) {
		gbc.gridx = gx;
		gbc.gridy = gy;
		gbc.gridwidth = gw;
		gbc.gridheight = gh;
		gbc.weightx = wx;
		gbc.weighty = wy;
	}
	
	public JLabel createLogoLabel(JLabel label, JPanel panel){
		label = new JLabel(matcherImageIcon);
		buildConstraints(matcherPanelConstraints, 5, 0, 15, 1, 100, 50);
		matcherPanelConstraints.fill = GridBagConstraints.VERTICAL;
		matcherPanelConstraints.anchor = GridBagConstraints.NORTHEAST;
		matcherPanelConstraints.insets = new Insets(0, 0, 0, 10);
		panel.add(label, matcherPanelConstraints);
		return label;
	}
	
	public JLabel createIntroTextLabel(JLabel label, JPanel panel){
		label = new JLabel(
				"<html>To extract named entities from text,"
						+ " add text below and load an appropriate classifier. "
						+ " Add text by using copy/paste or by selecting File > Open File from the menu.</html>");

		buildConstraints(matcherPanelConstraints, 20, 0, 1, 1, 100, 50);
		matcherPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		matcherPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		matcherPanelConstraints.insets = new Insets(120, 10, 0, 0);
		panel.add(label, matcherPanelConstraints);

		return label;
	}
	
	public JPanel createMatcherButtonPanel1(JPanel btnPanel, JButton btn1, JButton btn2, JPanel panel){
		btn1.updateUI();
		btn2.updateUI();
		//btn1 = new JButton("Add file");
		btnPanel.add(btn1);
		//btn2 = new JButton("Remove");
		btnPanel.add(btn2);
	
		matcherPanelConstraints.fill = GridBagConstraints.NONE;
		matcherPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		buildConstraints(matcherPanelConstraints, 0, 3, 1, 1, 50, 50);
		matcherPanelConstraints.insets = new Insets(0, 20, 0, 0);

		btnPanel.setBackground(bgColorPanel);
		panel.add(btnPanel, matcherPanelConstraints);
		return btnPanel;
	}
	
	public JScrollPane createFileListTableScrollPane(JTable table, JScrollPane scrollPane, JPanel panel){
		
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		table.setFillsViewportHeight(true);
		System.err.println(table.getColumnClass(0));
		//column 0 holds the checkbox for lemma use
		TableColumn col = table.getColumnModel().getColumn(0); 
		col.setPreferredWidth(70);
		//column 1 holds the file
		col = table.getColumnModel().getColumn(1); 
		col.setPreferredWidth(200);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
			
		scrollPane = new JScrollPane(table);
	  	buildConstraints(matcherPanelConstraints, 0, 2, 5, 1, 100, 200);
		matcherPanelConstraints.fill = GridBagConstraints.BOTH;
		matcherPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		matcherPanelConstraints.insets = new Insets(0, 20, 0, 0);
	    panel.add(scrollPane, matcherPanelConstraints);
		return scrollPane;
	}
	
	
	public DefaultTableModel createNETableModel(DefaultTableModel tableModel){
		//fill the table with empty list, not very elegant..
		List<String[]> data = new ArrayList<String[]>();
		Object[][] dummyArray = new Object[data.size()][4];
		int counter = 0;
		for(String[] s: data){
			dummyArray[counter] = s;
			counter++;
		}
		tableModel = new DefaultTableModel(dummyArray, tableHeaders);
		return tableModel;
	}
	
	public JScrollPane createNETable(JTable table, JScrollPane scrollPane, DefaultTableModel matcherTableModel, JPanel panel) {
		
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		//column 0 holds the NE-type
		TableColumn col = table.getColumnModel().getColumn(0); 
		col.setPreferredWidth(10);
		//column 1 holds the frequency fo the NE
		col = table.getColumnModel().getColumn(1); 
		col.setPreferredWidth(10);
		//column 2 holds the NE
		col = table.getColumnModel().getColumn(2); 
		col.setPreferredWidth(300);
		//column 4 holds the variants
		col = table.getColumnModel().getColumn(3); 
		col.setPreferredWidth(420);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		scrollPane = new JScrollPane(table);
        buildConstraints(matcherPanelConstraints, 5, 2, 25, 1, 800, 200);
		matcherPanelConstraints.fill = GridBagConstraints.BOTH;
		matcherPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		matcherPanelConstraints.insets = new Insets(0, 0, 0, 20);
		panel.add(scrollPane, matcherPanelConstraints);
		return scrollPane;
	}

	
	public JPanel createMatcherRadioButtonPanel(JPanel btnPanel, JRadioButton btn1, JRadioButton btn2, JPanel panel){
		GridLayout buttons = new GridLayout(2, 1, 5, 5);
		btnPanel.setLayout(buttons);
		ButtonGroup group = new ButtonGroup();
		btnPanel.setBackground(bgColorPanel);
	
		btn1.setVisible(false);
		btn2.setVisible(false);
		btn1.updateUI();
		btn2.updateUI();
		group.add(btn1);
		group.add(btn2);
		btnPanel.add(btn1);
		btnPanel.add(btn2);
		
		matcherPanelConstraints.fill = GridBagConstraints.NONE;
		matcherPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		buildConstraints(matcherPanelConstraints, 25, 3, 1, 1, 50, 50);
		matcherPanelConstraints.insets = new Insets(0, 0, 0, 0);
		panel.add(btnPanel, matcherPanelConstraints);
		return btnPanel;
	}
	
	public JPanel createMatcherButtonPanel2(JPanel btnPanel, JButton btn1, JButton btn2, JPanel panel){
		btn1.updateUI();
		btn2.updateUI();
		btnPanel.add(btn1);
		btnPanel.add(btn2);
	
		matcherPanelConstraints.fill = GridBagConstraints.NONE;
		matcherPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		buildConstraints(matcherPanelConstraints, 18, 3, 1, 1, 50, 50);
		matcherPanelConstraints.insets = new Insets(0, 0, 0, 0);
		btnPanel.setBackground(bgColorPanel);
		panel.add(btnPanel, matcherPanelConstraints);
		return btnPanel;
	}
	
	public JLabel createMatcherStatusLabel1(JLabel label, JPanel panel){
		// row for showing something: void for now
		label = new JLabel("<empty>");
		label.setFont(font);
		label.updateUI();
		matcherPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		buildConstraints(matcherPanelConstraints, 0, 4, 10, 1, 100, 1);
		matcherPanelConstraints.insets = new Insets(0, 20, 0, 0);
		panel.add(label, matcherPanelConstraints);
		return label;
	}
	
	public JLabel createMatcherStatusLabel2(JLabel label, JPanel panel){
		label = new JLabel("Status: Ready.");
		label.setFont(font);
		label.updateUI();
		matcherPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		buildConstraints(matcherPanelConstraints, 0, 5, 1, 1, 10, 1);
		matcherPanelConstraints.insets = new Insets(0, 20, 20, 0);
		panel.add(label, matcherPanelConstraints);
		return label;
	}
	
	
}
