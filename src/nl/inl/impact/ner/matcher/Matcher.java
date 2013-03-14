package nl.inl.impact.ner.matcher;


import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import nl.inl.impact.ner.gui.MatcherPanel;
import nl.inl.impact.ner.gui.MyTableModel;
import nl.inl.impact.ner.gui.SetStatus;
import nl.inl.impact.ner.io.FileHandler;
import nl.inl.impact.ner.utils.NERTStringUtils;

public class Matcher {
	
	JPanel matcherPanel = new JPanel();
	MatcherPanel panel = new MatcherPanel();
	
	private HashMap<String, Boolean> loadedFiles = new HashMap<String, Boolean>();
	SetStatus setStat = new SetStatus();
	FileHandler fileHandler = new FileHandler();
	static MatcherFactory mfMatch = new MatcherFactory();
	NEMatcher nematcher = new NEMatcher();

	private ActionListener actor = new ActionPerformer();

	private JLabel matcherLogoLabel;
	private JLabel matcherIntroTextLabel;

	private JTable matcherFileListTable;
	private MyTableModel matcherFileListTableModel;
	private JScrollPane matcherFileListTableScrollPane;

	private JTable matcherNETable;
	private JScrollPane matcherNETableScrollPane;
	private DefaultTableModel matcherNETableModel;

	private JButton matcherLoadFileButton = new JButton("Add file");
	private JButton matcherDeleteFileButton = new JButton("Remove file");
	private JPanel matcherButtonPanel1 = new JPanel();

	private JRadioButton pairViewRadioButton = new JRadioButton(
			"View by pairs", false);
	private JRadioButton groupViewRadioButton = new JRadioButton(
			"View by group", true);
	private JPanel matcherRadioButtonPanel = new JPanel();

	private JButton matcherStartButton = new JButton("Match");;
	private JButton matcherExportButton = new JButton("Export");
	private JPanel matcherButtonPanel2 = new JPanel();

	private static JLabel matcherStatusLabel1;
	private static JLabel matcherStatusLabel2;

	public JPanel buildMatcherPanel() {

		matcherPanel = new JPanel(new GridBagLayout());
		matcherPanel.setBackground(panel.bgColorPanel);
		//GridBagConstraints matcherPanelConstraints = new GridBagConstraints();

		// Row 1
		matcherLogoLabel = panel.createLogoLabel(matcherLogoLabel,
				matcherPanel);
		matcherIntroTextLabel = panel.createIntroTextLabel(
				matcherIntroTextLabel, matcherPanel);

		// Row 3 (Row 2 is empty. This is where the labels for the textfields
		// are in the extract-tab)
		matcherFileListTableModel = new MyTableModel(loadedFiles);
		matcherFileListTable = new JTable(matcherFileListTableModel);
		matcherFileListTableScrollPane = panel.createFileListTableScrollPane(
				matcherFileListTable, matcherFileListTableScrollPane,
				matcherPanel);
		matcherFileListTable.getModel().addTableModelListener(
				new TableModelListener() {
					public void tableChanged(TableModelEvent e) {
						setFileListLemmaUse(e.getFirstRow(), e.getColumn(),
								(TableModel) e.getSource());
					}
				});

		matcherNETableModel = panel.createNETableModel(matcherNETableModel);
		matcherNETable = new JTable(matcherNETableModel);
		matcherNETableScrollPane = panel.createNETable(matcherNETable,
				matcherNETableScrollPane, matcherNETableModel, matcherPanel);

		matcherButtonPanel1 = panel.createMatcherButtonPanel1(
				matcherButtonPanel1, matcherLoadFileButton,
				matcherDeleteFileButton, matcherPanel);
		matcherLoadFileButton.addActionListener(actor);
		matcherDeleteFileButton.addActionListener(actor);

		matcherRadioButtonPanel = panel.createMatcherRadioButtonPanel(
				matcherRadioButtonPanel, pairViewRadioButton,
				groupViewRadioButton, matcherPanel);
		pairViewRadioButton.addActionListener(actor);
		groupViewRadioButton.addActionListener(actor);

		matcherButtonPanel2 = panel.createMatcherButtonPanel2(
				matcherButtonPanel2, matcherStartButton,
				matcherExportButton, matcherPanel);
		matcherStartButton.addActionListener(actor);
		matcherExportButton.addActionListener(actor);

		matcherStatusLabel1 = panel.createMatcherStatusLabel1(
				matcherStatusLabel1, matcherPanel);
		matcherStatusLabel2 = panel.createMatcherStatusLabel2(
				matcherStatusLabel2, matcherPanel);
		return matcherPanel;
	}

