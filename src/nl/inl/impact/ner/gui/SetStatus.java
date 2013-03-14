package nl.inl.impact.ner.gui;

import javax.swing.JLabel;

public class SetStatus {
	/*
	 * protected static testImageIcon createtestImageIcon(String path, String
	 * description) { java.net.URL imgURL = RunNERT.class.getResource(path); if
	 * (imgURL != null) { return new testImageIcon(); //return new
	 * testImageIcon(imgURL, description); } else {
	 * System.err.println("Couldn't find file: " + path); return null; } }
	 */
	public void setStatus(String txt, JLabel thisLabel) {
		String preTxt = "";
		if(thisLabel.toString().equals("currentStatusLabel")){preTxt = "Status: ";}
		thisLabel.setText(preTxt + txt);
		thisLabel.paintImmediately(thisLabel.getVisibleRect());
	}
}
