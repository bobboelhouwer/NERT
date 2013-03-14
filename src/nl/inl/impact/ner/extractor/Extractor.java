package nl.inl.impact.ner.extractor;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import nl.inl.impact.ner.gui.Colors;
import nl.inl.impact.ner.gui.ExtractorPanel;
import nl.inl.impact.ner.gui.SetStatus;
import nl.inl.impact.ner.io.FileHandler;
import nl.inl.impact.ner.matcher.MatcherFactory;
import nl.inl.impact.ner.matcher.NEInfo;
import nl.inl.impact.ner.stanfordplus.ImpactCRFClassifier;
import nl.inl.impact.ner.utils.NERTStringUtils;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;

public class Extractor {
	
	JPanel extractorPanel = new JPanel();
	ExtractorPanel panel = new ExtractorPanel();
	FileHandler filehandler = new FileHandler();
	
	static MatcherFactory mfExtract = new MatcherFactory();
	SetStatus setStat = new SetStatus();
	
	private GridBagConstraints extractPanelConstraints = null;
	
	private JPanel extractorButtonPanel1 = new JPanel();
	private JButton loadTextButton = new JButton("Load File");
	private JButton loadClassifierButton = new JButton("Load Classifier");
	private JButton exportButton = new JButton("Export");
	
	private JPanel extractPanel; // testing-tab
	private JLabel extractingLogoLabel;
	private JLabel extractIntroTextLabel = null;
	private JEditorPane mainTextEditorPane = new JEditorPane();
	private JScrollPane mainTextScrollPane;
	private JLabel mainTextEditorPaneLabel;

	private DefaultListModel NEListModel = new DefaultListModel();
	private JList NEList = new JList(NEListModel);
	private JScrollPane NEListScrollPane;
	private JLabel NEListLabel;

	private DefaultListModel variantListModel = new DefaultListModel();
	private JList variantList = new JList(variantListModel);
	private JScrollPane variantListScrollPane;
	
	private JLabel variantListLabel;

	private JPanel buttonNavPanel = new JPanel();
	private JButton nextButton = new JButton(">");
	private JButton previousButton  = new JButton("<");

	private JPanel buttonPanel2 = new JPanel();
	private JButton extractButton = new JButton("Extract Named Entities");
	private JButton clearButton = new JButton("Clear");
	
	JLabel typeChoiceLabel = new JLabel("Highlight:");
	JCheckBox typeLOC = new JCheckBox("LOC", true);
	JCheckBox typeORG = new JCheckBox("ORG", true);
	JCheckBox typePER = new JCheckBox("PER", true);
	JPanel typeChoicePanel = new JPanel();
	
	

	boolean storeNEPositions = true;
	
	private static JLabel currentClassifierLabel;
	public static JLabel currentStatusLabel;
	
	FileHandler fileHandler = new FileHandler();
	// variables for extracting-panel
	String labeledText = "";
	private int selectedNEIndex = 0;
	
	private String initText = "Dit is Cleveland en dat is de Regering. Hallo Parys. Dit is Kleveland.";
	
	private ArrayList<String> previouslySelectedNEs = new ArrayList<String>();
	private String previouslySelectedNETag = null;
	public TreeMap<String, String> extractedNETM = new TreeMap<String, String>(
			String.CASE_INSENSITIVE_ORDER);
	public HashMap<String, String> selectedNEPosHM = new HashMap<String, String>();
	public ArrayList<Integer> selectedNEPosList = new ArrayList<Integer>();
	public ArrayList<Integer> previouslySelectedNEPosList = new ArrayList<Integer>();
	private static HashMap<String, String> svPropsHM = new HashMap<String, String>();
	public static HashMap<Integer, String> labeledSentences;
	
	private AbstractSequenceClassifier classifier;
	private JToolBar tagPanel;
	private HashMap<String, Color> tagToColorMap;
	private MutableAttributeSet defaultAttrSet = new SimpleAttributeSet();
	private ActionListener actor = new ActionPerformer();
	private File loadedFile;
	private String taggedContents = null;
	private String htmlContents = null;
	private String tagPattern = "";
	
	private String[] tgAndWd = new String[2];
	private ArrayList<String> currentVariants = new ArrayList<String>();
	
