package nl.inl.impact.ner.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import edu.stanford.nlp.util.StringUtils;

import nl.inl.impact.ner.io.FileHandler;
import nl.inl.impact.ner.spelvar.SpelVarFactory;
import nl.inl.impact.ner.stanfordplus.ImpactCRFClassifier;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TrainerPanel {

	JPanel trainPanel;
	
	private GridBagConstraints trainPanelConstraints = null;

	private JButton trainButton = null;
	private JEditorPane mainTextEditorPaneProps;
	private JEditorPane mainTextEditorPaneTrainfile;
	private JLabel trainingfileLabel = null;
	private JTextField trainingfileTextField = null;
	private JLabel gazetteerLabel = null;
	private JTextArea gazetteerTextArea = null;
	private JLabel saveModelToLabel = null;
	private JTextField saveModelToTextField = null;
	private JLabel doVariationReductionLabel = null;
	public JCheckBox doVariationReductionCheckBox = null;
	private JLabel propsLabel = null;
	
	private static String DEFAULTPROPERTIES = "/mnt/Projecten/Impact/NER/NER_tools/NERT/data/props/newspapers18c.props";
	
	private boolean useSpelVar = true;
	
	private static HashMap<String, String> svPropsHM = new HashMap<String, String>();
	
	private MutableAttributeSet defaultAttrSet = new SimpleAttributeSet();
	FileHandler fileHandler = new FileHandler();
	/*
	 * these are the properties that will be sent to the classifier, unless they
	 * are overwritten by a loaded propsfile.
	 */
	public String[] properties = {
			"-trainfile",
			"", // will be set automatically after user input
			"-serializeTo",
			"", // will be set automatically after user input
			"-useSpelVar",
			"true", // will be set automatically after user input
			"-useGazettes",
			"false", // will be set automatically after user input
			"-sloppyGazette",
			"false", // will be set automatically after user input
			"-gazette",
			"", // will be set automatically after user input
			"-useTags", "false", "-useClassFeature", "true", "-useWord",
			"true", "-useTags", "true", "-useNGrams", "true", "-noMidNGrams",
			"false", "-maxNGramLeng", "6", "-usePrev", "true", "-useNext",
			"true", "-useSequences", "true", "-usePrevSequences", "true",
			"-maxLeft", "1", "-useTypeSeqs", "true", "-useTypeSeqs2", "true",
			"-useTypeySequences", "true", "-wordShape", "chris2useLC",
			"-useDisjunctive", "true", "-spelvarMaxNgramLength", "6",
			"-spelvarMinNgramLength", "4", "-spelvarNgramOverlapThreshold",
			"2", "-spelvarMinStringLength", "-1", "-spelvarStandardLDWeight",
			"2", "-spelvarCompareLowerCaseLD", "false", "-spelvarLDThreshold",
			"1" };

	public String[] spelvarProperties = { "-spelvarMaxNgramLength", "6",
			"-spelvarMinNgramLength", "4", "-spelvarNgramOverlapThreshold",
			"2", "-spelvarMinStringLength", "-1", "-spelvarStandardLDWeight",
			"2", "-spelvarCompareLowerCaseLD", "false", "-spelvarLDThreshold",
			"1" };
	
	
	private ActionListener actor = new ActionPerformer();
	
	public JPanel buildTrainPanel() {
		
		trainPanel = new JPanel(new GridBagLayout());
		trainPanelConstraints = new GridBagConstraints();

		trainingfileLabel = new JLabel("Training file");
		trainPanelConstraints.fill = GridBagConstraints.NONE;
		trainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		trainPanelConstraints.gridx = 0;
		trainPanelConstraints.gridy = 0;
		trainPanelConstraints.insets = new Insets(5, 0, 5, 0);
		trainPanel.add(trainingfileLabel, trainPanelConstraints);

		trainingfileTextField = new JTextField();
		trainingfileTextField.addActionListener(actor);
		trainingfileTextField.setBorder(null);
		trainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		trainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		trainPanelConstraints.gridx = 1;
		trainPanelConstraints.gridy = 0;
		trainPanelConstraints.insets = new Insets(5, 0, 5, 0);
		trainPanel.add(trainingfileTextField, trainPanelConstraints);

		gazetteerLabel = new JLabel("Gazetteer lists");
		trainPanelConstraints.fill = GridBagConstraints.NONE;
		trainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		trainPanelConstraints.gridx = 0;
		trainPanelConstraints.gridy = 1;
		trainPanelConstraints.insets = new Insets(5, 0, 5, 0);
		trainPanel.add(gazetteerLabel, trainPanelConstraints);

		gazetteerTextArea = new JTextArea("", 5, 2);
		trainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		trainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		trainPanelConstraints.gridx = 1;
		trainPanelConstraints.gridy = 1;
		trainPanelConstraints.insets = new Insets(5, 0, 5, 0);
		trainPanel.add(gazetteerTextArea, trainPanelConstraints);

		saveModelToLabel = new JLabel("Save model to ");
		trainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		trainPanelConstraints.fill = GridBagConstraints.NONE;
		trainPanelConstraints.gridx = 0;
		trainPanelConstraints.gridy = 2;
		trainPanelConstraints.insets = new Insets(5, 0, 5, 0);
		trainPanel.add(saveModelToLabel, trainPanelConstraints);

		saveModelToTextField = new JTextField();
		saveModelToTextField.addActionListener(actor);
		saveModelToTextField.setBorder(null);
		trainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		trainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		trainPanelConstraints.gridx = 1;
		trainPanelConstraints.gridy = 2;
		trainPanelConstraints.insets = new Insets(5, 0, 5, 0);
		trainPanel.add(saveModelToTextField, trainPanelConstraints);

		doVariationReductionLabel = new JLabel(
				"Use spelling variation reduction");
		trainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		trainPanelConstraints.fill = GridBagConstraints.NONE;
		trainPanelConstraints.gridx = 0;
		trainPanelConstraints.gridy = 3;
		trainPanelConstraints.insets = new Insets(5, 0, 5, 0);
		trainPanel.add(doVariationReductionLabel, trainPanelConstraints);

		propsLabel = new JLabel("Properties ");
		trainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		trainPanelConstraints.fill = GridBagConstraints.NONE;
		trainPanelConstraints.gridx = 0;
		trainPanelConstraints.gridy = 4;
		trainPanelConstraints.insets = new Insets(5, 0, 5, 0);
		trainPanel.add(propsLabel, trainPanelConstraints);

		buildContentPanelsForTraining();
		//buildTagPanel();			//turned this off, is there a tagpanel in the trainingpanel??
		buildTrainButton();
		buildSpelVarCheckBox();
		
		return trainPanel;
	}

	private void buildContentPanelsForTraining() {

		// pane with properties
		mainTextEditorPaneProps = new JEditorPane();
		mainTextEditorPaneProps.setContentType("text/rtf");
		mainTextEditorPaneProps.addKeyListener(new InputListener());

		// defaultAttrSet =
		// ((StyledEditorKit)mainTextEditorPane.getEditorKit()).getInputAttributes();
		StyleConstants.setFontFamily(defaultAttrSet, "Lucida Sans");
		fillPropsField();
		JScrollPane scrollPane = new JScrollPane(mainTextEditorPaneProps);
		scrollPane.setPreferredSize(new Dimension(400, 500));
		scrollPane.setBorder(null);
		trainPanelConstraints.fill = GridBagConstraints.NONE;
		trainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		trainPanelConstraints.gridx = 1;
		trainPanelConstraints.gridy = 4;
		trainPanelConstraints.insets = new Insets(5, 0, 5, 0);
		trainPanel.add(scrollPane, trainPanelConstraints);
		// frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
		mainTextEditorPaneProps.setEditable(true);

		// pane with trainfile
		/*
		 * mainTextEditorPaneTrainfile = new JEditorPane ();
		 * mainTextEditorPaneTrainfile.setContentType("text/rtf");
		 * mainTextEditorPaneTrainfile.addKeyListener(new InputListener());
		 * 
		 * // defaultAttrSet =
		 * ((StyledEditorKit)mainTextEditorPane.getEditorKit(
		 * )).getInputAttributes(); StyleConstants.setFontFamily(defaultAttrSet,
		 * "Lucida Sans"); doc = new DefaultStyledDocument();
		 * mainTextEditorPaneTrainfile.setDocument(doc); try {
		 * doc.insertString(0, "trainfile", defaultAttrSet); } catch (Exception
		 * ex) { ex.printStackTrace(); System.exit(-1); }
		 * 
		 * JScrollPane scrollPane2 = new
		 * JScrollPane(mainTextEditorPaneTrainfile);
		 * scrollPane2.setPreferredSize(new Dimension(200, 400));
		 * trainPanel.add(scrollPane2); //frame.getContentPane().add(scrollPane,
		 * BorderLayout.CENTER); mainTextEditorPaneTrainfile.setEditable(true);
		 */
	}

	/*
	 * This method fills the properties textfield. It is called after a change
	 * has been made to one of the settings.
	 */

	private void fillPropsField() {
		Document doc = new DefaultStyledDocument();
		mainTextEditorPaneProps.setDocument(doc);
		for (int i = (properties.length - 1); i >= 0; i -= 2) {
			String p = properties[i - 1] + "=" + properties[i] + "\n";
			try {
				doc.insertString(0, p.substring(1, p.length()), defaultAttrSet);
			} catch (Exception ex) {
				ex.printStackTrace();
				System.exit(-1);
			}
		}
	}

	
	private void buildSpelVarCheckBox() {
		if (doVariationReductionCheckBox == null) {

			trainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
			// trainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
			trainPanelConstraints.gridx = 1;
			trainPanelConstraints.gridy = 3;
			trainPanelConstraints.insets = new Insets(5, 0, 5, 0);
			doVariationReductionCheckBox = new JCheckBox("", true);
			trainPanel.add(doVariationReductionCheckBox, trainPanelConstraints);
			doVariationReductionCheckBox.addActionListener(actor);
		}
	}

	

	private void buildTrainButton() {
		if (trainButton == null) {
			trainButton = new JButton("Train model");
			trainPanelConstraints.fill = GridBagConstraints.NONE;
			trainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
			trainPanelConstraints.gridx = 1;
			trainPanelConstraints.gridy = 5;
			trainPanel.add(trainButton, trainPanelConstraints);
			// frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			trainButton.addActionListener(actor);
		}
	}

	private void loadGazetteerlists(File file) {
		System.err.println("Loading gazetteer file");
		try {
			String s = gazetteerTextArea.getText();
			if (!s.equals("")) {
				s += "\n";
			}
			s += file.getAbsolutePath();
			gazetteerTextArea.setText(s);
			s = s.replaceAll("\n", ";");
			properties[7] = "true";
			properties[9] = "true";
			properties[11] = s;
			fillPropsField();
		} catch (Exception e) {
			System.err.println("Error loading |" + file + "|");
			e.printStackTrace();
			fileHandler.displayError("Error Loading URL " + file, "Message: "
					+ e.toString());
			return;
		}
	}
	

	private void loadDefaultPropertiesFile() {
		loadPropertiesFile(DEFAULTPROPERTIES);
	}

	private void loadPropertiesFile(File file) {
		loadPropertiesFile(file.toURI().toString());
	}

	private void loadPropertiesFile(String file) {
		System.err.println("Loading properties file " + file);
		try {
			mainTextEditorPaneProps.setPage(file);
		} catch (Exception e) {
			System.err.println("Error loading |" + file + "|");
			e.printStackTrace();
			fileHandler.displayError("Error Loading URL " + file, "Message: "
					+ e.toString());
			return;
		}
		mainTextEditorPaneProps.revalidate();
		mainTextEditorPaneProps.repaint();
		mainTextEditorPaneProps.setEditable(true);
	}

	
	private void initTraining() throws IOException {
		System.err.println("Init training");
		/*
		 * //store trainingfile in string DefaultStyledDocument doc =
		 * (DefaultStyledDocument)mainTextEditorPaneTrainfile.getDocument();
		 * String trainfilecontent = null; try { trainfilecontent=
		 * doc.getText(0, doc.getLength()); } catch (Exception e) {
		 * e.printStackTrace(); }
		 */
		System.err.println(">>" + properties[0] + "< >" + properties[1] + "<");
		System.err.println(">>" + properties[2] + "< >" + properties[3] + "<");
		// check if trainfile has been entered
		if (properties[1].equals("")) {
			JOptionPane.showMessageDialog(null,
					"Please enter a training file name.");
			return;
		}
		// check if a modelname has been entered
		// it could be that a name has been type in, but
		// that the user has not pressed enter. So first get
		// the text from the text field and check this.

		String s = saveModelToTextField.getText();
		System.err.println("save to: " + s);
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
				JOptionPane.showMessageDialog(null, "Invalid pathname.");
			}
		} else {
			properties[3] = s;
			fillPropsField();
		}

		if (properties[3].equals("")) {
			JOptionPane.showMessageDialog(null,
					"Please enter a name for the model to be created.");
			return;
		}
		if ((!properties[1].equals("")) && (!properties[3].equals(""))) {
			// store props in string
			DefaultStyledDocument doc = (DefaultStyledDocument) mainTextEditorPaneProps
					.getDocument();
			String str = null;
			try {
				str = doc.getText(0, doc.getLength());
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.err.println("propsstring: >" + str + "<");
			// String trainfiletemp =
			// "/mnt/Projecten/Impact/NER/NER_tools/NERT/data/traintest/newspapers18c_train.txt";
			// String propstemp =
			// "-props /mnt/Projecten/Impact/NER/NER_tools/NERT/data/props/newspapers18c.props";
			// String labeledText = classifier.classifyWithInlineXML(text);
			Properties props = stringToProperties(str);
			// result = new Properties();
			// return stringToProperties(str, result);
			// String[] prop = props.split("\\s+");

			System.err.println("START CREATING NEW CRF");
			ImpactCRFClassifier crf = new ImpactCRFClassifier(props);
			SpelVarFactory sv = new SpelVarFactory();
			System.err.println("ici");
			// crf.startCRFClassifier(crf, props, svPropsHM);
		}
	}

	
	public void setProperties() {
		/* set the properties for flags that differ from the default settings */
		String[] properties = {
				"-trainfile",
				"", // will be set automatically after user input
				"-serializeTo",
				"", // will be set automatically after user input
				"-useSpelVar",
				"true", // will be set automatically after user input
				"-useGazettes",
				"false", // will be set automatically after user input
				"-sloppyGazette",
				"false", // will be set automatically after user input
				"-gazette",
				"", // will be set automatically after user input
				"-useClassFeature", "true", "-useWord", "true", "-useTags",
				"true", "-useNGrams", "true", "-noMidNGrams", "false",
				"-maxNGramLeng", "6", "-usePrev", "true", "-useNext", "true",
				"-useSequences", "true", "-usePrevSequences", "true",
				"-maxLeft", "1", "-useTypeSeqs", "true", "-useTypeSeqs2",
				"true", "-useTypeySequences", "true", "-wordShape",
				"chris2useLC", "-useDisjunctive", "true" };

		Properties props = StringUtils.argsToProperties(properties);
		ImpactCRFClassifier crf = new ImpactCRFClassifier(props);
	}

	
	
	// method from StringUtils.java
	/**
	 * This method updates a Properties object based on a comma-separated String
	 * (with whitespace optionally allowed after the comma) representing
	 * properties to a Properties object. Each property is "property=value". The
	 * value for properties without an explicitly given value is set to "true".
	 */
	public static Properties stringToProperties(String str) {
		System.err.println("\nstringToProperties()");
		Properties pr = new Properties();
		// String[] propsStr = str.trim().split(",\\s*");
		// use new line as splitter
		String[] propsStr = str.trim().split("\\n|\\r");
		for (int i = 0; i < propsStr.length; i++) {
			String term = propsStr[i];
			int divLoc = term.indexOf('=');
			String key;
			String value;
			if (divLoc >= 0) {
				key = term.substring(0, divLoc).trim();
				value = term.substring(divLoc + 1).trim();
			} else {
				key = term.trim();
				value = "true";
			}

			// we need to separate any spelvar flags, because they are not
			// recognized
			// in the orginal Stanford jar. Save them by
			// overriding the existing String[] spelvarproperties
			// if(!key.equals("useSpelVar")){
			System.err.println("key: " + key + " value: " + value);
			if (key.toLowerCase().indexOf("spelvar") > -1) {
				System.err.println("spelvar key: " + key);
				svPropsHM.put(key, value);
			}
			pr.setProperty(key, value);
			// }
		}
		System.err.println("DONE");
		return pr;
	}

	

	private void loadTrainingFile(File file) {
		System.err.println("Loading training file " + file);
		try {
			// trainingfileTextField.setText(file.toURI().toString());
			trainingfileTextField.setText(file.getAbsolutePath());
			// properties[1] = file.toURI().toString();
			properties[1] = file.getAbsolutePath();
			fillPropsField();
		} catch (Exception e) {
			System.err.println("Error loading |" + file + "|");
			e.printStackTrace();
			fileHandler.displayError("Error Loading URL " + file, "Message: "
					+ e.toString());
			return;
		}
	}
	
	
	private class InputListener implements KeyListener {
		public void keyPressed(KeyEvent e) {
		}

		public void keyReleased(KeyEvent e) {
		}

		public void keyTyped(KeyEvent e) {
			//saveTaggedAs.setEnabled(false);
		}
	}
	
	private class ActionPerformer implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			String com = e.getActionCommand();

			
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

			
			//turned off, because the menu is turned off.
			//if (src == loadGazetteers) {
			//	loadGazetteerlists(fileHandler.getFile(true));
			//}
			
			
			
			
			
					/*
			if (com.equals("Open File")) {
				File file = fileHandler.getFile(true);
				if (file != null) {
					openFile(file);
				}
			} *//*else if (com.equals("Load URL")) {
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
		}
	}
	
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
	
}
