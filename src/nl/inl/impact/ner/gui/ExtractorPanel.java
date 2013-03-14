package nl.inl.impact.ner.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;

public class ExtractorPanel {
	
	private GridBagConstraints extractPanelConstraints = null;
	private JPanel extractPanel; // testing-tab
	private ImageIcon extractImageIcon = new ImageIcon("../impactLogo.gif");
	private MutableAttributeSet defaultAttrSet = new SimpleAttributeSet();
	public Font font = new Font("Lucida Sans", Font.ITALIC, 14);
	public Color bgColorPanel = new Color(176, 176, 173); // this matches the (grey) color of the logo
	
	
	public ExtractorPanel(){
		extractPanelConstraints = new GridBagConstraints();
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
		label = new JLabel(extractImageIcon);
		buildConstraints(extractPanelConstraints, 0, 0, 1, 1, 10, 10);
		//extractPanelConstraints.fill = GridBagConstraints.VERTICAL;
		//extractPanelConstraints.anchor = GridBagConstraints.NORTHEAST;
		//extractPanelConstraints.insets = new Insets(0, 0, 0, 10);
		panel.add(label, extractPanelConstraints);
		return label;
	}
	
	public JPanel createExtractorButtonPanel1(JPanel btnPanel, JButton btn1, JButton btn2, JButton btn3, JPanel panel){	
		btn1.updateUI();
		btn2.updateUI();
		btn3.updateUI();
		btn1.setBackground(bgColorPanel);
		btn2.setBackground(bgColorPanel);
		btn3.setBackground(bgColorPanel);
		btnPanel.add(btn1);
		btnPanel.add(btn2);
		btnPanel.add(btn3);
		
		//extractPanelConstraints.fill = GridBagConstraints.NONE;
		extractPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		buildConstraints(extractPanelConstraints, 0, 1, 1, 1, 10, 10);
		extractPanelConstraints.insets = new Insets(0, 20, 0, 0);
	
		btnPanel.setBackground(bgColorPanel);
		panel.add(btnPanel, extractPanelConstraints);
		return btnPanel;
	}
	
	public JLabel createCurrentClassifierLabel(JLabel label, JPanel panel){
		label = new JLabel("No classifier loaded.");
		label.setFont(font);
		//extractPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		extractPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		buildConstraints(extractPanelConstraints, 0, 2, 1, 1, 10, 1);
		extractPanelConstraints.insets = new Insets(0, 30, 0, 0);
		panel.add(label, extractPanelConstraints);
		return label;
	}
	
	public JLabel createCurrentStatusLabel(JLabel label, JPanel panel){
		label = new JLabel("Status: Ready.");
		label.setFont(font);
		//extractPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		extractPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		buildConstraints(extractPanelConstraints, 0, 3, 0, 1, 10, 1);
		extractPanelConstraints.insets = new Insets(0, 30, 10, 0);
		panel.add(label, extractPanelConstraints);
		return label;
	}
	
	public JLabel createIntroTextLabel(JLabel label, JPanel panel){
		label = new JLabel(
				"<html>To extract named entities from text,"
						+ " add text below and load an appropriate classifier. "
						+ " Add text by using copy/paste or by selecting File > Open File from the menu.</html>");

		buildConstraints(extractPanelConstraints, 18, 0, 1, 1, 100, 50);
		extractPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		extractPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		extractPanelConstraints.insets = new Insets(120, 10, 0, 0);
		panel.add(label, extractPanelConstraints);
		return label;
	}
	
	public JLabel createMainTextEditorPaneLabel(JLabel label, JPanel panel){
		label = new JLabel("TEXT");
		buildConstraints(extractPanelConstraints, 0, 4, 1, 1, 10, 1);
		extractPanelConstraints.anchor = GridBagConstraints.WEST;
		extractPanelConstraints.insets = new Insets(0, 30, 0, 0);
		panel.add(label, extractPanelConstraints);
		return label;
	}
	
	public JLabel createNEListLabel(JLabel label, JPanel panel){
		label = new JLabel("NAMED ENTITIES");
		buildConstraints(extractPanelConstraints, 18, 4, 1, 1, 10, 1);
		extractPanelConstraints.anchor = GridBagConstraints.WEST;
		extractPanelConstraints.insets = new Insets(0, 10, 0, 0);
		panel.add(label, extractPanelConstraints);
		return label;
	}
	
	public JLabel createVariantListLabel(JLabel label, JPanel panel){
		label = new JLabel("VARIANTS");
		extractPanelConstraints.anchor = GridBagConstraints.WEST;
		buildConstraints(extractPanelConstraints, 25, 4, 1, 1, 10, 1);
		extractPanelConstraints.insets = new Insets(0, 10, 0, 0);
		panel.add(label, extractPanelConstraints);
		return label;
	}
	
