package nl.inl.impact.ner.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class FileHandler {
	
	private JFileChooser fileChooser = new JFileChooser("/mnt/Projecten/Impact/NER/NER_tools/");
	private JFrame frame;
	
	
	/*	A general method for opening a file chooser dialogue. Used 
	 * 	for opening (open == true) and saving (open == false) file.
	 */
	
	
	public File getFile(boolean open) {
		System.err.println("FileHandler.getFile. Open? " + open);
		File file = null;
		int returnVal;
		if (open) {
			returnVal = fileChooser.showOpenDialog(frame);
		} else {
			returnVal = fileChooser.showSaveDialog(frame);
		}
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = fileChooser.getSelectedFile();
			if (open && !checkFile(file)) {
				file = null;
			}
		}
		return file;
	}
	
	
	public boolean checkFile(File file) {
		if (file.isFile()) {
			fileChooser.setCurrentDirectory(file.getParentFile());
			return true;
		} else {
			String message = "File Not Found: " + file.getAbsolutePath();
			displayError("File Not Found Error", message);
			return false;
		}
	}
	
	public void displayError(String title, String message) {
		JOptionPane.showMessageDialog(frame, message, title,
				JOptionPane.ERROR_MESSAGE);
	}
	
	
	/*	General method for writing a String to a file.
	 */
	
	public void printToFile(File file, String message) {
		System.err.println("Exporting data to file ...");
		PrintWriter pw = null;
		try {
			Writer fw = new FileWriter(file);
			pw = new PrintWriter(fw);
			pw.print(message);
		} catch (Exception e) {
			System.err.println("Exception: in printToFile " + file.getAbsolutePath());
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.flush();
				pw.close();
			}	
		}	
		System.err.println("Done exporting data to file.");
	}

	
	public static String readFile(String filepath){
		return readFile(new File(filepath));
	}
	
	/* 	Puts content of file in string. Called from the extractor class
	 */
	
	public static String readFile(File file){
		
		String lineSep = System.getProperty("line.separator");
		StringBuffer sb = new StringBuffer();
		try {
			FileReader doc = new FileReader(file.getAbsolutePath());
			BufferedReader buff = new BufferedReader(doc);
			boolean eof = false;
			while (!eof) {
				String line = buff.readLine();
				if (line == null) {
					eof = true;
				} else {
					sb.append(line);
					sb.append(lineSep);
				}
			}
			buff.close();
		} catch (IOException e) {
			System.err.println("Woops. Error reading file. " + e.toString());
		}
		return sb.toString();
	}
	
	public static void writeNEsToFile(String file, TreeMap<String, Integer>NEListHM) {

		FileOutputStream out;
		PrintStream p;

		try {
			out = new FileOutputStream(file);
			p = new PrintStream(out);
			Iterator it = NEListHM.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				p.println(pairs.getKey() + " " + pairs.getValue());
			}
			p.close();
			System.err.println("Written NE-list to file " + file);
		} catch (Exception e) {
			System.err.println("Error writing named entity list to file");
		}
	}
}