	public JPanel buildExtractorPanel() {
		
		extractPanel = new JPanel(new GridBagLayout());
		//extractPanelConstraints = new GridBagConstraints();
		extractPanel.setBackground(panel.bgColorPanel);
		
		// Row 0: logo, and status: row showing the current loaded classifier, 
		// a jlabel that shows the current status of the NERT
		extractingLogoLabel = panel.createLogoLabel(extractingLogoLabel,extractPanel);
		
		extractorButtonPanel1 = panel.createExtractorButtonPanel1(extractorButtonPanel1, loadTextButton, loadClassifierButton, exportButton, extractPanel);
		loadTextButton.addActionListener(actor);
		loadClassifierButton.addActionListener(actor);
		exportButton.addActionListener(actor);
		
		currentClassifierLabel = panel.createCurrentClassifierLabel(currentClassifierLabel, extractPanel);
		currentStatusLabel = panel.createCurrentStatusLabel(currentStatusLabel, extractPanel);
		

		// Row 2: labels for the textfields
		mainTextEditorPaneLabel = panel.createMainTextEditorPaneLabel(mainTextEditorPaneLabel, extractPanel);
		NEListLabel = panel.createNEListLabel(NEListLabel, extractPanel);	
		variantListLabel = panel.createVariantListLabel(variantListLabel, extractPanel);
		
		
		// Row 3: textfield for the the text from which NE's will be extracted
		mainTextScrollPane = panel.createMainTextEditorScrollPane(mainTextEditorPane, mainTextScrollPane, extractPanel);
		mainTextEditorPane.addKeyListener(new InputListener());
		mainTextEditorPane.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent ce) {
				System.err.println("caret listener: " + ce.toString() + " pos:" + ce.getMark());
			}
		});
		StyleConstants.setFontFamily(defaultAttrSet, "Lucida Sans");
		panel.setInitText(mainTextEditorPane, initText);
	
		//Row 3: the list with all extracted NE's
		NEListScrollPane = panel.createNEListScrollPane(NEListScrollPane, NEList, NEListModel, extractPanel);
		MouseListener mouselistener = new MouseAdapter() {
			public void mouseClicked(MouseEvent mouseEvent) {
				JList theList = (JList) mouseEvent.getSource();
				if (mouseEvent.getClickCount() == 1) {
					int index = theList.locationToIndex(mouseEvent.getPoint());
					NEListMouseSelectionHandler(index, theList.getModel().getElementAt(index).toString());
				}
			}
		};
		NEList.addMouseListener(mouselistener);

		KeyListener keylistener = new KeyListener() {
			public void keyPressed(KeyEvent e) {dumpInfo("Pressed", e);}
			//@Override
			public void keyReleased(KeyEvent e) {dumpInfo("Released", e);}
			//@Override
			public void keyTyped(KeyEvent e) {dumpInfo("Typed", e);}

			private void dumpInfo(String s, KeyEvent e) {
				// not sure if this works for all systems - comparing "code" to
				// "enter/return"
				int code = e.getKeyCode();
				if (KeyEvent.getKeyText(code).equals("Enter") || KeyEvent.getKeyText(code).equals("Return")) {
					JList theList = (JList) e.getSource();
					int index = theList.getSelectedIndex();
					Object o = theList.getModel().getElementAt(index);
					NEListKeySelectionHandler(o.toString());
				}
			}
		};
		NEList.addKeyListener(keylistener);

		// Row 3: the variant list: you can't select them, so no mouse/action listener
		variantListScrollPane = panel.createVariantListScrollPane(variantListScrollPane, variantList, extractPanel);
	
		// Row 4: buttons for extract and clear
		buttonPanel2 = panel.createButtonPanel2(buttonPanel2, extractButton, clearButton, extractPanel);
		extractButton.addActionListener(actor);
		clearButton.addActionListener(actor);
		
		// Row 4: buttons for navigating through the NE's
		buttonNavPanel = panel.createButtonNavPanel(buttonNavPanel, previousButton, nextButton, extractPanel);
		nextButton.addActionListener(actor);
		previousButton.addActionListener(actor);
		
		// Row 4: checkboxes for the highlight of types
		typeChoicePanel = panel.createTypeChoicePanel(typeChoicePanel, typeChoiceLabel, typeLOC, typeORG, typePER, extractPanel);
		typeLOC.addActionListener(actor);
		typeORG.addActionListener(actor);
		typePER.addActionListener(actor);
			
	
		return extractPanel;
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
	
	
	/* 	This method is called when a user has single-clicked on an NE in the NEList.
	 * 
	 */
	
	private void NEListMouseSelectionHandler(int index, String entry){
		if (index >= 0) {
			System.err.println("Single-clicked on: " + entry);
			
			//Deal with selected NE and repaint the main text
			//field, with each occurrence of this word highlighted.
			tgAndWd = NERTStringUtils.splitListEntry(entry, false); // split the clicked word
			// into tag, word
			DefaultStyledDocument doc = (DefaultStyledDocument) mainTextEditorPane
					.getDocument();
			fillVariantList();
			setNEPositionsAndScrollButtons();
			// repaint the textfield and highlight word wd with tag
			// tg
			updateTextFieldForXML(doc);
		}
	}
	
	private void NEListKeySelectionHandler(String entry) {
		System.err.println("Pressed enter/return on: " + entry);
		tgAndWd = NERTStringUtils.splitListEntry(entry, false); // split the clicked word into
		// tag, word
		DefaultStyledDocument doc = (DefaultStyledDocument) mainTextEditorPane
				.getDocument();
		fillVariantList();
		setNEPositionsAndScrollButtons();
		// repaint the textfield and highlight word wd with tag tg
		updateTextFieldForXML(doc);
	}
	
	public void fillVariantList() {
		// fills the variant list for word wd (which is global)

		/*
		 * This method is called when the user single clicks on a word in the
		 * word list (list1), and fills the variant list (list2) with the proper
		 * variants AND the given variants
		 */

		// 'word' has the wrong shape, change it..
		// String w1 = word.substring(0, 3);
		// String w2 = word.substring(4, word.length());
		String word = tgAndWd[1] + tgAndWd[0];
		currentVariants.clear();
		variantListModel.clear();
		// doesn't work:
		// variantListScrollPane.setName("Variants of '"+word+"'");
		// for safety: check if word exists in NETM first
		if (mfExtract.NETM.containsKey(word)) {
			System.err.println("NE " + word + " heeft de volgende varianten: ");
			Collection c = mfExtract.NETM.get(word);
			Iterator d = c.iterator();
			while (d.hasNext()) {
				NEInfo e = (NEInfo) d.next();

				/*
				 * //'given' variants for(Iterator<String> it =
				 * e.givenVariants.keySet().iterator(); it.hasNext(); ){ String
				 * givenVar = it.next(); int freq =
				 * e.givenVariants.get(givenVar); String wholeString = "";
				 * 
				 * if(showFreq.isSelected()){ wholeString = "(" + freq + ") "; }
				 * 
				 * wholeString += givenVar; if(showVariantInfo.isSelected()){
				 * wholeString += " (given)"; }
				 * variantListModel.addElement(wholeString); }
				 */
				// 'standard' variants
				for (Iterator<String> it = e.variants.keySet().iterator(); it
						.hasNext();) {
					String var = it.next();
					System.err.println("\t" + var);
					String[] variantInfo = e.variants.get(var).split(";");
					// jaccard, which is what we want here, is under index==1
					// (jac=2)
					String j = variantInfo[1].substring(4);
					// System.err.println("variantInfo[1]: "+variantInfo[1]+" jac="+j);

					int jacSim = Integer.parseInt(variantInfo[1].substring(4));
					// int freq =
					// Integer.parseInt(e.variants.get(var).substring(2,
					// freqindex));
					// int freq = e.variants.get(var);

					// if(jacSim>=simThreshold){
					// System.err.println("WORD: "+word+" VARIANT: "+showVar);
					String wholeString = "";
					/*
					 * if(showFreq.isSelected()){ System.err.println(var);
					 * Collection ca = mf.NETM.get(var); Iterator da =
					 * ca.iterator(); int freq = 0; while(da.hasNext()){ NEInfo
					 * ea = (NEInfo) da.next(); freq = ea.frequency; }
					 * wholeString = "(" + String.valueOf(freq) + ") "; }
					 */
					wholeString += var.substring(0, var.length() - 3);
					System.err.println("Adding " + wholeString
							+ " to currentVariants");
					currentVariants.add(wholeString);

					/*
					 * if(showVariantInfo.isSelected()){ wholeString +=
					 * " ("+e.variants.get(var)+")"; }
					 */
					// String showVar = var.substring(0, var.length()-3);
					variantListModel.addElement(wholeString);
				}
			}
		}
	}
	
	private class ActionPerformer implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			String com = e.getActionCommand();

			if (src == loadClassifierButton) {
				File file = fileHandler.getFile(true);
				if (file != null) {
					loadClassifier(file);
				}
			}
			
			if (src == loadTextButton) {
				File file = fileHandler.getFile(true);
				if (file != null) {
					loadFileToTextField(file);
				}
			}
			
			if (src == exportButton) {
				String s = mainTextEditorPane.getText();
				fileHandler.printToFile(fileHandler.getFile(false), s);
			}
			
			if (src == clearButton) {
				clearExtractPanel();
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
			} else if (com.equals("Extract Named Entities")) {
				// first empty all relevant HMs and textfields
				// labeledSentences.clear();
				// ImpactCRFClassifier.labeledSentences.clear(); // TODO FIX THIS....
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
				setStat.setStatus("Extracting NE's ...", currentStatusLabel);
				extract(mainTextEditorPane, labeledText, classifier);
				//saveTaggedAs.setEnabled(true);
				setStat.setStatus("Creating NE-list ...", currentStatusLabel);
				// done extracting. Now it's time to setup the NE-list with references
				// to the main text and to do variant matching
				fillNEList();
				initVariantMatching();
				setStat.setStatus("Ready.", currentStatusLabel);
				mainTextEditorPane.setCaretPosition(0);
				NEList.requestFocus();
				NEList.ensureIndexIsVisible(0);

			} 
		}
	}
	
	
	
	public void loadFileToTextField(File f){
		
		/*	User has selected file f, put it in the textfield. */
		String s = fileHandler.readFile(f);
		//clearExtractPanel();
		
		Document doc = new DefaultStyledDocument();
		mainTextEditorPane.setDocument(doc);
		try {
			doc.insertString(0, s, defaultAttrSet);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
		
		mainTextEditorPane.revalidate();
		mainTextEditorPane.repaint();
		
	}
	
	public void clearExtractPanel() {

		// invoked when user has clicked on 'clear' button
		// clears all text fields in the extractPanel

		NEListModel.clear();
		variantListModel.clear();
		previousButton.setEnabled(false);
		nextButton.setEnabled(false);
		
		previouslySelectedNEs.clear();
		previouslySelectedNETag = null;
		System.err.println("testje A");
		tgAndWd[1] = "";
		tgAndWd[0] = "";
		System.err.println("testje B ");
		mainTextEditorPane.setContentType("text/rtf");
		Document doc = new DefaultStyledDocument();
		mainTextEditorPane.setDocument(doc);
		// defaultAttrSet =
		// ((StyledEditorKit)mainTextEditorPane.getEditorKit()).getInputAttributes();
		// StyleConstants.setFontFamily(defaultAttrSet, "Lucinda Sans Unicode");
		System.err.println("attr: " + defaultAttrSet);

		try {
			doc.insertString(0, " ", defaultAttrSet);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}

		mainTextEditorPane.setEditable(true);
		mainTextEditorPane.revalidate();
		mainTextEditorPane.repaint();

		//saveUntagged.setEnabled(false);
		//saveTaggedAs.setEnabled(false);

		taggedContents = null;
		htmlContents = null;
		loadedFile = null;

	}

	
	
	
	
	
	public void extract(JEditorPane pane, String labeledText, AbstractSequenceClassifier classifier) {

		/*
		 * This is the main method that controls the extraction of NE's from the
		 * text in the editorPane. TODO make it work for html, or disable this
		 * option TODO make it work for BIO-format.
		 */

		System.err.println("Starting extracting.");
		//System.err.println("content type: "+ pane.getContentType());
		if (!pane.getContentType().equals("text/html")) {
			DefaultStyledDocument doc = (DefaultStyledDocument) pane
					.getDocument();
			String text = null;
			try {
				text = doc.getText(0, doc.getLength());
			} catch (Exception e) {
				e.printStackTrace();
			}
			// labeledText holds the entire text in which NE's are marked with
			// XML-tags
			// like 'Hello <B-LOC>Cleveland</B-LOC>!'
			// System.err.println("unlabeled text: "+text);
			text = NERTStringUtils.removeEuroSign(text);
			labeledText = classifier.classifyWithInlineXML(text);
			
			
			
			System.err.println("Done classifying. Starting indexing NE's and repainting textfield...");

			// labeledText = putBackEuroSign(labeledText);
			// System.err.println("Succesfully dealt with eurosigns.");

			System.err.println("labeledText 1: "+labeledText);
			// labeledText = removeEuroSign(labeledText);
			taggedContents = labeledText;
			System.err.println("labeledText 2: "+labeledText);
			Set<String> tags = classifier.labels();
			// TODO reduce the Set tags from B- and I- to only the type.

			String background = classifier.backgroundSymbol();
			tagPattern = "";
			// there are B- and I- tags, we want the bare tags only (PER, LOC,
			// ORG).
			for (String tag : tags) {
				if (tag.equals(background)) {
					continue;
				}
				if (tag.substring(0, 1).equals("B")) {
					if (tagPattern.length() > 0) {
						tagPattern += "|";
					}
					tagPattern += tag.substring(2);
				}
			}
			// System.err.println(tagPattern);

			// labeledText holds the entire text with both B- and I-tags.
			// First convert this String and join all adjacent B- and I-tags
			labeledText = NERTStringUtils.joinBAndITags(labeledText);

			// method below prints the text on the screen - it converts
			// the labeledtext to finaltext without labels but with colors
			updateTextFieldForXMLAfterExtraction(doc);

		} 
		/*
		else {

			// TODO: this part for HTML will need some changes for it to work
			// properly

			String untaggedContents = pane.getText();
			if (untaggedContents == null) {
				untaggedContents = "";
			}
			taggedContents = classifier.classifyWithInlineXML(untaggedContents);

			Set<String> tags = classifier.labels();
			String background = classifier.backgroundSymbol();
			String tagPattern = "";
			for (String tag : tags) {
				if (tag.equals(background)) {
					continue;
				}
				if (tagPattern.length() > 0) {
					tagPattern += "|";
				}
				tagPattern += tag;
			}

			Pattern startPattern = Pattern.compile("<(" + tagPattern + ")>");
			Pattern endPattern = Pattern.compile("</(" + tagPattern + ")>");

			String finalText = taggedContents;

			Matcher m = startPattern.matcher(finalText);
			while (m.find()) {
				String tag = m.group(1);
				String color = colorToHTML(tagToColorMap.get(tag));
				String newTag = "<span style=\"background-color: " + color
						+ "; color: white\">";
				finalText = m.replaceFirst(newTag);
				int start = m.start() + newTag.length();
				Matcher m1 = endPattern.matcher(finalText);
				m1.find(m.end());
				String entity = finalText.substring(start, m1.start());
				System.err.println(tag + ": " + entity);
				finalText = m1.replaceFirst("</span>");
				m = startPattern.matcher(finalText);
			}
			// System.err.println(finalText);
			pane.setText(finalText);
			pane.revalidate();
			pane.repaint();

			// System.err.println(finalText);
		}
		*/
		
		
	}
	
	
	/**
	 * Load a classifier from a file or the default. The default is specified by
	 * passing in <code>null</code>.
	 */
		public void loadClassifier(File file) {
			setStat.setStatus("Loading classifier ...", currentStatusLabel);
			try {
				if (file != null) {
					classifier = ImpactCRFClassifier.getClassifier(file);
					setStat.setStatus("Ready.", currentStatusLabel);
					currentClassifierLabel.setText("Classifier: " + file.getName());
					currentClassifierLabel.paintImmediately(currentClassifierLabel
							.getVisibleRect());
					System.err.println("Loaded classifier " + file.getName());
				}
				/*
				 * else { // default classifier in jar classifier =
				 * ImpactCRFClassifier.getDefaultClassifier(); }
				 */
			} catch (Throwable e) {
				// we catch Throwable, since we'd also like to be able to get an
				// OutOfMemoryError
				String message;
				if (file != null) {
					message = "Error loading CRF: " + file.getAbsolutePath();
				} else {
					message = "Error loading default CRF";
				}
				System.err.println(message);
				String title = "CRF Load Error";
				String msg = e.toString();
				if (msg != null) {
					message += "\n" + msg;
				}
				fileHandler.displayError(title, message);
				return;
			}
			removeTags();
			//FL 18-10 uitgezet
			//buildTagPanel();
			
			// buildExtractButton();
			extractButton.setEnabled(true);
			//extract.setEnabled(true);		//menu item: get rid of this?
			setStat.setStatus("Ready.", currentStatusLabel);
		}

		private void removeTags() {
			if (mainTextEditorPane.getContentType().equals("text/html")) {
				if (htmlContents != null) {
					mainTextEditorPane.setText(htmlContents);
				}
				mainTextEditorPane.revalidate();
				mainTextEditorPane.repaint();
			} else {
				DefaultStyledDocument doc = (DefaultStyledDocument) mainTextEditorPane
						.getDocument();
				SimpleAttributeSet attr = new SimpleAttributeSet();
				StyleConstants.setForeground(attr, Color.BLACK);
				StyleConstants.setBackground(attr, Color.WHITE);
				doc.setCharacterAttributes(0, doc.getLength(), attr, false);
			}
			//saveTaggedAs.setEnabled(false);
		}
		
		
		public void moveCaretToNE() {
			String pos = selectedNEPosHM.get(Integer.toString(selectedNEIndex));
			int p = Integer.parseInt(pos);
			System.err.println("Move caret to caret position " + p);
			mainTextEditorPane.setCaretPosition(Integer.parseInt(pos));
			System.err.println("mark of caret: "
					+ mainTextEditorPane.getCaret().getMark());
			// System.err.println("visibility of caret: "+mainTextEditorPane.getCaret().isVisible());
			mainTextEditorPane.getCaret().setVisible(true);
			// mainTextEditorPane.setCaretPosition(position)
		}
		
		public void fillNEList() {
			NEListModel.clear();
			for (String key : extractedNETM.keySet()) {
				NEListModel.addElement(key);
				String ind = extractedNETM.get(key);
				System.err.println(key + "=>" + ind);
			}
		}
		
		/*
		 * This method is called from the extract() method, so directly after
		 * NE-extraction has finished. It initiates the variant matching process and
		 * calls the method to fill the variant list.
		 */

		public void initVariantMatching() {
			setStat.setStatus("Collecting NE-variants ...", currentStatusLabel);

			System.err.println("Starting NE variant matching");
			mfExtract.totalCleanup();
			if(loadedFile == null){mfExtract.loadNEsFromList(extractedNETM);}
			if(loadedFile != null){mfExtract.loadNEsFromList(extractedNETM, loadedFile.getName());}
			mfExtract.initNEMatcher(true, true, true); // flags for possible skipping of
			// LOC/ORG/PER/LOC

			// a print just for testing:
			for (String word : mfExtract.NETM.keySet()) {
				Collection c = mfExtract.NETM.get(word);
				Iterator d = c.iterator();
				while (d.hasNext()) {
					NEInfo e = (NEInfo) d.next();
					// 'standard' variants
					for (Iterator<String> it = e.variants.keySet().iterator(); it
							.hasNext();) {
						String var = it.next();
						System.err.println("WORD=" + word + " VARIANTS=" + var);
					}
				}
			}
		}

		
		public void setNEPositionsAndScrollButtons() {

			/*
			 * This method is called when a user has clicked on a NE in the NE-list.
			 * It collects the positions of all occurrences of this NE *and* its
			 * variants, and sets the scroll buttons.
			 */
			selectedNEPosList.clear();
			selectedNEIndex = 0;

			// move the editorpane to the first location stored as a value of
			// key 'tg+" "+wd' in the extractedNETM
			String pos = extractedNETM.get(tgAndWd[0] + " " + tgAndWd[1]);
			int sep = pos.indexOf(",");
			int thispos = 0;
			if (sep >= 0) {
				thispos = Integer.parseInt(pos.substring(0, sep));
			} else {
				thispos = Integer.parseInt(pos);
			}
			System.err.println(tgAndWd[1] + " pos: " + pos + " thispos: " + thispos);

			// fillVariantList();

			// store all the positions in a List
			selectedNEPosHM.clear();
			String[] allPos = pos.split(",");
			for (int i = 0; i < allPos.length; i++) {
				selectedNEPosList.add(Integer.parseInt(allPos[i]));
			}
			// we also want the positions of all the variants, so
			// add those too.
			Iterator itr = currentVariants.iterator();
			while (itr.hasNext()) {
				String a = (String) itr.next();
				System.err.println("variant:" + a);
				String val = extractedNETM.get(tgAndWd[0] + " " + a);
				System.err.println("\tpositions: " + val);
				if (!val.equals("")) {
					String[] p = val.split(",");
					for (int i = 0; i < p.length; i++) {
						int po = Integer.parseInt(p[i]);
						if (po < thispos) {
							thispos = po;
						}
						selectedNEPosList.add(po);
						// selectedNEPosHM.put(Integer.toString(i), allPos[i]);
					}
				}
			}
			// all values are now stored in selectedNEPosList, do a sorted
			// iteration over this list and copy them to selectedNEPosHM,
			// which uses an index - this makes jumping from one NE to
			// the other easier
			Collections.sort(selectedNEPosList);
			Iterator<Integer> it = selectedNEPosList.iterator();
			int c = 0;
			while (it.hasNext()) {
				String d = it.next().toString();
				selectedNEPosHM.put(Integer.toString(c), d);
				if (c == 0) {
					mainTextEditorPane.setCaretPosition(Integer.parseInt(d));
				}
				c++;
			}

			previousButton.setEnabled(false);
			if (selectedNEPosHM.size() > 1) {
				nextButton.setEnabled(true);
			} else {
				nextButton.setEnabled(false);
			}
			System.err.println("number of occurrences of this NE: "
					+ selectedNEPosList.size());
			mainTextEditorPane.setCaretPosition(thispos);
		}

		private void storeNEAndItsIndex(String namedentity, int startIndex,
				String tag) {
			/*
			 * Add NE to TM. If it already exists, get found positions and add the
			 * new one.
			 */

			if (!extractedNETM.containsKey((tag + " " + namedentity))) {
				// System.err.println("A. woord="+finalText.substring(start, end)+
				// " index="+index);
				extractedNETM.put(tag + " " + namedentity, Integer
						.toString(startIndex));
			} else {
				String indices = extractedNETM.get(tag + " " + namedentity);
				// System.err.println("B. woord="+finalText.substring(start, end)+
				// " bestaande index="+indices);
				indices += "," + Integer.toString(startIndex);
				// System.err.println("\tB. nieuwe index="+indices);
				// System.err.println("word: "+finalText.substring(start,
				// end)+" tag: "+tag);
				extractedNETM.put(tag + " " + namedentity, indices);
			}
		}

		private void updateTextFieldForXMLAfterExtraction(DefaultStyledDocument doc) {

			/*
			 * This is the main method for the painting of the editorPane that holds
			 * the main text after extraction. It is called only once.
			 */

			setStat.setStatus("Repainting textfield ...", currentStatusLabel);
			// TODO reset the standard font and font colors so we don't stuck
			// with some silly copy-paste font and color from the user

			int c = 0;
			String finalText = labeledText;
			// System.err.println("a) "+finalText);
			// here, the labels in the String labeledText are removed
			// and replaced by colored labels that go in the String
			// finalText (NB these colors are not visible in stErr)

			/*
			 * Instead of a reg. ex., we try Java's 'indexOf' method to track all
			 * NE's, store their positions and remove the tags.
			 * 
			 * NB the tricky part is to locate the first tag because it can either
			 * be LOC, PER or ORG.
			 */

			int startLoopFromIndex = 0;
			int[] startIndex = new int[3]; // 0: LOC, 1: ORG, 2: PER

			String startIndexLOC = "<LOC>";
			String startIndexORG = "<ORG>";
			String startIndexPER = "<PER>";

			while (startLoopFromIndex < finalText.length()) {
				System.err.println("Start while loop. startloopfromIndex = "+ startLoopFromIndex);
				// find first occurrence of one of the three tags
				startIndex[0] = finalText
						.indexOf(startIndexLOC, startLoopFromIndex);
				startIndex[1] = finalText
						.indexOf(startIndexORG, startLoopFromIndex);
				startIndex[2] = finalText
						.indexOf(startIndexPER, startLoopFromIndex);
				int minIndex = NERTStringUtils.getMinIndex(startIndex);
				System.err.println("Found tags at pos " + startIndex[0] + " "+ startIndex[1] + " " + startIndex[2]);
				if(minIndex >= 0){
				//if (startIndex[minIndex] >= 0) {

					String startTag = "<LOC>";
					if (minIndex == 1) {startTag = "<ORG>";}
					if (minIndex == 2) {startTag = "<PER>";}
					System.err.println("The min index is " + startIndex[minIndex]
							+ " with tag " + startTag);
					// the first tag is 'startTag' and occurs at index
					// 'startIndex[minIndex]'
					// now that we now the type of the first occurring start tag,
					// we can set the first occurring end tag.
					String endTag = "</LOC>";
					if (minIndex == 1) {
						endTag = "</ORG>";
					}
					if (minIndex == 2) {
						endTag = "</PER>";
					}
					System.err.println("Endtag: " + endTag);

					// we replace the tag with void ("")
					finalText = finalText.replaceFirst(startTag, "");

					// we now look for the first occurrence of the end tag
					int endIndex = finalText.indexOf(endTag);

					// we replace the endtag with void ("")
					finalText = finalText.replaceFirst(endTag, "");

					// highlight this NE
					c = highlightNEs(doc, c, finalText, startIndex[minIndex],
							endIndex, startTag.substring(1, startTag.length() - 1));

					// Now that we know that start and end index and the type, we
					// can store the NE
					System.err.println("storing NE "
							+ finalText.substring(startIndex[minIndex], endIndex));
					storeNEAndItsIndex(finalText.substring(startIndex[minIndex],
							endIndex), startIndex[minIndex], startTag.substring(1,
							startTag.length() - 1));
					startLoopFromIndex = endIndex;
					System.err.println("New value of startloopFromIndex = "
							+ startLoopFromIndex);
				} else {
					// No more NE's were found, so we're stopping the while loop
					startLoopFromIndex = finalText.length();
					System.err.println("No more tags found. startloopFromIndex = "
							+ startLoopFromIndex);
				}
				// now repeat the loop, starting at the index of 'endIndex'
			}

			setStat.setStatus("Ready.", currentStatusLabel);
		}

		private void updateTextFieldForXMLAfterExtraction_OUD(
				DefaultStyledDocument doc) {

			/*
			 * This is the main method for the painting of the editorPane that holds
			 * the main text after extraction. It is called only once.
			 */

			setStat.setStatus("Repainting textfield ...", currentStatusLabel);
			// TODO reset the standard font and font colors so we don't stuck
			// with some silly copy-paste font and color from the user

			Pattern startPattern = Pattern.compile("<(" + tagPattern + ")>");
			Pattern endPattern = Pattern.compile("</(" + tagPattern + ")>");
			// System.err.println("thisPattern: "+thisPattern);

			// this variable tracks the number of NE's that should be
			// highlighted (after the user has clicked on it in the NE-list)
			// for specialMarkingNum = 0, it makes only this word
			// visible in the mainTextEditorPane
			int caretPos = 0;
			int c = 0;
			int counter = 0;

			String finalText = labeledText;
			int minimalIndex = 0;
			// System.err.println("a) "+finalText);
			// here, the labels in the String labeledText are removed
			// and replaced by colored labels that go in the String
			// finalText (NB these colors are not visible in stErr)
			Matcher m = startPattern.matcher(finalText);
			while (m.find()) {
				int start = m.start();
				finalText = m.replaceFirst("");
				// System.err.println("b) "+finalText);
				m = endPattern.matcher(finalText);
				if (m.find()) {
					int end = m.start();
					String tag = m.group(1);
					finalText = m.replaceFirst("");
					// System.err.println("c) "+finalText);
					c = highlightNEs(doc, c, finalText, start, end, tag);

					// now store the position of this particular NE in the String
					// labeledText
					int index = labeledText.indexOf(
							finalText.substring(start, end), minimalIndex);
					// the found index localizes the word: subtract the length of
					// the tag
					// but we first get the correct minimal index for the next word
					// by
					// adding the length of the word + the length of the tag + 3
					// (since </ and > are missing from the tag)
					// NB we don't need to know the end index
					minimalIndex = index + finalText.substring(start, end).length()
							+ tag.length() + 3;
					index = index - (tag.length() + 2);
					index = index - (counter * 11); // this is necessary to get the
					// correct caret position
					// 11 = length of <TAG></TAG>, times the
					// number of past tags

					// System.err.println(finalText.substring(start,
					// end)+" index: "+index+" minimalIndex: "+minimalIndex);
					// NB: since B- and I- tags come separately, we have to add
					// words tagged
					// with I- to the previous word
					if (storeNEPositions) {
						// after extraction, store the positions of all the NE's.
						// this only has to be done the first time.
						storeIndices(finalText, start, end, tag, index);
					}
					// System.err.println("c) "+finalText);
					counter++;
				} else {
					// print error message
				}
				m = startPattern.matcher(finalText);
			}
			// mainTextEditorPane.revalidate();
			// mainTextEditorPane.repaint();
			storeNEPositions = false;
			setStat.setStatus("Ready.", currentStatusLabel);
		}

		public void updateTextFieldForXML(DefaultStyledDocument doc) {

			/*
			 * Repaint the text field after an NE has been selected. -highlight the
			 * selected NE and its variants -give the previously highlighted NE's
			 * and its variants back its original highlights (or mark them 'blank'
			 * when the user has turn off the highlights for this tag in the meantime)
			 */

			setStat.setStatus("Repainting textfield ...", currentStatusLabel);
			// TODO reset the standard font and font colors so we don't stuck
			// with some silly copy-paste font and color from the user

			// get the doctext
			String doctext = null;
			try {
				doctext = doc.getText(0, doc.getLength());
			} catch (Exception e) {
				e.printStackTrace();
			}

			Iterator it = previouslySelectedNEs.iterator();
			while (it.hasNext()) {
				repaintOriginalMarkings(doc, doctext, (String) it.next());
			}
			previouslySelectedNEs.clear();

			// find all occurrences of the NE and its variants, and
			// repaint them. Store the currently selected NE and its
			// variants in an arraylist.

			repaint(doc, doctext, tgAndWd[1]);
			previouslySelectedNEs.add(tgAndWd[1]);

			it = currentVariants.iterator();
			while (it.hasNext()) {
				String var = (String) it.next();
				repaint(doc, doctext, var);
				previouslySelectedNEs.add(var);
			}
			previouslySelectedNETag = tgAndWd[0];
			previouslySelectedNEPosList.clear();

			Iterator iter = selectedNEPosList.iterator();
			while (iter.hasNext()) {
				previouslySelectedNEPosList.add((Integer) iter.next());
			}

			/*
			 * Pattern startPattern = Pattern.compile("<("+tagPattern+")>"); Pattern
			 * endPattern = Pattern.compile("</("+tagPattern+")>");
			 * //System.err.println("thisPattern: "+thisPattern);
			 * 
			 * //this variable tracks the number of NE's that should be
			 * //highlighted (after the user has clicked on it in the NE-list) //for
			 * specialMarkingNum = 0, it makes only this word //visible in the
			 * mainTextEditorPane int caretPos = 0; int c = 0; int counter = 0;
			 * 
			 * String finalText = labeledText; int minimalIndex = 0;
			 * //System.err.println("a) "+finalText); //here, the labels in the
			 * String labeledText are removed //and replaced by colored labels that
			 * go in the String //finalText (NB the colors are not visible in stErr)
			 * Matcher m = startPattern.matcher(finalText); while (m.find()) { int
			 * start = m.start(); finalText = m.replaceFirst("");
			 * //System.err.println("b) "+finalText); m =
			 * endPattern.matcher(finalText); if (m.find()) { int end = m.start();
			 * String tag = m.group(1); finalText = m.replaceFirst("");
			 * //System.err.println("c) "+finalText); c = highlightNEs(doc, c,
			 * finalText, start, end, tag);
			 * 
			 * //now store the position of this particular NE in the String
			 * labeledText int index =
			 * labeledText.indexOf(finalText.substring(start, end), minimalIndex);
			 * //the found index localizes the word: subtract the length of the tag
			 * //but we first get the correct minimal index for the next word by
			 * //adding the length of the word + the length of the tag + 3 //(since
			 * </ and > are missing from the tag) //NB we don't need to know the end
			 * index minimalIndex = index + finalText.substring(start, end).length()
			 * + tag.length() + 3; index = index - (tag.length()+2); index = index -
			 * (counter*11); //this is necessary to get the correct caret position
			 * //11 = length of <TAG></TAG>, times the //number of past tags
			 * 
			 * //System.err.println(finalText.substring(start,
			 * end)+" index: "+index+" minimalIndex: "+minimalIndex); //NB: since B-
			 * and I- tags come separately, we have to add words tagged //with I- to
			 * the previous word if(storeNEPositions){ //after extraction, store the
			 * positions of all the NE's. //this only has to be done the first time.
			 * storeIndices(finalText, start, end, tag, index); }
			 * //System.err.println("c) "+finalText); counter++; } else { // print
			 * error message } m = startPattern.matcher(finalText); }
			 */
			// mainTextEditorPane.revalidate();
			// mainTextEditorPane.repaint();
			storeNEPositions = false;
			setStat.setStatus("Ready.", currentStatusLabel);
		}

		private void repaintOriginalMarkings(DefaultStyledDocument doc,
				String doctext, String word) {
			System.err.println("repainting original markings.. tag = "
					+ previouslySelectedNETag);
			/*
			 * This method puts back the 'original' background color of the NE and
			 * its variants that were previously selected. Otherwise, they would
			 * remain highlighted after repainting. this method is very similar to
			 * the repaint()-method
			 */

			if(previouslySelectedNETag != null){
				
				int startpos = 0;
				//get the proper attSet, depending on whether the previously highlighted words
				//need marking or not
				AttributeSet attSet = getAttributeSet(previouslySelectedNETag);
				if( (previouslySelectedNETag.equals("LOC") && !typeLOC.isSelected()) || (previouslySelectedNETag.equals("PER") && !typePER.isSelected()) || (previouslySelectedNETag.equals("ORG") && !typeORG.isSelected()) ){
					System.err.println("relevant tag: "+previouslySelectedNETag+" LOC:"+typeLOC.isSelected()+" ORG:"+typeORG.isSelected()+" PER:"+typePER.isSelected() );
					attSet = getAttributeSetForNoMarking();
				}
				int ind = 0;
				while ((ind > -1) && (startpos < doc.getLength())) {
					// System.err.println("Looking for word "+word
					// +" from position "+startpos);
					ind = doctext.indexOf(word, startpos);
					String thispos = String.valueOf(ind);
					// System.err.println("index of word: "+ind);
					if ((previouslySelectedNEPosList.contains(ind)) && (ind != -1)) {
						try {
							// String entity = finalText.substring(start, end);
							// System.err.println("repainting... ind="+a+" wd.length="+b);
							doc.setCharacterAttributes(ind, word.length(), attSet,
									false);
							mainTextEditorPane.revalidate();
							mainTextEditorPane.repaint();
						} catch (Exception ex) {
							ex.printStackTrace();
							System.exit(-1);
						}
					}
					startpos = ind + word.length();
				}
			}
		}

		private void repaint(DefaultStyledDocument doc, String doctext, String word) {

			/*
			 * This repaint method searches the doctext from the editorpane for
			 * occurrences of the selected NE or its variants, and repaints them
			 * with a special background color.
			 * 
			 * A check is needed to make sure that only actually tagged NE's are
			 * found, e.g. only those occurrences of 'Cleveland' should be
			 * highlighted that the NER has marked as NE. Therefore, a check is
			 * added that compares the found position of a candidate with the stored
			 * positions of the NE and its variants in the List selectedNEPosList.
			 */

			int startpos = 0;

			AttributeSet attSet = getAttributeSetForSpecialMarking();

			System.err.println("Word " + word + " and its "
					+ currentVariants.size() + " variants has "
					+ selectedNEPosList.size() + " occurrences.");
			if(word != null){
				int ind = 0;
				while ((ind > -1) && (startpos < doc.getLength())) {
					System.err.println("Looking for word " + word + " from position "
							+ startpos);
					ind = doctext.indexOf(word, startpos);
					System.err.println("index of word word: " + ind);
					if ((selectedNEPosList.contains(ind)) && (ind != -1)) {
						try {
							// String entity = finalText.substring(start, end);
							// System.err.println("repainting... ind="+a+" wd.length="+b);
							doc.setCharacterAttributes(ind, word.length(), attSet,
									false);
							mainTextEditorPane.revalidate();
							mainTextEditorPane.repaint();
						} catch (Exception ex) {
							ex.printStackTrace();
							System.exit(-1);
						}
					}
					startpos = ind + word.length();
				}
			}
		}

		public void repaintAfterTagHighlightChange() {
			/*
			 * This method repaints the main text after the user has selected or
			 * deselected one of the types (LOC, PER, ORG). This method is very
			 * similar to the repaint()-method
			 */

			DefaultStyledDocument doc = (DefaultStyledDocument) mainTextEditorPane
					.getDocument();
			// get the doctext
			String doctext = null;
			try {
				doctext = doc.getText(0, doc.getLength());
			} catch (Exception e) {
				e.printStackTrace();
			}
			int startpos = 0;
			AttributeSet attSet;
			Iterator it = extractedNETM.keySet().iterator();
			while (it.hasNext()) {
				boolean highlight = false;
				String s = (String) it.next();
				String tag = s.substring(0, 3);
				if ((tag.equals("LOC")) && (typeLOC.isSelected())) {
					highlight = true;
				}
				if ((tag.equals("ORG")) && (typeORG.isSelected())) {
					highlight = true;
				}
				if ((tag.equals("PER")) && (typePER.isSelected())) {
					highlight = true;
				}
				attSet = getAttributeSetForNoMarking();
				if (highlight) {
					System.err.println("w:" + s + " LOC: " + typeLOC.isSelected()
							+ " ORG: " + typeORG.isSelected() + " PER: "
							+ typePER.isSelected());
					attSet = getAttributeSet(tag);
				}
				String word = s.substring(4, s.length());
				String p = extractedNETM.get(s);
				String[] pos = p.split(",");
				ArrayList<Integer> posit = new ArrayList<Integer>();
				for (int i = 0; i < pos.length; i++) {
					posit.add(Integer.parseInt(pos[i]));
				}
				Iterator ite = posit.iterator();
				while (ite.hasNext()) {
					Integer start = (Integer) ite.next();
					try {
						// String entity = finalText.substring(start, end);
						// System.err.println("repainting... ind="+a+" wd.length="+b);
						doc.setCharacterAttributes(start, word.length(), attSet,
								false);
						mainTextEditorPane.revalidate();
						mainTextEditorPane.repaint();
					} catch (Exception ex) {
						ex.printStackTrace();
						System.exit(-1);
					}
				}
			}
			// repaint the main text to specially highlight the selected NE's
			// again..
			updateTextFieldForXML(doc);
		}

		private int highlightNEs(DefaultStyledDocument doc, int c,
				String finalText, int start, int end, String tag) {

			int caretPos;
			AttributeSet attSet;
			if ((tag.equals(tgAndWd[0]))
					&& (currentVariants.contains(finalText.substring(start, end)) || finalText
							.substring(start, end).equals(tgAndWd[1]))) {
				// System.err.println("ALHIER. "+finalText.substring(start,end));
				// selected word from the NE-list: give special marking
				attSet = getAttributeSetForSpecialMarking();
				// int ind = getNEIndex();
				if (selectedNEIndex == c) {
					caretPos = start;
					// System.err.println("caretpos = "+caretPos);
					c++;
				}
			} else {
				// regular marking
				if ((tag.equals("LOC") && typeLOC.isSelected())
						|| (tag.equals("ORG") && typeORG.isSelected())
						|| (tag.equals("PER") && typePER.isSelected())) {
					// System.err.println("tag="+tag+" LOC? "+typeLOC.isSelected()+" ORG? "+typeORG.isSelected()+" PER? "+typePER.isSelected());
					attSet = getAttributeSet(tag);
				} else {
					// apparently this is necessary to remove the existing colors...
					attSet = getAttributeSetForNoMarking();
				}
			}

			try {
				if (start < end) {
					String entity = finalText.substring(start, end);
					doc.setCharacterAttributes(start, entity.length(), attSet,
							false);
					mainTextEditorPane.revalidate();
					mainTextEditorPane.repaint();
				} else {
					System.err.println("ERROR. " + start + " " + end);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				System.exit(-1);
			}
			return c;
		}

		private void storeIndices(String finalText, int start, int end, String tag,
				int index) {

			/*
			 * Add NE to TM. If it already exists, get found positions and add the
			 * new one.
			 */

			if (!extractedNETM.containsKey((tag + " " + finalText.substring(start,
					end)))) {
				// System.err.println("A. woord="+finalText.substring(start, end)+
				// " index="+index);
				extractedNETM.put(tag + " " + finalText.substring(start, end),
						Integer.toString(index));
			} else {
				String indices = extractedNETM.get(tag + " "
						+ (finalText.substring(start, end)));
				// System.err.println("B. woord="+finalText.substring(start, end)+
				// " bestaande index="+indices);
				indices += "," + Integer.toString(index);
				// System.err.println("\tB. nieuwe index="+indices);
				// System.err.println("word: "+finalText.substring(start,
				// end)+" tag: "+tag);
				extractedNETM.put(tag + " " + (finalText.substring(start, end)),
						indices);
			}
		}
		
		
		/*
		 * Returns the position of the selected NE's Nth appearance, obtained from
		 * the current selectedNEIndex
		 */

		public int getNEIndex() {
			String[] pos = extractedNETM.get(tgAndWd[0] + " " + tgAndWd[1]).split(",");
			System.err.println("Woord " + tgAndWd[1] + " heeft " + pos.length
					+ " voorkomens.");
			System.err.println("SelectedIndex: " + selectedNEIndex + " pos="
					+ pos[selectedNEIndex]);
			return Integer.parseInt(pos[selectedNEIndex]);
		}


		public void openFile(File file) {
			//openURL(file.toURI().toString());
			loadedFile = file;
			//saveUntagged.setEnabled(true);
		}
		
	
	
		private void buildTagPanel() {

			if (tagPanel == null) {
				tagPanel = new JToolBar(SwingConstants.HORIZONTAL);
				tagPanel.setFloatable(false);
				// oorspronkelijk:
				// extractPanel.add(tagPanel, BorderLayout.EAST);
				extractPanelConstraints.fill = GridBagConstraints.NONE;
				extractPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
				panel.buildConstraints(extractPanelConstraints, 2, 1, 1, 1, 50, 10);
				/*
				 * d.gridx = 2; d.gridy = 1; d.gridheight = 1; d.gridwidth = 1;
				 * d.weightx = 50; d.weighty = 10;
				 */
				extractPanelConstraints.insets = new Insets(5, 5, 5, 5);
				// extractPanel.add(tagPanel, d);
				// frame.getContentPane().add(tagPanel, BorderLayout.EAST);
			} else {
				// tagPanel.removeAll();
			}

			if (classifier != null) {

				makeTagMaps();

				Set<String> tags = classifier.labels();
				String backgroundSymbol = classifier.backgroundSymbol();

				for (String tag : tags) {
					if (tag.equals(backgroundSymbol)) {
						continue;
					}
					Color color = tagToColorMap.get(tag);
					JButton b = new JButton(tag, new Colors.ColorIcon(color));
					// tagPanel.add(b);
				}
			}
			// tagPanel.revalidate();
			// tagPanel.repaint();
		}
		
		private void makeTagMaps() {

			Set<String> tags = classifier.labels();
			String backgroundSymbol = classifier.backgroundSymbol();
			int numColors = tags.size() - 1;
			Color[] colors = Colors.getNColors(numColors);
			tagToColorMap = new HashMap<String, Color>();

			int i = 0;
			for (String tag : tags) {
				if (tag.equals(backgroundSymbol)) {
					continue;
				}
				if (tagToColorMap.get(tag) != null) {
					continue;
				}

				tagToColorMap.put(tag, colors[i++]);
			}
		}

	
		

		private AttributeSet getAttributeSet(String tag) {
			MutableAttributeSet attr = new SimpleAttributeSet();
			// HM with tags and colors is made in makeTagMap()
			Color color = tagToColorMap.get("B-" + tag);
			System.err.println("getAttributeSet. tag: "+tag+" color="+color);
			StyleConstants.setBackground(attr, color);
			StyleConstants.setForeground(attr, Color.WHITE);
			return attr;
		}

		public AttributeSet getAttributeSetForSpecialMarking() {
			MutableAttributeSet attr = new SimpleAttributeSet();
			Color color = new Color(255, 220, 100); // mellow yellow
			StyleConstants.setBackground(attr, color);
			StyleConstants.setForeground(attr, Color.BLACK);
			return attr;
		}

		public AttributeSet getAttributeSetForNoMarking() {
			MutableAttributeSet attr = new SimpleAttributeSet();
			Color color = Color.white;
			StyleConstants.setBackground(attr, color);
			StyleConstants.setForeground(attr, Color.BLACK);
			return attr;
		}	
	

}
