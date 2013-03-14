package nl.inl.impact.ner.matcher;

import java.util.ArrayList;
import java.util.TreeMap;

public class NEInfo {
	String sourcefile = "";					//file from which this NE is taken
	boolean useAsLemma = false;
	public int frequency = 0;
	String date = "";						//(best guess at) date of entry/entries
	String type = "";						//LOC/PER/ORG
	String normalizedString = "";			//normalized version of entire NE
	String keyword = "";					//main word used for matching (ie last name)
	ArrayList<String> sourcefiles = new ArrayList<String>();
	ArrayList<String> ngrams = new ArrayList<String>();
	//ArrayList<String> variants = new ArrayList<String>();
	public TreeMap<String, String> variants = new TreeMap<String, String>();
	
	//this map is used for all variants that are given in the file, i.e. with tabs
	//it's separated from the 'variants' map, because some given variants might
	//not match at all with the keyword, and therefore be thrown out, 
	//which is what we do not want
	public TreeMap<String, Integer> givenVariants = new TreeMap<String, Integer>();
	private static final long serialVersionUID = -5903728481621584812L;
}