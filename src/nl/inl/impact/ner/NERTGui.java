//this is based on the class NERGUI from Stanford

//NERGUI -- a GUI for a probabilistic (CRF) sequence model for NER.
//Copyright (c) 2002-2008 The Board of Trustees of
//The Leland Stanford Junior University. All Rights Reserved.
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//For more information, bug reports, fixes, contact:
// Christopher Manning
// Dept of Computer Science, Gates 1A
// Stanford CA 94305-9010
// USA
// Support/Questions: java-nlp-user@lists.stanford.edu
// Licensing: java-nlp-support@lists.stanford.edu
// http://nlp.stanford.edu/downloads/crf-classifier.shtml

package nl.inl.impact.ner;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import nl.inl.impact.ner.extractor.Extractor;
import nl.inl.impact.ner.gui.TrainerPanel;
import nl.inl.impact.ner.matcher.Matcher;

/**
 * A GUI for Named Entity sequence classifiers. This version only supports the
 * CRF.
 * 
 * @author Jenny Finkel
 * @author Christopher Manning
 */

public class NERTGui {
	//calls the constructor for the entire panel for matching, and its methods 
	Matcher matcher = new Matcher();		
	//calls the constructor for the entire panel for extracting, and its methods
	Extractor extractor = new Extractor();
	//calls the constructor for the entire panel for training, and its methods
	TrainerPanel trainerPanel = new TrainerPanel();

	
	// frames, panels, labels, etcF
	private JFrame frame;
		
	// general variables
	private static int HEIGHT = 1000;
	private static int WIDTH = 1200;

	// menu items
	/*
	private JMenuItem saveUntagged = null;
	private JMenuItem saveTaggedAs = null;
	private JMenuItem extract = null;
	private JMenuItem loadTrainingfile = null;
	private JMenuItem loadPropsfile = null;
	private JMenuItem loadGazetteers = null;
*/
	/*
	public TreeMap<String, String> extractedNETM = new TreeMap<String, String>(
			String.CASE_INSENSITIVE_ORDER);
	public HashMap<String, String> selectedNEPosHM = new HashMap<String, String>();
	public ArrayList<Integer> selectedNEPosList = new ArrayList<Integer>();
	public ArrayList<Integer> previouslySelectedNEPosList = new ArrayList<Integer>();
	
	public static HashMap<Integer, String> labeledSentences;

	
	String labeledText = "";
	boolean storeNEPositions = true;
*/	
	
	/* variables for matching, the boolean value is the setting for use as a lemma */
	//private HashMap<String, Boolean> loadedFiles = new HashMap<String, Boolean>();

	

	

	private void createAndShowGUI() {
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame = new JFrame("Impact / Stanford Named Entity Recognizer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setPreferredSize(new Dimension(WIDTH, HEIGHT));
		
		//turned off the menubar for now, but this restricts some 
		//of the functionality:
		//-loading classifier
		//-loading gazetteer lists
		//frame.setJMenuBar(addMenuBar());
		

		//JPanel matcherPane = matcher.buildMatcherPanel();
		JPanel extractorPane = extractor.buildExtractorPanel();
		//JPanel trainerPane = trainerPanel.buildTrainPanel();		
		

		// construct tabs, one for training, one for testing
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("NE Extraction", extractorPane);
		//tabbedPane.addTab("Training", trainerPane);
		//tabbedPane.addTab("Variant matching", matcherPane);
		frame.add(tabbedPane);
		// tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	
	
	
	

	

	/*
	public void exit() {
		// ask if they're sure?
		System.exit(-1);
	}
*/
	
	

	
	
	
	

	
	/**
	 * Run the GUI. This program accepts no command-line arguments. Everything
	 * is entered into the GUI.
	 */
	public static void main(String[] args) {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				NERTGui nertGui = new NERTGui();
				try {
					for (LookAndFeelInfo info : UIManager
							.getInstalledLookAndFeels()) {
						if ("Nimbus".equals(info.getName())) {
							UIManager.setLookAndFeel(info.getClassName());
							break;
						}
					}
				} catch (Exception e) {
					// If Nimbus is not available, you can set the GUI to
					// another look and feel.
					UIManager.put("swing.boldMetal", Boolean.FALSE);
				}
				nertGui.createAndShowGUI();
			}
		});
	}

	