	public JScrollPane createMainTextEditorScrollPane(JEditorPane editPanel, JScrollPane scrollPane, JPanel panel){
		editPanel.updateUI();
		editPanel.setContentType("text/rtf");
		editPanel.setEditable(true);
		
		scrollPane = new JScrollPane(editPanel);
		extractPanelConstraints.fill = GridBagConstraints.BOTH;
		extractPanelConstraints.anchor = GridBagConstraints.EAST;
		buildConstraints(extractPanelConstraints, 0, 5, 18, 1, 100, 200);
		extractPanelConstraints.insets = new Insets(0, 20, 0, 0);
		panel.add(scrollPane, extractPanelConstraints);
		return scrollPane;
	}
	
	public JPanel createButtonPanel2(JPanel btnPanel, JButton btn1, JButton btn2, JPanel panel){
		btn1.updateUI();
		btn2.updateUI();
		btn1.setEnabled(false);
		btnPanel.updateUI();
		btn1.setBackground(bgColorPanel);
		btn2.setBackground(bgColorPanel);
		btnPanel.setBackground(bgColorPanel);
		btnPanel.add(btn1);
		btnPanel.add(btn2);
		extractPanelConstraints.fill = GridBagConstraints.NONE;
		extractPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		buildConstraints(extractPanelConstraints, 0, 6, 1, 1, 50, 50);
		extractPanelConstraints.insets = new Insets(0, 20, 0, 0);
		panel.add(btnPanel, extractPanelConstraints);
		return btnPanel;
	}
	
	public JPanel createTypeChoicePanel(JPanel typePanel, JLabel label, JCheckBox cb1, JCheckBox cb2, JCheckBox cb3, JPanel panel){
		
		typePanel.add(label);
		label.updateUI();
		cb1.updateUI();
		cb2.updateUI();
		cb3.updateUI();
		cb1.setBackground(bgColorPanel);
		cb2.setBackground(bgColorPanel);
		cb3.setBackground(bgColorPanel);
		typePanel.add(cb1);
		typePanel.add(cb2);
		typePanel.add(cb3);
		typePanel.setBackground(bgColorPanel);
	
		extractPanelConstraints.fill = GridBagConstraints.NONE;
		extractPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		buildConstraints(extractPanelConstraints, 1, 6, 1, 1, 50, 50);
		extractPanelConstraints.insets = new Insets(0, 0, 0, 0);
		panel.add(typePanel, extractPanelConstraints);
		return typePanel;
	}
	
	
	
	public JPanel createButtonNavPanel(JPanel btnPanel, JButton btn1, JButton btn2, JPanel panel){
		GridLayout buttons = new GridLayout(1, 2, 5, 5);
		btnPanel.setLayout(buttons);
		btn1.updateUI();
		btn2.updateUI();
		btn1.setBackground(bgColorPanel);
		btn2.setBackground(bgColorPanel);
		btn1.setEnabled(false);
		btnPanel.setBackground(bgColorPanel);
		btnPanel.add(btn1);
		btnPanel.add(btn2);
		extractPanelConstraints.fill = GridBagConstraints.NONE;
		extractPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		buildConstraints(extractPanelConstraints, 18, 6, 1, 1, 50, 50);
		extractPanelConstraints.insets = new Insets(0, 0, 0, 0);
		panel.add(btnPanel, extractPanelConstraints);
		return btnPanel;
	}
	
	public JScrollPane createNEListScrollPane(JScrollPane scrollPane, JList list, DefaultListModel listModel, JPanel panel){
	
		list.setVisibleRowCount(-1);
		list.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
	
		scrollPane = new JScrollPane(list);
		buildConstraints(extractPanelConstraints, 18, 5, 7, 1, 100, 200);
		extractPanelConstraints.fill = GridBagConstraints.BOTH;
		extractPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		extractPanelConstraints.insets = new Insets(0, 0, 0, 0);
		panel.add(scrollPane, extractPanelConstraints);
		return scrollPane;
	}
	
	public JScrollPane createVariantListScrollPane(JScrollPane scrollPane, JList list, JPanel panel){
		list.setVisibleRowCount(-1);
		scrollPane = new JScrollPane(list);
		extractPanelConstraints.fill = GridBagConstraints.BOTH;
		extractPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		buildConstraints(extractPanelConstraints, 25, 5, 7, 1, 100, 200);
		extractPanelConstraints.insets = new Insets(0, 0, 0, 20);
		panel.add(scrollPane, extractPanelConstraints);
		return scrollPane;
	}
	
	/*	Sets the initial text in the texteditor when the extractor is initialized */
	public void setInitText(JEditorPane editPanel, String initText){
		Document doc = new DefaultStyledDocument();
		editPanel.setDocument(doc);
		try {
			doc.insertString(0, initText, defaultAttrSet);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}
}