	public void fillFileListTable(File f) {
		// fill the filelist with the name of the loaded file
		System.err.println("Loading file for matcher");
		loadedFiles.put(f.getName(), false); // all files start with the
												// lemma-tag 'false'

		matcherFileListTableModel.fillList(loadedFiles);
		matcherFileListTable.clearSelection();
		// fill the NE list with the content of the loaded file
		// fillmatcherNEList();
	}

	
	public void fillNETable() {
		System.err.println("fillNEList");
		Iterator<String> itr = mfMatch.NEtypes.iterator();
		while (itr.hasNext()) {
			String t = itr.next();
			for (String k : mfMatch.NETM.keySet()) {
				String[] key = NERTStringUtils.convertNETMformat(k);
				if (key[0].equals(t)) {
					feedVarsToNETable(k, key[0], key[1]);
				}
			}
		}
	}
	
	public void feedVarsToNETable(String entry, String type, String ne){
		// collect all variants
		ArrayList<String> variants = new ArrayList<String>();
		String f = "";
		Collection<NEInfo> c = mfMatch.NETM.get(entry);
		Iterator<NEInfo> d = c.iterator();
		while (d.hasNext()) {
			NEInfo e = (NEInfo) d.next();
			// get frequency
			f = String.valueOf(e.frequency);
			for (Iterator<String> it = e.givenVariants.keySet()
					.iterator(); it.hasNext();) {
				String givenVar = it.next();
				int freq = e.givenVariants.get(givenVar);
				variants.add(givenVar);
			}
			// matched variants
			for (Iterator<String> it = e.variants.keySet()
					.iterator(); it.hasNext();) {
				String var = it.next();
				String[] variantInfo = e.variants.get(var).split(
						";");
				// jaccard, which is what we want here, is under
				// index==1 (jac=2)
				String j = variantInfo[1].substring(4);
				// System.err.println("variantInfo[1]: "+variantInfo[1]+" jac="+j);
				int jacSim = Integer.parseInt(variantInfo[1]
						.substring(4));
				// int freq =
				// Integer.parseInt(e.variants.get(var).substring(2,
				// freqindex));
				// int freq = e.variants.get(var);
				if (jacSim >= 0) {
						variants.add(var.substring(0, var.length() - 3));
					// if(showVariantInfo.isSelected()){wholeString
					// +=
					// " ("+e.variants.get(var)+")";}
				}
			}
		}
		if ((groupViewRadioButton.isSelected())
				|| (mfMatch.pairwiseNETM.size() == 0)) {
			String allvars = "";
			for (String v : variants) {
				if (allvars.equals("")) {
					allvars += v;
				} else {
					allvars += ", " + v;
				}
			}
			matcherNETableModel.addRow(new String[] { type, f, ne, allvars });
		}
		if (pairViewRadioButton.isSelected()) {
			for (String v : variants) {
				matcherNETableModel.addRow(new String[] { type, f, ne, v });
			}
		}
	}
	
	
	