/*
	private class ActionPerformer implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			String com = e.getActionCommand();
*/
			// matching panel
			/*
			if (src == matchingLoadFileButton) {
				System.err.println("Loading file for matching");
				File file = fileHandler.getFile(true);
				if (file != null) {
					matcherTab.loadNEFile(file, tableModel, matchingFileListTable);
					fillMatchingNEList();
					setStatus(mfMatch.NETM.size() + " NEs. ", matchingStatusLabel1);
				}
			}
			
			if (src == matchingDeleteFileButton) {
				System.err.println("Removing file");
				int index = matchingFileListTable.getSelectedRow();
				if(index == -1){
				JOptionPane.showMessageDialog(frame,
						    "Please select a file to remove.",
						    "Error removing file ...",
						    JOptionPane.WARNING_MESSAGE);
				}
				else{
					String f = matchingFileListTable.getModel().getValueAt(index, 1).toString();
					System.err.println("Removing file "+f);
					removeMatchingFile(f, index);
				}
		      }

			if (src == matchingStartButton) {
				System.err.println("Starting matching NE's in NElist ... ");
				setStatus("Status: Matching ...", matchingStatusLabel2);
				// check lemma use first
				boolean lem = false;
				for(String f : matcherTab.loadedFiles.keySet()){
					if(matcherTab.loadedFiles.get(f)){
						lem = true;
					}
				}
				mfMatch.useLemma = lem;
				boolean y = mfMatch.initNEMatching(true, true, true);
				fillMatchingNEList();
				pairViewRadioButton.setVisible(true);
				groupViewRadioButton.setVisible(true);
				setStatus("Status: Ready.", matchingStatusLabel2);
			}
			
			if (src == matchingExportButton) {
				
				System.err.println("Exporting NEs ... ");
				setStatus("Status: Exporting ...", matchingStatusLabel2);
				fileHandler.printToFile(fileHandler.getFile(false), matcherTab.writeDataToString(matchingNETable));
				setStatus("Exported table.", matchingStatusLabel1);
				setStatus("Status: Ready.", matchingStatusLabel2);
			}
			
			

			if ((src == pairViewRadioButton) || (src == groupViewRadioButton)) {
				// user has changed view, so repaint the NE list.
				//repaintMatchingLists(pairViewRadioButton.isSelected());
				fillMatchingNEList();
			}*/
/*
			// test panel
			if (src == clearButton) {
				clearextractPanel();
			}

			if (src == nextButton) {
				selectedNEIndex++;
				System.err.println("BtnNext. Moving to occurrence "
						+ (selectedNEIndex + 1) + "/" + selectedNEPosHM.size());
				// enable previousButton if selectedNEIndex > 0
				if (selectedNEIndex > 0) {
					previousButton.setEnabled(true);
				}
				// disable button if there are no more NE's
				if (selectedNEIndex == (selectedNEPosHM.size() - 1)) {
					nextButton.setEnabled(false);
				}
				// set caret position
				if ((selectedNEIndex >= 0)
						&& (selectedNEIndex <= (selectedNEPosHM.size()) - 1)) {
					moveCaretToNE();
				}
			}

			if (src == previousButton) {
				selectedNEIndex--;
				System.err.println("BtnPrev. Moving to occurrence "
						+ (selectedNEIndex + 1) + "/" + selectedNEPosHM.size());
				if (selectedNEIndex == 0) {
					previousButton.setEnabled(false);
				}
				if (selectedNEIndex < selectedNEPosHM.size()) {
					nextButton.setEnabled(true);
				}
				// set caret position
				if ((selectedNEIndex >= 0)
						&& (selectedNEIndex < (selectedNEPosHM.size()) - 1)) {
					moveCaretToNE();
				}
			}
				*/
			/*
			if (src == trainingfileTextField) { // fires after 'enter'
				String s = trainingfileTextField.getText();
				File f = new File(s);
				if (!f.exists()) {
					JOptionPane.showMessageDialog(null,
							"No such file or directory:\n" + s);
				} else {
					properties[1] = s;
					fillPropsField();
				}
			}
			*/
			/*
			if (src == typeLOC) {
				if (typeLOC.isSelected()) {
					System.err.println("Highlight LOCs");
				} else {
					System.err.println("Do not highlight LOCs");
				}
				DefaultStyledDocument doc = (DefaultStyledDocument) mainTextEditorPane
						.getDocument();
				repaintAfterTagHighlightChange();
				// updateTextFieldForXMLAfterExtraction(doc);
			}
			if (src == typeORG) {
				if (typeORG.isSelected()) {
					System.err.println("Highlight ORGs");
				} else {
					System.err.println("Do not highlight ORGs");
				}
				DefaultStyledDocument doc = (DefaultStyledDocument) mainTextEditorPane
						.getDocument();
				repaintAfterTagHighlightChange();
				// updateTextFieldForXMLAfterExtraction(doc);
			}
			if (src == typePER) {
				if (typePER.isSelected()) {
					System.err.println("Highlight PERs");
				} else {
					System.err.println("Do not highlight PERs");
				}
				DefaultStyledDocument doc = (DefaultStyledDocument) mainTextEditorPane
						.getDocument();
				// updateTextFieldForXMLAfterExtraction(doc);
				repaintAfterTagHighlightChange();
			}*/