	public void setFileListLemmaUse(int row, int column, TableModel model) {
		System.err.println("column=" + column + " row=" + row);
		// TableModel model = (TableModel)e.getSource();
		// String columnName = model.getColumnName(column);
		int r = model.getRowCount();
		System.err.println("row=" + row + " rowcount=" + r);
		//if (r <= row) {
			Object checkbValue = model.getValueAt(row, column);
			Object filename = model.getValueAt(row, column + 1);
			System.err.println("column=" + column + " row=" + row + " data="
					+ checkbValue + " filename=" + filename);
			if (column != -1) {
				loadedFiles.put(filename.toString(), (Boolean) checkbValue);
				mfMatch.setUseAsLemma(filename.toString(), loadedFiles.get(filename));
				setStat.setStatus(mfMatch.NETM.size() + " NEs, "
						+ mfMatch.numberOfNEsUsedAsLemma + " lemmas. ",
						matcherStatusLabel1);
			}
		//}
	}
	
	

	//@TODO: method doesn't work!!
	public void removematcherFile(String f, int index){
		
		/*	Remove file f from matcher table, and remove its contents from the NETM.
		 */
		
		//remove from HashMap
		loadedFiles.remove(f);
		
		//remove from matcherFileListTable
		
		String a = matcherFileListTable.getModel().getValueAt(index, 1).toString();
		System.err.println("Removing index "+ index+" with content "+a);
		
		//System.err.println("num rows: " + matcherFileListTable.getRowCount());
		//matcherFileListTable.remove(index);
		//tableModel.fillList(loadedFiles);
		//tableModel.fireTableRowsDeleted(index, index);
		
	}
	
	
	private class ActionPerformer implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			
			if (src == matcherLoadFileButton) {
				System.err.println("Loading file for matcher");
				File file = fileHandler.getFile(true);
				if (file != null) {
					fillFileListTable(file);
					clearTable();
					mfMatch.loadNEsFromFile(file);
					fillNETable();
					setStat.setStatus(mfMatch.NETM.size() + " NEs. ",
							matcherStatusLabel1);
				}
			}

			if (src == matcherDeleteFileButton) {
				System.err.println("Removing file");
				int index = matcherFileListTable.getSelectedRow();
				if (index == -1) {
					JOptionPane.showMessageDialog(new JFrame(),
							"Please select a file to remove.",
							"Error removing file ...",
							JOptionPane.WARNING_MESSAGE);
				} else {
					String f = matcherFileListTable.getModel().getValueAt(
							index, 1).toString();
					System.err.println("Removing file " + f);
					removematcherFile(f, index);
				}
			}

			if (src == matcherStartButton) {
				System.err.println("Starting matcher NE's in NElist ... ");
				setStat.setStatus("Status: matcher ...", matcherStatusLabel2);
				// check lemma use first
				boolean lem = false;
				for (String f : loadedFiles.keySet()) {
					if (loadedFiles.get(f)) {
						lem = true;
					}
				}
				mfMatch.useLemma = lem;
				boolean y = mfMatch.initNEMatcher(true, true, true);
				clearTable();
				fillNETable();pairViewRadioButton.setVisible(true);
				groupViewRadioButton.setVisible(true);
				setStat.setStatus("Status: Ready.", matcherStatusLabel2);
			}

			if (src == matcherExportButton) {

				System.err.println("Exporting NEs ... ");
				setStat.setStatus("Status: Exporting ...", matcherStatusLabel2);
				fileHandler.printToFile(fileHandler.getFile(false), NERTStringUtils.writeDataToString(matcherNETable));
				setStat.setStatus("Exported table.", matcherStatusLabel1);
				setStat.setStatus("Status: Ready.", matcherStatusLabel2);
			}

			if ((src == pairViewRadioButton) || (src == groupViewRadioButton)) {
				// user has changed view, so repaint the NE list.
				// repaintmatcherLists(pairViewRadioButton.isSelected());
				// matcherTab.fillmatcherNEList();
				clearTable();
				fillNETable();
			}
		}

	}

	/*
	 * This method clears all the content of the table. There's probably a
	 * better and faster way to do this.
	 */

	public void clearTable() {

		matcherNETable.setAutoCreateRowSorter(false);
		int numRows = matcherNETableModel.getRowCount();
		for (int i = numRows - 1; i >= 0; i--) {
			matcherNETableModel.removeRow(i);
		}
		matcherNETable.setAutoCreateRowSorter(true);
	}


}