/*
			if (src == saveModelToTextField) { // fires after 'enter'
				String s = saveModelToTextField.getText();
				// check if pathname is valid, without the actual filename
				int dirInd = s.lastIndexOf("/");
				if (dirInd > 0) {
					String pathname = s.substring(0, dirInd);
					File f = new File(pathname);
					if (f.exists()) {
						System.err.println("valid pathname: " + pathname);
						properties[3] = s;
						fillPropsField();
					} else {
						JOptionPane
								.showMessageDialog(null, "Invalid pathname.");
					}
				} else {
					properties[3] = s;
					fillPropsField();
				}
			}

			if (src == doVariationReductionCheckBox) {
				if (doVariationReductionCheckBox.isSelected()) {
					properties[5] = "true";
					useSpelVar = true;
					// add spelvarproperties
					for (int i = 46; i < 60; i++) {
						properties[i] = spelvarProperties[i - 46];
					}
				} else {
					properties[5] = "false";
					useSpelVar = false;
					// erase all property values for spelvar:
					for (int i = 46; i < 60; i++) {
						properties[i] = "";
					}
				}
				fillPropsField();
			}

			if (src == loadGazetteers) {
				loadGazetteerlists(fileHandler.getFile(true));
			}

			if (com.equals("Open File")) {
				File file = fileHandler.getFile(true);
				if (file != null) {
					openFile(file);
				}
			} /*else if (com.equals("Load URL")) {
				String url = getURL();
				if (url != null) {
					openURL(url);
				}
			} else if (com.equals("Exit")) {
				exit();
			} /*else if (com.equals("Clear")) {
				clearextractPanel();
			} *//*else if (com.equals("Cut")) {
				cutDocument();
			} else if (com.equals("Copy")) {
				copyDocument();
			} else if (com.equals("Paste")) {
				pasteDocument();
			} *//*else if (com.equals("Load CRF From File")) {
				File file = fileHandler.getFile(true);
				if (file != null) {
					loadClassifier(file);
				}
			} else if (com.equals("Load Default CRF")) {
				loadClassifier(null);
			} else if (com.equals("Extract Named Entities")) {
				// first empty all relevant HMs and textfields
				// labeledSentences.clear();
				ImpactCRFClassifier.labeledSentences.clear();
				storeNEPositions = true;
				NEListModel.clear();
				extractedNETM.clear();
				currentVariants.clear();
				selectedNEPosHM.clear();
				selectedNEPosList.clear();
				previouslySelectedNEs.clear();
				labeledText = "";
				NEListModel.clear();
				variantListModel.clear();
				setStatus("Extracting NE's ...", currentStatusLabel);
				extractorTab.extract(mainTextEditorPane, labeledText, classifier);
				saveTaggedAs.setEnabled(true);
				setStatus("Creating NE-list ...", currentStatusLabel);
				// done extracting. Now it's time to setup the NE-list with references
				// to the main text and to do variant matching
				fillNEList();
				initVariantMatching();
				setStatus("Ready.", currentStatusLabel);
				mainTextEditorPane.setCaretPosition(0);
				NEList.requestFocus();
				NEList.ensureIndexIsVisible(0);

			} else if (com.equals("Save Untagged File")) {
				saveUntaggedContents(loadedFile);
			} else if (com.equals("Save Untagged File As ...")) {
				saveUntaggedContents(fileHandler.getFile(false));
			} else if (com.equals("Save Tagged File As ...")) {
				fileHandler.printToFile(fileHandler.getFile(false), taggedContents);
			} else if (com.equals("Load Training File")) {
				loadTrainingFile(fileHandler.getFile(true));
			} else if (com.equals("Load Properties File")) {
				loadPropertiesFile(fileHandler.getFile(true));
			} else if (com.equals("Load Default Properties File")) {
				loadDefaultPropertiesFile();
			} else if (com.equals("Train model")) {
				initTraining();
			}
			// else {
			// System.err.println("Unknown Action: "+e);
			// }
		
			 */
	/*
		}
	}
	*/
	/*
	public void saveUntaggedContents(File file) {
		try {
			String contents;
			if (mainTextEditorPane.getContentType().equals("text/html")) {
				contents = mainTextEditorPane.getText();
			} else {
				Document doc = mainTextEditorPane.getDocument();
				contents = doc.getText(0, doc.getLength());
			}
			fileHandler.printToFile(file, contents);
			saveUntagged.setEnabled(true);
			loadedFile = file;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
*/
	
	/*
	public String getURL() {
		String url = JOptionPane.showInputDialog(frame, "URL: ", "Load URL",
				JOptionPane.QUESTION_MESSAGE);
		return url;
	}

*/
	


	

	/*****************************************************************/
	// setup tabs: test, train and matching
	/*****************************************************************/

	
/*
	private void buildMainTextEditorPane() {

		
	}

*/	
	/*
	private JMenuBar addMenuBar() {
		JMenuBar menubar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		menubar.add(fileMenu);

		JMenu editMenu = new JMenu("Edit");
		menubar.add(editMenu);

		JMenu classifierMenu = new JMenu("Classifier");
		menubar.add(classifierMenu);

		// FILE MENU
		JMenuItem openFile = new JMenuItem("Open File");
		openFile.setMnemonic('O');
		openFile.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_F, java.awt.Event.CTRL_MASK));
		openFile.addActionListener(actor);
		fileMenu.add(openFile);

		JMenuItem loadURL = new JMenuItem("Load URL");
		loadURL.setMnemonic('L');
		loadURL.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_U, java.awt.Event.CTRL_MASK));
		loadURL.addActionListener(actor);
		fileMenu.add(loadURL);

		fileMenu.add(new JSeparator());

		saveUntagged = new JMenuItem("Save Untagged File");
		saveUntagged.setMnemonic('S');
		saveUntagged.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_S, java.awt.Event.CTRL_MASK));
		saveUntagged.addActionListener(actor);
		saveUntagged.setEnabled(false);
		fileMenu.add(saveUntagged);

		JMenuItem saveUntaggedAs = new JMenuItem("Save Untagged File As ...");
		saveUntaggedAs.setMnemonic('U');
		saveUntaggedAs.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_U, java.awt.Event.CTRL_MASK));
		saveUntaggedAs.addActionListener(actor);
		fileMenu.add(saveUntaggedAs);

		saveTaggedAs = new JMenuItem("Save Tagged File As ...");
		saveTaggedAs.setMnemonic('T');
		saveTaggedAs.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_T, java.awt.Event.CTRL_MASK));
		saveTaggedAs.addActionListener(actor);
		saveTaggedAs.setEnabled(false);
		fileMenu.add(saveTaggedAs);

		fileMenu.add(new JSeparator());

		loadTrainingfile = new JMenuItem("Load Training File");
		loadTrainingfile.setMnemonic('V');
		loadTrainingfile.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_V, java.awt.Event.CTRL_MASK));
		loadTrainingfile.addActionListener(actor);
		// loadPropsfile.setEnabled(false);
		fileMenu.add(loadTrainingfile);

		loadPropsfile = new JMenuItem("Load Properties File");
		loadPropsfile.setMnemonic('P');
		loadPropsfile.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_P, java.awt.Event.CTRL_MASK));
		loadPropsfile.addActionListener(actor);
		// loadPropsfile.setEnabled(false);
		fileMenu.add(loadPropsfile);

		loadGazetteers = new JMenuItem("Load Gazetteers");
		loadGazetteers.setMnemonic('G');
		loadGazetteers.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_G, java.awt.Event.CTRL_MASK));
		loadGazetteers.addActionListener(actor);
		fileMenu.add(loadGazetteers);

		fileMenu.add(new JSeparator());

		JMenuItem exit = new JMenuItem("Exit");
		exit.setMnemonic('x');
		exit.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_Q, java.awt.Event.CTRL_MASK));
		exit.addActionListener(actor);
		fileMenu.add(exit);

		// EDIT MENU
		/*
		JMenuItem cut = new JMenuItem("Cut");
		cut.setMnemonic('X');
		cut.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X,
				java.awt.Event.CTRL_MASK));
		cut.addActionListener(actor);
		editMenu.add(cut);

		JMenuItem copy = new JMenuItem("Copy");
		copy.setMnemonic('C');
		copy.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_C, java.awt.Event.CTRL_MASK));
		copy.addActionListener(actor);
		editMenu.add(copy);

		JMenuItem paste = new JMenuItem("Paste");
		paste.setMnemonic('V');
		paste.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_V, java.awt.Event.CTRL_MASK));
		paste.addActionListener(actor);
		editMenu.add(paste);

		JMenuItem clear = new JMenuItem("Clear");
		clear.setMnemonic('C');
		clear.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_L, java.awt.Event.CTRL_MASK));
		clear.addActionListener(actor);
		editMenu.add(clear);
		 */
	/*
		// CLASSIFIER MENU
		JMenuItem loadCRF = new JMenuItem("Load CRF From File");
		loadCRF.setMnemonic('R');
		loadCRF.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_R, java.awt.Event.CTRL_MASK));
		loadCRF.addActionListener(actor);
		classifierMenu.add(loadCRF);

		JMenuItem loadDefaultCRF = new JMenuItem("Load Default CRF");
		loadDefaultCRF.setMnemonic('L');
		loadDefaultCRF.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_L, java.awt.Event.CTRL_MASK));
		loadDefaultCRF.addActionListener(actor);
		classifierMenu.add(loadDefaultCRF);

		extract = new JMenuItem("Extract NE's");
		extract.setMnemonic('N');
		extract.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_N, java.awt.Event.CTRL_MASK));
		extract.addActionListener(actor);
		classifierMenu.add(extract);
		extract.setEnabled(false);

		return menubar;
	}
*/
	

}



