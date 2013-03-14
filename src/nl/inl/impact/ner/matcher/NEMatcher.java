package nl.inl.impact.ner.matcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.util.Timing;

import nl.inl.impact.ner.utils.SemiFoneticTranscriptor;

public class NEMatcher {

	/*
	 * Frank Landsbergen, INL, September 2010 version 0.1
	 * 
	 * This NEMatcher compares a given string or a set of strings in a file to
	 * another set of strings and returns the n closest variants.
	 * 
	 * Matching is done by comparing the number of shared s-grams (with skip 0,
	 * 1 and 2, using digrams) of normalized versions of the NE's, multiplied by
	 * the difference in string length. Scores range from 0 - 100, where 100 is
	 * a match with the exact same string (that is, with the exact same
	 * normalized string).
	 * 
	 * String normalization involves (1) the
	 * change to lowercase and (2) the transition into a semi-phonetic
	 * transcription.
	 * 
	 * @TODO One of the steps in the normalizer-method only works for Java 1.6
	 * and up. Make compatible for 1.5?
	 */

	// parameters
	static boolean verbose = false;
	static boolean pairview = false;
	static boolean groupview = false;
	static boolean printScore = false;
	static boolean printSource = false;
	static boolean printType = false;
	static boolean showDoubles = false;
	static boolean showPhoneticTranscription = false;
	
	static boolean useLemma = false;
	static int nBest = -1;
	static int minScore = 50;

	static int defaultMinScore = 50;
	static boolean hasType = false; // needs to be set to 'true' if the input
	static boolean useType = false;
	static boolean PERFilter = false;
	static String ignoreWordsWithTag = "";
	static int diffInitialPunishment = 10;
	
	int minSGramCutOffValue = 2; // min. number of shared s-grams: if less, the
									// variant is disregarded

	int N = 2; // default values. These are used to build the s-grams.
	int MIN_N_LENGTH = 2;
	int MAX_N_LENGTH = 2;
	int maxSGrams = 0; // the maximum possible number of s-grams, which is the
	// number of s-grams of the given word.

	static int nonUniqueCounter = 0;
	static int neCounter = 0;
	
	int typeTab = -1;
	int neTab = 0;			//	by default, we expect the NE to be the first (=0) element on each line,
							//	unless otherwise indicated by the argument 'line' in the propsfile
	int lemmaTypeTab = -1;	//	same, but for the lemma information
	int lemmaNeTab = 0;
	
	Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

	//Set<String> lemmaSet = new HashSet<String>();
	LinkedList<String> lemmaSet = new LinkedList<String>();
	
	//Set<String> neSet = new HashSet<String>();
	
	LinkedList<String> neSet = new LinkedList<String>();
	//SortedSet<String> neSet = new TreeSet<String>();
	LinkedList<String> restSet = new LinkedList<String>();
	LinkedList<String> sourceInfo = new LinkedList<String>();
	LinkedList<String> lemmaSourceInfo = new LinkedList<String>();
	
	HashMap<String, Integer> fileOnsetHM = new HashMap<String, Integer>();
	HashMap<String, Integer> fileOffsetHM = new HashMap<String, Integer>();
	Set<String> showFileSet = new HashSet<String>();
	
	HashMap<String, String> neHM = new HashMap<String, String>();
	HashMap<String, Set<String>> sGramHM = new HashMap<String, Set<String>>();
	HashMap<String, Integer> vars = new HashMap<String, Integer>();
	HashMap<String, String> normalizedHM = new HashMap<String, String>();

	HashMap<String, Set<String>> typeHM = new HashMap<String, Set<String>>();
	HashMap<String, Set<String>> lemmaTypeHM = new HashMap<String, Set<String>>();
	HashMap<String, String[]> partsHM = new HashMap<String, String[]>();
	HashMap<String, String> initialsHM = new HashMap<String, String>();
	HashMap<String, Boolean> hasInitialsHM = new HashMap<String, Boolean>();
	HashMap<String, String> royaltyHM = new HashMap<String, String>();

	Set<String> surnameIdentifiers = new HashSet<String>();

	HashMap<String, String> string2romanHM = new HashMap<String, String>();

	String romanRegexShort = "([clxvi123456789]+)";
	String stringRegex = "(eerste|tweede|derde|vierde|vijfde|zesde|zevende|achtste|negende)";
	String phonTransFile = "";
	
	Matcher matcher;
	Pattern patternStringRegex = Pattern.compile(stringRegex);
	Pattern patternRomanRegexShort = Pattern.compile(romanRegexShort);

	SemiFoneticTranscriptor sft;
	
	public HashMap<String, String> loadProperties(String filename) {

		System.err.print("Loading NEMatcher properties ...");

		// load the properties file and store all the props in hashmap
		// 'arguments'
		HashMap<String, String> arguments = new HashMap<String, String>();

		try {
			FileReader doc = new FileReader(filename);
			BufferedReader buff = new BufferedReader(doc);
			boolean eof = false;
			while (!eof) {
				String line = buff.readLine();
				if (line == null) {
					eof = true;
				} else {
					if (!line.equals("") && !line.substring(0, 1).equals("#")) {
						// strip remarks from the line
						int remarkIndex = line.indexOf("#");
						if (remarkIndex > -1) {
							line = line.substring(0, remarkIndex);
						}
						// split the line
						String[] args = line.split("=");

						if (args[0].equalsIgnoreCase("verbose")) {
							if(args.length > 1){arguments.put("verbose", args[1]);}
							else{arguments.put("verbose", "");}
						}
						if (args[0].equalsIgnoreCase("pairview")) {
							if(args.length > 1){arguments.put("pairview", args[1]);}
							else{arguments.put("pairview", "");}
						}
						if (args[0].equalsIgnoreCase("groupview")) {
							if(args.length > 1){arguments.put("groupview", args[1]);}
							else{arguments.put("groupview", "");}
						}
						if (args[0].equalsIgnoreCase("printscore")) {
							if(args.length > 1){arguments.put("printScore", args[1]);}
							else{arguments.put("printScore", "");}
						}
						if (args[0].equalsIgnoreCase("printsource")) {
							if(args.length > 1){arguments.put("printSource", args[1]);}
							else{arguments.put("printSource", "");}
						}
						if (args[0].equalsIgnoreCase("printtype")
								|| args[0].equalsIgnoreCase("printtype")) {
							if(args.length > 1){arguments.put("printType", args[1]);}
							else{arguments.put("printType", "");}
						}
						if (args[0].equalsIgnoreCase("nbest")) {
							if(args.length > 1){arguments.put("nBest", args[1]);}
							else{arguments.put("nBest", "");}
						}
						if (args[0].equalsIgnoreCase("minscore")
								|| args[0].equalsIgnoreCase("minimalscore")) {
							if(args.length > 1){arguments.put("minScore", args[1]);}
							else{arguments.put("minScore", "");}
						}
						if (args[0].equalsIgnoreCase("usetype")) {
							if(args.length > 1){arguments.put("useType", args[1]);}
							else{arguments.put("useType", "");}
						}
						if (args[0].equalsIgnoreCase("line")) {
							if(args.length > 1){arguments.put("line", args[1]);}
							else{arguments.put("line", "");}
						}
						if (args[0].equalsIgnoreCase("lemmaline")) {
							if(args.length > 1){arguments.put("lemmaLine", args[1]);}
							else{arguments.put("lemmaLine", "");}
						}
						/*
						if (args[0].equalsIgnoreCase("showtype")) {
							if(args.length > 1){arguments.put("showType", args[1]);}
							else{arguments.put("showType", "");}
						}
						*/
						if (args[0].equalsIgnoreCase("onlyshowfile") || args[0].equalsIgnoreCase("onlyshowfiles")) {
							if(args.length > 1){arguments.put("onlyShowFile", args[1]);}
							else{arguments.put("onlyShowFile", "");}
						}
						if (args[0].equalsIgnoreCase("file")) {
							if(args.length > 1){arguments.put("sourceFile", args[1]);}
							else{arguments.put("sourceFile", "");}
						}
						if (args[0].equalsIgnoreCase("dir")) {
							if(args.length > 1){arguments.put("sourceDir", args[1]);}
							else{arguments.put("sourceDir", "");}
						}
						if (args[0].equalsIgnoreCase("lemmafile")) {
							if(args.length > 1){arguments.put("lemmaFile", args[1]);}
							else{arguments.put("lemmaFile", "");}
						}
						if (args[0].equalsIgnoreCase("lemmadir")) {
							if(args.length > 1){arguments.put("lemmaDir", args[1]);}
							else{arguments.put("lemmaDir", "");}
						}
						if (args[0].equalsIgnoreCase("word")) {
							if(args.length > 1){arguments.put("word", args[1]);}
							else{arguments.put("word", "");}
						}
						if (args[0].equalsIgnoreCase("showdoubles")) {
							if(args.length > 1){arguments.put("showDoubles", args[1]);}
							else{arguments.put("showDoubles", "");}
						}
						
						if (args[0].equalsIgnoreCase("perfilter")) {
							if(args.length > 1){arguments.put("PERFilter", args[1]);}
							else{arguments.put("PERFilter", "");}
						}
						if (args[0].equalsIgnoreCase("surnameidentifiers")) {
							if(args.length > 1){arguments.put("surnameIdentifiers", args[1]);}
							else{arguments.put("surnameIdentifiers", "");}
						}
						if (args[0].equalsIgnoreCase("string2roman")) {
							if(args.length > 1){arguments.put("string2roman", args[1]);}
							else{arguments.put("string2roman", "");}
						}
						if (args[0].equalsIgnoreCase("phontrans")) {
							if(args.length > 1){arguments.put("phonTrans", args[1]);}
							else{arguments.put("phonTrans", "");}
						}
						if (args[0].equalsIgnoreCase("punishdiffinitial") || args[0].equalsIgnoreCase("punishdiffinitials") ) {
							if(args.length > 1){arguments.put("punishDiffInitial", args[1]);}
							else{arguments.put("punishDiffInitial", "");}
						}
						if (args[0].equalsIgnoreCase("ignorewordswithtag")) {
							if(args.length > 1){arguments.put("ignoreWordsWithTag", args[1]);}
							else{arguments.put("ignoreWordsWithTag", "");}
						}
						if (args[0].equalsIgnoreCase("showphonetictranscription")) {
							if(args.length > 1){arguments.put("showPhoneticTranscription", args[1]);}
							else{arguments.put("showPhoneticTranscription", "");}
						}
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Woops. Error reading file. " + e.toString());
		}
		System.err.println(" done.");
		return arguments;
	}

	
	private List<String> loadPhonTransRules(){
		List<String> phonTransList = new LinkedList<String>();
		//if(verbose){System.err.print("Loading phonetic transcription rules ...");}
		//System.err.println("FILE: >"+phonTransFile+"<");
		try {
			FileReader doc = new FileReader(phonTransFile);
			BufferedReader buff = new BufferedReader(doc);
			boolean eof = false;
			while (!eof) {
				String line = buff.readLine();
				if (line == null) {
					eof = true;
				} else {
					if (!line.equals("")) {
						//lines could start with # or have it added to end of string: chop off
						if (!line.substring(0, 1).equals("#")) {
							//rules are in format A=>B
							int ind = line.indexOf("#");
							if(ind > 0){line = line.substring(0, ind);}
							line = line.trim();
							if(verbose){System.err.println("Found rule: "+line);}
							phonTransList.add(line);
						}
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Woops. Error reading file. " + e.toString());
		}
		
		if(verbose){System.err.println("Done. Loaded " + phonTransList.size() + " external rules.");}
		return phonTransList;
	}
	
	
	private void loadSurnameIdentifiers(String filename) {

		if(filename.equals("")){
			if(verbose){System.err.println("Empty argument for surname identifiers - not using this.");}
		}
		else{
			if(verbose){System.err.println("Loading file with surname identifiers ... ");}
	
			try {
				FileReader doc = new FileReader(filename);
				BufferedReader buff = new BufferedReader(doc);
				boolean eof = false;
				while (!eof) {
					String line = buff.readLine();
					if (line == null) {
						eof = true;
					} else {
						if (!line.equals("")) {
							if (!line.substring(0, 1).equals("#")) {
								if(verbose){System.err.println("Found identifier: "+line);}
								surnameIdentifiers.add(line);
							}
						}
					}
				}
				if(verbose){System.err.println("Done.");}
			} catch (IOException e) {
				System.err.println("Woops. Error reading surname identifiers file. " + e.toString());
			}
		}
	}

	private void loadString2Roman(String filename) {

		if(filename.equals("")){
			if(verbose){System.err.println("Empty argument for string2roman - not using this.");}
		}
		else{
			if(verbose){System.err.println("Loading file with string2roman conversion rules ... ");}
			try {
				FileReader doc = new FileReader(filename);
				BufferedReader buff = new BufferedReader(doc);
				boolean eof = false;
				while (!eof) {
					String line = buff.readLine();
					if (line == null) {
						eof = true;
					} else {
						if (!line.substring(0, 1).equals("#")) {
							String[] parts = line.split("=>");
							string2romanHM.put(parts[0], parts[1]);
						}
					}
				}
			} catch (IOException e) {
				System.err.println("Woops. Error reading string2roman file. " + e.toString());
			}

			// we also set the stringRegex with the keys from the HM.
			if (string2romanHM.size() > 0) {
				String strRegex = "(";
				for (String s : string2romanHM.keySet()) {
					if (!strRegex.equals("(")) {
						strRegex += "|";
					}
					strRegex += s;
				}
				int index = strRegex.lastIndexOf("|");
				strRegex = strRegex.substring(0, index) + ")";
				if(verbose){System.err.println("Added regex: "+strRegex);}
				patternStringRegex = Pattern.compile(stringRegex);
			}
	
			if(verbose){System.err.println("done.");}
		}
	}

	public void initMatcher(HashMap<String, String> args) {

		/*
		 * Main initialization method with the arguments passed on from the
		 * properties file. The actual matching is called from this method.
		 */

		Timing MatcherTimer = new Timing();
		
		String word = "";
		String sourceFile = "";
		String sourceDir = "";
		String lemmaFile = "";
		String lemmaDir = "";
		nonUniqueCounter = 0;
		String lineStructure = "";
		String lemmaLineStructure = "";
		String onlyShowFiles = "all";
		
		// set main parameters
		if (args.containsKey("verbose")) {
			verbose = Boolean.parseBoolean(args.get("verbose"));
		}
		if (args.containsKey("pairview")) {
			pairview = Boolean.parseBoolean(args.get("pairview"));
		}
		if (args.containsKey("groupview")) {
			groupview = Boolean.parseBoolean(args.get("groupview"));
		}
		if (args.containsKey("printScore")) {
			printScore = Boolean.parseBoolean(args.get("printScore"));
		}
		if (args.containsKey("printSource")) {
			printSource = Boolean.parseBoolean(args.get("printSource"));
		}
		if (args.containsKey("printType")) {
			printType = Boolean.parseBoolean(args.get("printType"));
		}
		if (args.containsKey("showDoubles")) {
			showDoubles = Boolean.parseBoolean(args.get("showDoubles"));
		}
		if (args.containsKey("nBest")) {
			//System.err.println("nBest: "+args.get("nBest"));
			double val = Double.parseDouble(args.get("nBest"));
			if(val < 0){
				//we do not need to store this value since -1 is the default value
			}
			else{
				nBest = (int) val;
			}
		}
		if (args.containsKey("minScore")) {
			minScore = Integer.parseInt(args.get("minScore"));
		}
		if (args.containsKey("punishDiffInitial")) {
			diffInitialPunishment = Integer.parseInt(args.get("punishDiffInitial"));
		}
		/*
		if (args.containsKey("hasType")) {
			hasType = Boolean.parseBoolean(args.get("hasType"));
		}
		*/
		if (args.containsKey("line")) {
			lineStructure = args.get("line");
		}
		if (args.containsKey("lemmaLine")) {
			lemmaLineStructure = args.get("lemmaLine");
		}
		
		if (args.containsKey("useType")) {
			useType = Boolean.parseBoolean(args.get("useType"));
		}
		if (args.containsKey("onlyShowFile")) {
			onlyShowFiles = args.get("onlyShowFile");
		}
		
		if (args.containsKey("PERFilter")) {
			PERFilter = Boolean.parseBoolean(args.get("PERFilter"));
		}
		
		if (args.containsKey("ignoreWordsWithTag")) {
			ignoreWordsWithTag = args.get("ignoreWordsWithTag");
		}
		//new sept 2011
		if (args.containsKey("showPhoneticTranscription")) {
			//System.out.println(args.get("showPhoneticTranscription"));
			showPhoneticTranscription = Boolean.parseBoolean(args.get("showPhoneticTranscription"));
		}

		// we only use pairview
		if (groupview && pairview) {
			groupview = false;
		}
		if (!groupview) {
			pairview = true;
		}

		
		if(!lineStructure.equals("")){
			//	read line structure, expecting comma separated arguments
			String[] parts = lineStructure.split(",");
			
			for(int i = 0 ; i < parts.length; i++){
				//System.err.println("part: "+parts[i]);
				String[] p2 = parts[i].split(":");
				//System.err.println("p2[0]: "+p2[0]+" p2[1]: " + p2[1]);
				if(p2[0].equalsIgnoreCase("type")){
					if(p2.length > 1){
						typeTab = Integer.parseInt(p2[1]);
					}
				}
				if(p2[0].equalsIgnoreCase("ne")){
					if(p2.length > 1){
						neTab = Integer.parseInt(p2[1]);
					}
				}
			}
		}
		//System.err.println("lemmaline: "+lemmaLineStructure);
		if(!lemmaLineStructure.equals("")){
			//	read line structure, expecting comma separated arguments
			String[] parts = lemmaLineStructure.split(",");
			
			for(int i = 0 ; i < parts.length; i++){
				//System.err.println("part: "+parts[i]);
				String[] p2 = parts[i].split(":");
				//System.err.println("p2[0]: "+p2[0]+" p2[1]: " + p2[1]);
				if(p2[0].equalsIgnoreCase("type")){
					if(p2.length > 1){
						lemmaTypeTab = Integer.parseInt(p2[1]);
					}
				}
				if(p2[0].equalsIgnoreCase("ne")){
					if(p2.length > 1){
						lemmaNeTab = Integer.parseInt(p2[1]);
					}
				}
			}
		}
				
		if (verbose) {
			System.err.println("Settings:");
			//input parameters
			System.err.println("###\tinput parameters:");
			System.err.println("\ttypeTab = " + typeTab);
			System.err.println("\tneTab = " + neTab);
			System.err.println("\tlemmaTypeTab = " + lemmaTypeTab);
			System.err.println("\tlemmaNeTab = " + lemmaNeTab);
			System.err.println("\tignoreWordsWithTag = " + ignoreWordsWithTag);
			//System.err.println("hasType = " + hasType);
			
			//output parameters
			System.err.println("###\toutput parameters:");
			System.err.println("\tverbose = " + verbose);
			if(pairview){System.err.println("\tview = pairview");}
			if(groupview){System.err.println("\tview = groupview");}
			System.err.println("\tonlyShowFiles = " + onlyShowFiles);
			System.err.println("\tprintScore = " + printScore);
			System.err.println("\tprintSource = " + printSource);
			System.err.println("\tprintType = " + printType);
			System.err.println("\tnBest = " + nBest);
			System.err.println("\tshowDoubles = " + showDoubles);
			System.err.println("\tshowPhoneticTranscription = " + showPhoneticTranscription);
			//System.err.println("showType = " + showType);
			
			//scoring parameters
			System.err.println("###\tscoring parameters:");
			System.err.println("\tminScore = " + minScore);
			System.err.println("\tdiffInitialPunishment = " + diffInitialPunishment);
			System.err.println("\tPERFilter = " + PERFilter);
			
			//pattern files
			System.err.println("###\tpattern matching files:");
			if (args.containsKey("surnameIdentifiers")) {
				System.err.println("\tsurnameIdentifiers = "
						+ args.get("surnameIdentifiers"));
			}
			if (args.containsKey("string2roman")) {
				System.err.println("\tstring2roman = " + args.get("string2roman"));
			}
			if (args.containsKey("phonTrans")) {
				System.err.println("\tphonetic transcription file: " + args.get("phonTrans"));
			}
		}

		// read external files if applicable
		if (args.containsKey("surnameIdentifiers")) {
			loadSurnameIdentifiers(args.get("surnameIdentifiers"));
		}
		if (args.containsKey("string2roman")) {
			loadString2Roman(args.get("string2roman"));
		}
		if (args.containsKey("phonTrans")) {
			phonTransFile = args.get("phonTrans");
		}
		
		//initialize the transcriptor
		//load content of phonTrans file into List
		sft = new SemiFoneticTranscriptor();
		if(args.containsKey("phonTrans")){
			if(args.get("phonTrans").equals("")){
				if(verbose){System.err.println("Found empty argument for phonTrans. Using default rules");}
				sft.initTranscriptor();
			}
			else{
				if(verbose){System.err.println("Found filename with phonTrans. Loading external rule set");}
				List<String> li = loadPhonTransRules();
				sft.initTranscriptor(li);
			}
		}
		else{
			if(verbose){System.err.println("No phonTrans argument. Using default rules");}
			sft.initTranscriptor();
		}
		
		// set files or word
		word = args.get("word");
		sourceFile = args.get("sourceFile");
		sourceDir = args.get("sourceDir");
		lemmaFile = args.get("lemmaFile");
		lemmaDir = args.get("lemmaDir");

		if (word != null) {
			if (verbose) {
				System.err.println("word = " + word);
			}
			neSet.add(word);
		}
		if (sourceFile != null) {
			if (verbose) {
				System.err.println("sourceFile = " + sourceFile);
			}
			//set onlyShowFiles
			if(onlyShowFiles.equals("all")){
				//the user has not specified which file to show: we show the sourcefile
				//as default
				File file = new File(sourceFile);
				String filebasename = file.getName();
				showFileSet.add(filebasename);
			}
			//loadNEs(sourceFile, false, hasType);
			loadNEs(sourceFile, false);
		}
		if (sourceDir != null) {
			if (verbose) {
				System.err.println("sourceDir = " + sourceDir);
			}
			if (verbose) {
				System.err.println("Looking for files in dir " + sourceDir);
			}
			File folder = new File(sourceDir);
			String[] listOfFiles = folder.list();
			String absPath = folder.getAbsolutePath();
			// File[] listOfFiles = folder.listFiles();
			String[] files = new String[listOfFiles.length];
			for (int i = 0; i < listOfFiles.length; i++) {
				files[i] = absPath + "/" + listOfFiles[i];
				if (verbose) {
					System.err.println("Found file " + files[i]);
				}
				//the user has not specified a subset of files he wants to see, we take all
				//the files that are specified in the 'dir' flag and use those.
				if(onlyShowFiles.equals("all")){

					//	we save the basename for reference, in order to avoid
					//	problems with relative and absolute paths by the user
					File file = new File(files[i]);
					String filebasename = file.getName();
					//System.err.println("Storing filebasename "+filebasename);
					showFileSet.add(filebasename);
					
					//showFileSet.add(files[i]);
				}
				
				//loadNEs(files[i], false, hasType);
				loadNEs(files[i], false);
			}
		}

		if (lemmaFile != null) {
			if (verbose) {
				System.err.println("lemmaFile = " + lemmaFile);
			}
			useLemma = true;
			//loadNEs(lemmaFile, true, hasType);
			loadNEs(lemmaFile, true);
		}

		if (lemmaDir != null) {
			if (verbose) {
				System.err.println("lemmaDir = " + lemmaDir);
			}
			if (verbose) {
				System.err.println("Looking for files in dir " + lemmaDir);
			}
			useLemma = true;
			File folder = new File(lemmaDir);
			String absPath = folder.getAbsolutePath();
			File[] listOfFiles = folder.listFiles();
			String[] files = new String[listOfFiles.length];
			for (int i = 0; i < listOfFiles.length; i++) {
				files[i] = absPath + "/" + listOfFiles[i].getName();
				if (verbose) {
					System.err.println("Found file " + files[i]);
				}
				//loadNEs(files[i], true, hasType);
				loadNEs(files[i], true);
			}
		}
		
		//onlyshowfiles
		//option 1: the user has not specified which files to show: onlyShowFiles="all",
		//and we show all files (either in dir, or in file). This has been done already
		//option 2: the user has specified which files to show: onlyShowFiles!="all",
		//we show only these files. We do this here:
		//get the list of files that the user wants to have printed.
		//could be 1 or more files, in the latter case they are ';'-separated.
		if(!onlyShowFiles.equals("all")){
			String[] onlyShowFi = onlyShowFiles.split(";");
			for(String f : onlyShowFi){
				f.replaceAll("^\\s+", "");
				f.replaceAll("\\s+$", "");
				File file = new File(f);
				String filebasename = file.getName();
				showFileSet.add(filebasename);
			}
			
		}
		
		
		
		if(verbose){System.err.println("Found " + nonUniqueCounter + " NEs; loaded "
				+ neSet.size() + " unique NEs and " + lemmaSet.size()
				+ " lemmata.");}

		/*
		 * Now we start the matcher with the appropriate set of NE's. This
		 * means: if a Set of lemmata exists, we use those as the main set from
		 * which the s-gram index is made, and we iterate over the neSet to find
		 * a match with one or more lemmata. If the set of lemmata is empty, we
		 * compare the NE's in the neSet with each other: we create an s-gram
		 * index from the NE's in neSet, and iterate over the same set for
		 * matching.
		 */

		// Create s-gram index
		if (lemmaSet.size() != 0) {
			createSGramHM(lemmaSet);
		} else {
			createSGramHM(neSet);
		}

		// Run the matcher
		/*
		if (verbose) {
			System.err.println("Starting matching for " + neSet.size()
					+ " NEs.");
		}
		*/
		/*
		if(!verbose){
			System.err.print("Starting matching for " + neSet.size() +" NEs ... ");
		}
		*/
		
		//calculate the total number of NE's we're going to match
		int totalNumOfNEs = 0;
		if(verbose){
			System.err.println("Find matches for the following files:");
			for(String fi : showFileSet){
				//File file = new File(fi);
				//String basefilename = file.getName();
				System.err.println(fi);
				int onset = fileOnsetHM.get(fi);
				//System.err.println("fileOnsetHM of this file: "+onset);
				int offset = fileOffsetHM.get(fi);
				//System.err.println("fileOffsetHM of this file: "+offset);
				totalNumOfNEs += (offset - onset);
			}
		}
		System.err.println("Starting matching for " + totalNumOfNEs	+ " NEs.");
		
		int counter = 0;
		
		for(String fi : showFileSet){
		//for(String fi : fileOnsetHM.keySet()){
			//System.err.println("file: " + fi + " onset: " + fileOnsetHM.get(fi) + " offset: " + fileOffsetHM.get(fi));
			//File file = new File(fi);
			//String basefilename = file.getName();
			//System.err.println(fi);
			int onset = fileOnsetHM.get(fi);
			int offset = fileOffsetHM.get(fi);
				
			if(verbose){
				System.err.println("Matching NEs of file " + fi);
			}
			
			for(int i = onset; i < offset; i++){
				String w = neSet.get(i);
				//System.err.println("## finding matches for word "+w);
				
				String preStr = "";
				String sourceinf = "";
				if(restSet.size() > i){
					preStr = restSet.get(i);
				}
				if(sourceInfo.size() > i){
					sourceinf = sourceInfo.get(i);
				}
				if (verbose && counter % 1000 == 0) {
					System.err.print("[" + counter + "]");
				}
				HashMap<Integer, Set<String>> res = doSGramComparison(w,
						normalizedHM.get(w));
				
				//System.err.println("## found "+res.size()+" matches for word "+w);
				if (useType) {
					//System.err.println("## filtering these matches...");
					res = filterResults(w, res);
				}
				//printMatches(w, res);
				printMatches(w, preStr, sourceinf, res);
				cleanUp();
				counter++;
			}
		}
		if(verbose){
			long millis = MatcherTimer.stop();
			double secs = ((double) millis) / 1000;
			System.err.println("\nDone. Matching took " + secs + " seconds.");
		}
		
		if(!verbose){
			System.err.println(" done.");
		}
		
		/*
		for(int i = 0 ; i < sourceInfo.size(); i++){
			
			String a = sourceInfo.get(i);
			System.out.println(i+" "+a);
		}
		*/
	}

	public HashMap<String, List<String>> runMatcherForTrainAndTest(Set<String> NEList, Set<String> lemmaList, int minS){
		minScore = minS;
		groupview = false;
		hasType = false;
		useType = false;
		//initialize the transcriptor
		sft.initTranscriptor();
		createSGramHM(lemmaSet);
		int counter = 0;
		HashMap<String, List<String>> results = new HashMap<String, List<String>>();
		for (String w : neSet) {
			counter++;
			if (verbose && counter % 1000 == 0) {
				System.err.print("[" + counter + "]");
			}
			HashMap<Integer, Set<String>> res = doSGramComparison(w,
					normalizedHM.get(w));
			results.put(w, printMatchesToList(w, res));
			cleanUp();
			
		}
		return results;
	}
	
	public void initMatcher(String printview) {
		initMatcher(printview, defaultMinScore);
	}

	
	private void createSGramHM(Collection<String> inputSet) {

		/*
		 * This method builds the s-gram hashmap from all the NE's loaded from
		 * the input file and stored in neSet. The s-grams are built on the
		 * basis of normalized version of the NE's.
		 * 
		 * Building is done by calling the method 'sGramBuilder()' three times,
		 * with different settings for the s(kip): 0, 1, 2. sGramBuilder()
		 * returns a set of sgrams for the particular NE, and these are all
		 * added to the main hashmap 'sGramHM'.
		 * 
		 * In order to keep track of the source of each word, we add the sourceinfo index to each
		 * word: 'word#index'. This is needed because words are not unique.
		 * 
		 */

		System.err.print("Building s-gram index ... ");
		for (String s : inputSet) {
			String s_normalized = normalizedHM.get(s);
			if (s_normalized.length() > 1) {
				Set<String> sgA = sGramBuilder(s_normalized, N, 0);
				addSetToSGramHM(s, sgA);
				Set<String> sgB = sGramBuilder(s_normalized, N, 1);
				addSetToSGramHM(s, sgB);
				Set<String> sgC = sGramBuilder(s_normalized, N, 2);
				addSetToSGramHM(s, sgC);
			}
		}
		System.err.print("done (" + sGramHM.size() + " unique s-grams).");
		System.err.println("\n");
	}
	
	public List<HashMap<Integer, Set<String>>> performMatching(List<String> neSet)
	{
		List<HashMap<Integer, Set<String>>> res = new ArrayList<HashMap<Integer, Set<String>>>();
		for (String s: neSet)
			normalizedHM.put(s, normalizeString(s));
		createSGramHM(neSet);
		
		// iterate over all words in the list and print their variants
		// int counter = 0;
		
		//for (String word : neSet) {
		int counter = 0;
		for(int i = 0; i < neSet.size(); i++)
		{
			String word = neSet.get(i);
			String preStr = "";
			String sourceinf = "";
			/*
			if(restSet.size() > i){
				preStr = restSet.get(i);
			}
			if(sourceInfo.size() > i){
				sourceinf = sourceInfo.get(i);
			}
			*/
			// if(counter%100==0){System.err.print("["+counter+"]");}
			// counter++;
			// HashMap<Integer, Set<String>> res = doSGramComparison(word,
			// normalizeString(word));
			HashMap<Integer, Set<String>> groups = doSGramComparison(word,
					normalizedHM.get(word));
			res.add(groups);
			//printMatches(word, preStr, sourceinf, res);
			//printMatches(word, res);
			cleanUp();
			counter++;
		}
		return res;
	}
	
	public void initMatcher(String printview, int minScore) {

		/*
		 * This method iterates over all the NE's in neSet and looks up their
		 * variants. The string 'printview' is 'p' (pair) or 'g' (group) and
		 * sets the output format.
		 */

		// create s-gram index from all words
		createSGramHM();

		// iterate over all words in the list and print their variants
		// int counter = 0;
		
		//for (String word : neSet) {
		int counter = 0;
		for(int i = 0; i < neSet.size(); i++){
			String word = neSet.get(i);
			String preStr = "";
			String sourceinf = "";
			if(restSet.size() > i){
				preStr = restSet.get(i);
			}
			if(sourceInfo.size() > i){
				sourceinf = sourceInfo.get(i);
			}
			// if(counter%100==0){System.err.print("["+counter+"]");}
			// counter++;
			// HashMap<Integer, Set<String>> res = doSGramComparison(word,
			// normalizeString(word));
			HashMap<Integer, Set<String>> res = doSGramComparison(word,
					normalizedHM.get(word));
			printMatches(word, preStr, sourceinf, res);
			//printMatches(word, res);
			cleanUp();
			counter++;
		}

	}

	private void cleanUp() {
		maxSGrams = 0;
		vars.clear();
	}

	//public void printMatches(String w, HashMap<Integer, Set<String>> results) {
	public void printMatches(String w, String preStr, String sourceinf, HashMap<Integer, Set<String>> results) {
		/*
		 * Main method to pass the results to STDOUT, in different ways.
		 */

		
		//	If showDoubles = false, we go over the results and remove all the homonyms.
		// 	@TODO it is stupid to only remove them at this stage, this should be done much earlier
		//	if showSource = true, we need to glue together all the sources from the doubles, e.g.
		//	God (100) (NG1.txt:2340), God (100) (NG2.txt:4500) > God (100) (NG1.txt:2340, NG2.txt:4500)
		
		HashMap<Integer, Set<String>> results2;
		if(!showDoubles){
			results2 = removeDoubles(results);
		}
		else{
			results2 = results;
		}
		
		
		// because the results are unsorted, we build an array of the keys
		// of the hashmap and sort this array.
		Object[] key = results.keySet().toArray();
		Arrays.sort(key);

		String typesW = "";
		if (printType) {
			Set<String> ty = typeHM.get(w);
			for (String t : ty) {
				typesW += t + " ";
			}
			if(typesW.length() > 1){typesW = typesW.substring(0, typesW.length()-1);}
			typesW += "\t";
		}

			
		if (!pairview) {
			//	for groupview, print out the preceding string of the current word (that is: all information
			//	that was in the file but was not used), followed by the word
			if(!preStr.equals("")){
				System.out.print(preStr+"\t");
			}
			System.out.print(typesW + w);
			if(showPhoneticTranscription){
				System.out.print(" [" + normalizedHM.get(w) +"]");
			}
		}
		int countResults = 0;
		for (int i = (key.length - 1); i >= 0; i--) {
			Set<String> res = results.get(key[i]);
			
			//the object 'key' now holds all sorted results per score. What not has been done is a
			//sorting of results within similar scores. E.g. 'Gemeente Helmond' and 'Helmond' can both
			//have scores of 100 for 'Helmond' (with 'gemeente' in the phontrans list), while the latter
			//is preferrable. 
			
			//System.err.println("\nressize: "+res.size());
			
			String[] sortedRes = sortSimilarScores(res, w);
			
			//System.err.println("ressize: "+res.size()+" sortedressize: "+sortedRes.length+"\n");
			for(int j = 0; j < sortedRes.length; j++){
			//for(String r : sortedRes){
			//for (String r : res) {
				if (nBest == -1 || (nBest != -1 && countResults < nBest)) {
					//System.err.println(">>"+sortedRes[j]+"<");
					
					String types = "";
					String r2 = sortedRes[j];
					// add type (which precedes the NE)
					if (printType) {
						Set<String> ty = typeHM.get(w);
						for (String t : ty) {
							types += t + " ";
						}
					}
					//show source: show the file and line number within that file of each NE
					//System.err.println(r);
					
					//remove the index number from the match
					int ind = sortedRes[j].lastIndexOf(" ");
					
					if(ind > 0){
						//	careful: if showDoubles = false, one variant (r2) can have multiple hits, e.g.
						//	Mulder !(80) 12345;23456;3233
						
						//	if printSource = false, we just remove all this, if printSource = true,
						//	we split the index-string and print all the sources
						
						r2 = sortedRes[j].substring(0, ind);
						
						if(showPhoneticTranscription){
							if(printScore){
								int r3 = r2.lastIndexOf(" (");
								r2 += " [" + normalizedHM.get(r2.substring(0,r3)) +"]";
							}
							else{
								r2 += " [" + normalizedHM.get(r2) +"]";
							}				
						}
						
						if (printSource){
							String[] sources = sortedRes[j].substring(ind+1).split(";");
							r2 += " (";
							for(int k = 0; k < sources.length; k++){
								//int num = Integer.parseInt(sortedRes[j].substring(ind+1));
								int num = Integer.parseInt(sources[k]);
								if(useLemma){
									//if(lemmaSourceInfo.size() <= num){
									//	System.out.println("(uselemma) woord: "+w+" var: "+r2+" sortedRes[j]:" +sortedRes[j]+" num: "+num+" lemmaSourceInfo.size: "+lemmaSourceInfo.size());
								//	}
									//r2 += " ((a " + num +"/"+lemmaSourceInfo.size()+" )) ";
									r2 += (new File(lemmaSourceInfo.get(num))).getName();
								}
								else{
									//if(sourceInfo.size() <= num){
									//	System.out.println("(!uselemma) woord: "+w+" var: "+r2+" sortedRes[j]:" +sortedRes[j]+" num: "+num+" sourceInfo.size: "+sourceInfo.size());
									//}
									//r2 += " ((b " + num +"/"+sourceInfo.size()+" )) ";
									r2 += (new File(sourceInfo.get(num))).getName();
								}
								//r += "(" + sour + ")";
								if(k < sources.length-1){
									r2 += ";";
								}
							}
							r2 += ")";
						}
					}
					if (!pairview) {
						System.out.print("\t" + types + r2);
					} 
					else {
						if(!preStr.equals("")){
							System.out.print(preStr+"\t");
						}
						if(showPhoneticTranscription){
							System.out.println(typesW + w + " [" + normalizedHM.get(w) +"]\t" + types + r2);
						}
						else{
							System.out.println(typesW + w + "\t" + types + r2);
						}
					}
				}
				countResults++;
			}
		}
		if(pairview && key.length == 0){
			if(!preStr.equals("")){
				System.out.print(preStr+"\t");
			}
			//System.out.println(typesW + w);
			if(showPhoneticTranscription){
				System.out.println(typesW + w + " [" + normalizedHM.get(w) +"]");
			}
			else{
				System.out.println(typesW + w);
			}
		}
		if (!pairview) {
			System.out.println("");
		}

	}

	
	private List<String> printMatchesToList(String w, HashMap<Integer, Set<String>> results) {
		List<String> resultsFinal = new ArrayList<String>();
		Object[] key = results.keySet().toArray();
		//Arrays.sort(key);
		for (int i = (key.length - 1); i >= 0; i--) {
			Set<String> res = results.get(key[i]);
			for (String r : res) {
				//we put all the final results in the list
				resultsFinal.add(r);
			}
		}
		return resultsFinal;
	}
	
	
	public void loadNEs(String file, boolean isLemma) {

		/*
		 * This method loads the NEs from the file 'file' in the set
		 * neSet<String>. The file is expected to have an NE on each line.
		 */

		
		
		
		if(verbose){System.err.print("Loading named entities from file " + file + " ... ");}
		int linenumber = 0;
		//int lemmalinenumber = 0;
		
		if(!isLemma){
			//Only the basename of the file is stored to avoid troubles with relative and absolute pathnames,
			//but this has the consequence that we can only handle unique filenames, even if they are in 
			//different directories
			File fi = new File(file);
			String filebasename = fi.getName();
			fileOnsetHM.put(filebasename, neCounter);
			//System.err.println("Loading file to fileOnsetHm: "+filebasename+"=>"+neCounter);
		}
		
		try {
			FileReader doc = new FileReader(file);
			BufferedReader buff = new BufferedReader(doc);
			boolean eof = false;
			while (!eof) {
				String line = buff.readLine();
				boolean isPer = false;
				if (line == null) {
					eof = true;
				} else {
					String type = "";
					linenumber++;
					//if(!isLemma){linenumber++;}
					//if(isLemma){lemmalinenumber++;}
					//	integers typeTab and neTab tell us where the relevant information is,
					//	we store the preceding info and print this out later with the results
					
					//clean up the string: we remove leading and trailing whitespace (and tabs),and double spaces within the string
					//line = cleanUpString(line);
					
					//	we split the line on tabs 
					String[] parts = line.split("\\t");
					
					//	set the correct typetab, either for lemmata or ne
					int thisTypeTab = typeTab;
					if(isLemma){ thisTypeTab = lemmaTypeTab;}
					
					int thisNETab = neTab;
					if(isLemma) { thisNETab = lemmaNeTab; }
					
					if(thisTypeTab >= 0){
						if(parts.length <= thisTypeTab){
							System.err.println("Error reading line " + line + "\nTab index mismatch. Nr. of tabs on this line: " + parts.length + " (lemma)typetab: "+ thisTypeTab);
						}
						type = parts[thisTypeTab];
						if (type.equals("PER")) {
							isPer = true;
						}
					}
					//	we store all information preceding the first tab with either type- or ne -info
					String restInfo = "";
					int mintab = thisTypeTab;
					if(thisNETab < mintab || mintab < 0){mintab = thisNETab;}
					for(int i = 0; i < mintab; i++){
						restInfo += parts[i];
						if(i < (mintab - 1)){restInfo += "\t";}
					}
					restSet.add(restInfo);
					
					
					//	we store the file and local line number as well. They are linked by their index
					//	in neSet or lemmaSet
					if(printSource){
						//System.err.println("storing sourceInfo for file "+file);
						if(!isLemma){
							sourceInfo.add(file + ":" + Integer.toString(linenumber));
							//System.err.println("linenumber: "+ linenumber +" sourceInfo.size(): "+sourceInfo.size());
						}
						if(isLemma){
							lemmaSourceInfo.add(file + ":" + Integer.toString(linenumber));
							//System.err.println("linenumber: "+ linenumber +" lemmaSourceInfo.size(): "+lemmaSourceInfo.size());
						}
					}
					
					//	now we go on with the actual ne, which we pass to String line:
					//System.err.println("line: " + line);
					//line = line.replaceAll("^\\W", "");
					//System.err.println("partslength: " + parts.length + " netab: " + neTab);
					if(parts.length <= thisNETab){
						System.err.println("Error reading line " + line + "\nTab index mismatch. Nr. of tabs on this line: " + parts.length + " (lemma)netab: "+thisNETab);
					}
					line = parts[thisNETab];
					line = cleanUpString(line);
					/*
					if (hasType) {
						// file has format "TYPE NE", find the first whitespace
						// and get the substring from that position on.
						int i = line.indexOf(" ");
						if (i >= 0) {
							type = line.substring(0, i);
							type = type.replaceAll("\\W", "");	//just to be sure, could happen with funny coding
							if (type.equals("PER")) {
								isPer = true;
							}
							// remove TYPE from the string
							line = line.substring(i + 1);
						}
					}*/
					if (!line.equals("")) {
						nonUniqueCounter++;
						String filteredLine = line;
						if (PERFilter) {
							if (!useType || useType && isPer) {
								filteredLine = getPERstructure(line);
							}
						}
						/*
						if(!useType){
							//if useType = false, all ne's get the same type.
							if (type.equals("")){
								type = "GENERIC";
							}
						}
						*/
						if (useType) {
							Set<String> typeSet;
							if(isLemma){typeSet = lemmaTypeHM.get(line);}
							else{typeSet = typeHM.get(line);}
							if (typeSet == null) {
								typeSet = new HashSet<String>();
								typeSet.add(type);
								if(isLemma){lemmaTypeHM.put(line, typeSet);}
								else{typeHM.put(line, typeSet);}
							} 
							else {
								if (!typeSet.contains(type)) {
									typeSet.add(type);
									if(isLemma){lemmaTypeHM.put(line, typeSet);}
									else{typeHM.put(line, typeSet);}
								}
							}
						}
						
						//the ignoreWordsWithTag indicates words that should not be taken
						//into consideration in matching, e.g. "%Baron% van Tengnagel %Markies% De Cantecleir" > "van Tengnagel De Cantecleir"
						if(!ignoreWordsWithTag.equals("")){
							filteredLine = removeIgnorableParts(line);
						}
						//System.err.println("###Storing "+line+"=>"+normalizeString(filteredLine)+" in normalizedHM");
						normalizedHM.put(line, normalizeString(filteredLine));
						
						if (isLemma) {
							//System.err.println("LEMMA. Adding >" + line+"<");
							lemmaSet.add(line);
						} else {
							neSet.add(line);
							//fileInfoHM.put(neCounter, file);
							neCounter++;
						}
					}
				}
			}
			buff.close();
		} catch (IOException e) {
			System.err.println("Woops. Error reading input file. " + e.toString());
		}
		if(verbose){
			if (isLemma) {
				System.err.print("done (" + lemmaSet.size() + " NEs).\n");
			} else {
				System.err.print("done (" + neSet.size() + " NEs).\n");
			}
		}
		if(!isLemma){
			File fi = new File(file);
			String filebasename = fi.getName();
			fileOffsetHM.put(filebasename, neCounter);
		}
	}

	private HashMap<Integer, Set<String>> doSGramComparison(String ne, 
			String ne_normalized) {

		// This method calls the main matching method.

		// find matches and put them in a hashmap with format "NE" =>
		// "shared s-grams"
		
		//System.err.println("find matches...");
		HashMap<String, Integer> results = findSGramMatches(ne, ne_normalized);
		//System.err.println("find matches... done");
		// TreeMap<String, Integer> resultsS = findSGramMatches(ne_normalized);

		// convert the hashmap 'results' into the hashmap 'resultsFlipped,
		// with format "normalized number of shared s-grams" => {NE1, NE2, NE3
		// ...}
		HashMap<Integer, Set<String>> resultsFlipped = convertHM(results, ne);

		return resultsFlipped;

	}

	private HashMap<String, Integer> findSGramMatches(String ne,
			String ne_normalized) {

		/*
		 * Break String ne into s-grams. We do this three times, for s=0
		 * (=n-gram), s=1 and s=2. Then collect the variants from the hashmap
		 * sGramHM by using the found s-grams as keys: the values are the
		 * variants. We store these in the global HashMap 'vars' with the number
		 * of shared s-grams as keys.
		 */
		
		for (int i = 0; i < 3; i++) {
			Set<String> sg = sGramBuilder(ne_normalized, N, i);
			collectVariantsFromsGramHM(ne, sg);
			maxSGrams += sg.size();
			sg.clear();
		}
		
		//System.err.println("## "+vars);
		
		return vars;
	}

	private void collectVariantsFromsGramHM(String s, Set<String> set) {
		for (String key : set) {
			if (sGramHM.containsKey(key)) {
				Set<String> v = sGramHM.get(key);
				for (String va : v) {
					//System.err.println("\tchecking for " +va);
					//System.err.println(va);
					//each word in the sgramHM has its index connected to it, e.g. jansen#12345.
					//we strip this off for comparison first
					String va2 = va;
					int ind = va.lastIndexOf("#");
					va2 = va.substring(0, ind);
					//int n = Integer.parseInt(va.substring(ind+1));
					//if(lemmaSourceInfo.get(n).equals(sourceinf)){
					//	System.err.println(va + " " + va2 + " " + n + " " + lemmaSourceInfo.get(n)+" "+sourceinf);
					//}
					// skip the word we're actually looking for if we're
					// comparing
					// words from one list (that is, if we do not use a lemma
					// file)
					//System.err.println("toevoegen van " + va + " aan variantenlijst.");
					if (useLemma || (!useLemma && !va2.equals(s))) {
						if (vars.containsKey(va)) {
							int freq = vars.get(va);
							freq++;
							vars.put(va, freq);
						} else {
							vars.put(va, 1);
						}	
					}
				}
			}
		}
	}

	private void createSGramHM(LinkedList<String> set) {

		/*
		 * This method builds the s-gram hashmap from all the NE's loaded from
		 * the input file and stored in neSet. The s-grams are built on the
		 * basis of normalized version of the NE's.
		 * 
		 * Building is done by calling the method 'sGramBuilder()' three times,
		 * with different settings for the s(kip): 0, 1, 2. sGramBuilder()
		 * returns a set of sgrams for the particular NE, and these are all
		 * added to the main hashmap 'sGramHM'.
		 */

		if (verbose) {
			System.err.print("Building s-gram index ... ");
		}
		int linenum = 0;
		
		for (String s : set) {
			String s_normalized = normalizedHM.get(s);
			String s2 = s;
			//we add the unique index nr to each string, so to be able to trace
			//back homonyms
			s2 += "#"+Integer.toString(linenum);
			if (s_normalized.length() > 1) {
				Set<String> sgA = sGramBuilder(s_normalized, N, 0);
				addSetToSGramHM(s2, sgA);
				Set<String> sgB = sGramBuilder(s_normalized, N, 1);
				addSetToSGramHM(s2, sgB);
				Set<String> sgC = sGramBuilder(s_normalized, N, 2);
				addSetToSGramHM(s2, sgC);
			}
			linenum++;
		}
		if (verbose) {
			System.err.print("done (" + sGramHM.size() + " unique s-grams).");
		}
		//System.err.println("total number of lines: "+linenum);
		if (verbose) {
			System.err.println("\n");
		}
	}

	private void createSGramHM() {

		/*
		 * This method builds the s-gram hashmap from all the NE's loaded from
		 * the input file and stored in neSet. The s-grams are built on the
		 * basis of normalized version of the NE's.
		 * 
		 * Building is done by calling the method 'sGramBuilder()' three times,
		 * with different settings for the s(kip): 0, 1, 2. sGramBuilder()
		 * returns a set of sgrams for the particular NE, and these are all
		 * added to the main hashmap 'sGramHM'.
		 * 
		 * In order to keep track of the source of each word, we add the sourceinfo index to each
		 * word: 'word#index'. This is needed because words are not unique.
		 * 
		 */

		System.err.print("Building s-gram index ... ");
		for (String s : neSet) {
			String s_normalized = normalizedHM.get(s);
			if (s_normalized.length() > 1) {
				Set<String> sgA = sGramBuilder(s_normalized, N, 0);
				addSetToSGramHM(s, sgA);
				Set<String> sgB = sGramBuilder(s_normalized, N, 1);
				addSetToSGramHM(s, sgB);
				Set<String> sgC = sGramBuilder(s_normalized, N, 2);
				addSetToSGramHM(s, sgC);
			}
		}
		System.err.print("done (" + sGramHM.size() + " unique s-grams).");
		System.err.println("\n");
	}

	private void addSetToSGramHM(String word, Set<String> set) {

		/*
		 * This method adds the s-grams of 'set' to the main sGramHM. S-grams
		 * become keys, the word 'word' from which the s-grams are derived is
		 * added to a set as value to this key.
		 * 
		 * the unique wordindex is added to String word, e.g.: jansen#12342.
		 * Lemmata and ne's have different indexes. 
		 */

		for (String k : set) {
			Set<String> sgr = sGramHM.get(k);
			if (sgr == null) {
				sgr = new HashSet<String>();
				sgr.add(word);
				sGramHM.put(k, sgr);
			}
			if (!sgr.contains(word)) {
				sgr.add(word);
				sGramHM.put(k, sgr);
			}
		}
	}

	private Set<String> sGramBuilder(String str, int n, int s) {

		/*
		 * This method returns a set of s-grams from string 'str'. The s-grams
		 * are constructed by using the parameters n and s.
		 * 
		 * s-grams are n-grams in which the n characters are not necessarily
		 * adjacent (s: 'skip'). s(a, b) => a: number of characters, b: number
		 * of skipped chars e.g. Leiden with s(2,1): Li, ed, ie, dn
		 * 
		 * We use s(2, 0), s(2, 1) and s(2, 2), so basically digrams with
		 * different skips, this should make it robust against spelling
		 * variations.
		 * 
		 * Side note: Jarvelin et al. use s-grams in their paper, but somewhat
		 * differently then is done here. From the two words that are compared,
		 * they combine all the s-grams. They then check the presence of each
		 * s-gram in each word in a matrix. If the s-gram is present in both
		 * words, score=0, if s-gram is absent in one of the words, score=1. The
		 * sum of the scores is a measure for the distance.
		 * 
		 * e.g. for n=2 and s=0, Leiden and Leyden give the following treeset:
		 * {Le ei ey id yd de en} Leiden: 1 1 0 1 0 1 1 Leyden: 1 0 1 0 1 1 1
		 * dist: 0 +1 +1 +1 +1 +0 +0 = 4
		 */

		Set<String> gramSet = new HashSet<String>();
		if (str.length() > 0) {
			// we use a beginning and end tag for each word for supposed better
			// matching
			str = "<" + str + ">";
			//int la = str.length() - (n - 1) - s;
			for (int i = 0; i < (str.length() - (n - 1) - s); i++) {
				gramSet.add(str.substring(i, i + 1)
						+ str.substring((s + i + 1), (s + i + 2)));
			}
		}
		return gramSet;
	}

	private HashMap<Integer, Set<String>> convertHM(
			HashMap<String, Integer> hm, String ne) {

		/*
		 * This method takes the incoming hm which has <NE, shared s-gram
		 * frequency> and returns a hm2 <normalized frequency, [NE1, NE2, NE3,
		 * ...]>.
		 * 
		 * The shared s-gram frequency is normalized by multiplying it with a
		 * value v representing the difference in string length, and then turned
		 * into a percentage. e.g. 'Jan' and 'Frank' will have v = ( 3 / 5 )
		 * 
		 * The incoming hm holds all the possible matches, while the outgoing
		 * hm2 is limited to only those matches that have a normalized score >=
		 * minScore.
		 */
		
		//System.err.println("Converting HM...");
		for (Iterator<Integer> varIterator = hm.values().iterator(); varIterator
				.hasNext();) {
			if (varIterator.next() <= minSGramCutOffValue) {
				varIterator.remove();
			}
		}

		HashMap<Integer, Set<String>> hm2 = new HashMap<Integer, Set<String>>();

		//System.out.println("Comparing word with variants. word = "+ne+" num. of variants: "+hm.size());
		
		for (Map.Entry<String, Integer> e : hm.entrySet()) {
			String s = e.getKey();
			int f = e.getValue();
			// if(f == 1){
			
			//System.err.println("\tw="+ne+" var="+s+" f="+f);
			
			// }
			// compare the length of word s with the word ne and use the length
			// difference as a weight in the actual s-gram frequency.
			// we use the normalized string when comparing string length,
			// so 's-Hage and sHage have the same length

			// String s2 = normalizeString(s);
			// String ne2 = normalizeString(ne);
			String s2 = s;
			int num = -1;
			//if(printSource){
			int ind = s.lastIndexOf("#");
			s2 = s.substring(0, ind);
			num = Integer.parseInt(s.substring(ind+1));
			//}
			//System.err.println("Converting scores for word "+ne+"(normalized: "+normalizedHM.get(ne)+") and var "+s+"(normalized: "+normalizedHM.get(s2)+")");
		
			double v = (0.0 + Math.min(normalizedHM.get(s2).length(),
					normalizedHM.get(ne).length()))
					/ (0.0 + Math.max(normalizedHM.get(s2).length(),
							normalizedHM.get(ne).length()));
			f = (int) (f * v);
			// get percentages by dividing it by the maximum possible value
			// 'maxSGrams'
			f = (f * 100) / maxSGrams;
			
			f -= applyDiffInitialPunishment(normalizedHM.get(ne), normalizedHM.get(s2));
			//if(showPhoneticTranscription){System.err.println("ne: "+ne +" ("+normalizedHM.get(ne) + "); s: "+ s2 +" (" + normalizedHM.get(s2)+"); v=" + v+"; f="+f);}

			if (f >= minScore) {
				//System.out.println("Good variants:" +ne+" (norm:"+ normalizedHM.get(ne)+ ") and: " + s+" (norm:" + normalizedHM.get(s) + ") score: " + f +
				//" " + v );
				if (hm2.containsKey(f)) {
					Set<String> val = hm2.get(f);
					if (!val.contains(e)) {
						if (printScore) {
							s2 += " (" + f + ")";
						}	
						//if(printSource){
							if(num != -1){
								s2 += " " + num;
							}
						//}
						//System.err.println("s2: "+s2);
						val.add(s2);
						//System.out.println("\texisting score. Added set val: "+val);
						hm2.put(f, val);
					}
				} else {
					Set<String> val = new HashSet<String>();
					if (printScore) {
						s2 += " (" + f + ")";
					}
					//if(printSource){
						if(num != -1){
							s2 += " " + num;
						}
					//}
					//System.err.println("s2: "+s2);
					val.add(s2);
					//System.out.println("\tNew score. Added set val: "+val);
					hm2.put(f, val);
				}
			}
		}
		//System.out.println("Done comparing word with variants. word = "+ne+" num. of variants: "+hm2.size());
		//System.err.println("Converting HM... done.");
		//System.err.println("##"+hm2);
		return hm2;
	}

	private HashMap<Integer, Set<String>> filterResults(String w,
			HashMap<Integer, Set<String>> results) {

		/*
		 * 	Iterate over the results set and remove those words that do not have
		 * 	a matching type with the String 'w'.
		 * 	If we are not working with lemmata, we just compare the types
		 * 	in typeHM. If we are dealing with lemmata, we compare the type of the ne
		 * 	in typeHM with that in lemmaTypeHM.
		 */

		//System.err.println("Filtering results..");
		//System.out.println("this word has " + results.size() + " sets of variants:");
		//System.out.println("\t"+results);
		Set<String> wordTypes = typeHM.get(w);
		boolean isPER = false;
		Collection<Set<String>> c = results.values();
		Iterator<Set<String>> resultIterator = c.iterator();
		while(resultIterator.hasNext()){
		
		//for (Iterator<Set<String>> resultIterator = results.values().iterator(); resultIterator.hasNext();) {
			//System.out.println("\tresults size: "+results.size());
			Set<String> vars = resultIterator.next();
			//System.out.println("\tvariants in set: "+vars);
			Iterator<String> varIterator = vars.iterator();
			while (varIterator.hasNext()) {
				String var = (String) varIterator.next();
				//System.out.println("\tvar "+var);
				boolean matchingType = false;

				//if (printSource){
					int ind = var.lastIndexOf(" ");
					var = var.substring(0, ind);
				//}
				if (printScore) {
					ind = var.lastIndexOf("(");
					var = var.substring(0, ind - 1);
				}
				Set<String> resultTypes;
				if(useLemma){resultTypes = lemmaTypeHM.get(var);}
				else{resultTypes = typeHM.get(var);}
				//System.out.println("resultTypes: "+resultTypes);
				for (String wT : wordTypes) {
					//System.out.println("\t\twordtype: "+wT);
					if (wT.equals("PER")) {
						isPER = true;
					}
					for (String rT : resultTypes) {
						//System.out.println("\t\tvartype: "+rT);
						//System.out.println("word: " + w + " wordType: "+wT+" varType: "+rT);
						if (wT.equals(rT)) {
							matchingType = true;
						}
					}
				}

				/*
				 * If !match, we remove 'var' from the set
				 */
				if (!matchingType) {
					//System.out.println("\tRemoving var "+var +" from the list of results.");
					varIterator.remove();
				}
				//else{
					//System.out.println("\tThis variant type is ok.");
				//}

			}
			//System.out.println("For vars "+vars+" we are done. now do per-check.");
			//System.out.println("\tresults size: "+results.size());
			//System.out.println("\tvariants in this results set: "+vars);
			
			// with the filtered group, we go over the PERs
			// now we look for PERs. In the perfilter, we have stored the
			// surname of the PERs and matched on that.
			// if there is a match on the last name, we now need to see if the
			// match also applies for the firstnames
			//System.err.println("PERFILTER? " +w);
			
			//System.err.println("\t\t\tcheckin.." + useType + " " + PERFilter + " " +isPER+" for word "+w);
			
			if (vars.size() > 0 && PERFilter) {
				if (!useType || useType && isPER) {
					//System.err.println("###startin checkin..");
					// royalty check for word w
					if (royaltyHM.containsKey(w)) {
						String roy1 = "";
						String roy2 = "";
						// System.err.println("Royalty name: "+w);
						// matching for strings first ('tweede', 'derde')

						matcher = patternStringRegex.matcher(royaltyHM.get(w)
								.toLowerCase());
						if (matcher.find() && !matcher.group(0).equals("")) {
							// System.out.println("\tROYALTY MATCH STRING: "
							// +matcher.group(0));
							roy1 = string2romanHM.get(matcher.group(0));
						} else {
							matcher = patternRomanRegexShort.matcher(royaltyHM
									.get(w).toLowerCase());
							if (matcher.find() && !matcher.group(0).equals("")) {
								// System.out.println("\tROYALTY MATCH NUMBER: "
								// +matcher.group(0));
								roy1 = matcher.group(0);
							}
						}

						// now get the royal part of the vars.

						//for (resultIterator2 = results.values().iterator(); resultIterator.hasNext();) {
							//Set<String> vars2 = resultIterator2.next();
							Iterator<String> varIterator2 = vars.iterator();
							while (varIterator2.hasNext()) {
								String var = (String) varIterator2.next();
								// System.err.println("comparing types of word "+w+" and var "+var);
								//boolean matchingRoyalty = false;

								if (printScore) {
									int ind = var.lastIndexOf("(");
									var = var.substring(0, ind - 1);
								}
								// System.out.println("Checking PER names "+w+" "+var);
								if (!royaltyHM.containsKey(var)) {} 
								else {
									// both are royalty, and their first names
									// match. Now check the latter part of the
									// name,
									// which is stored in the royaltyHM

									// matching for strings first ('tweede',
									// 'derde')
									matcher = patternStringRegex
											.matcher(royaltyHM.get(var)
													.toLowerCase());
									if (matcher.find()
											&& !matcher.group(0).equals("")) {
										// System.out.println("\tROYALTY MATCH STRING: "
										// +matcher.group(0));
										roy2 = string2romanHM.get(matcher
												.group(0));
									} else {
										matcher = patternRomanRegexShort
												.matcher(royaltyHM.get(var)
														.toLowerCase());
										if (matcher.find()
												&& !matcher.group(0).equals("")) {
											// System.out.println("\tROYALTY MATCH NUMBER: "
											// +matcher.group(0));
											roy2 = matcher.group(0);
										}
									}
									// we now have both roy strings. If they are
									// equal, we have a go, otherwise we remove
									// the string
									if (!roy1.equals(roy2)) {
										varIterator2.remove();
									}
								}

							}
						}
						else if (initialsHM.containsKey(w)) {
							//}
							// check if the main word has given names. If not (e.g.
							// 'Jansen'), than all matches are ok
								
							
							// the main word has given names, which are stored by
						// their initials:
						// Jan Piet van de Ven > initials: JP

						//for (resultIterator = results.values().iterator(); resultIterator.hasNext();) {
							Iterator<String> varIterator2 = vars.iterator();
							while (varIterator2.hasNext()) {
							//vars = resultIterator.next();
							//varIterator = vars.iterator();
							//while (varIterator.hasNext()) {
								String var = (String) varIterator2.next();
								//System.err.println("\tinitials: comparing types of word "+w+" and var "+var + " on initials");
								boolean matchingPER = false;

								if (printScore) {
									int ind = var.lastIndexOf("(");
									var = var.substring(0, ind - 1);
								}
								// System.out.println("Checking PER names "+w+" "+var);
								// check the initials of the variant.
								// no initials: ok
								// initials? Check for similarity.
								if (initialsHM.containsKey(var)) {
									// var has initials, e.g. 'c. de vry' or
									// 'christian de vry'
									// this is a potential match if the main
									// word w has matching initials
									//System.out.println("\tvar "+var+" has initials: "+initialsHM.get(var)+" and word "+w+" has initials "+initialsHM.get(w));
									if (initialsHM.get(var).equals(
											initialsHM.get(w))) {

										// now we have two PERs of which the
										// surnames have matched, and of which
										// the initials of the given names match
										// too.
										// check for the final exception:
										// Jan Jansen <> Joris Jansen (which
										// have matching initials for the given
										// names)
										// this type does not match.
										//System.out.println("\tword: "+hasInitialsHM.containsKey(w)+" var: "+hasInitialsHM.containsKey(var));
										if (!hasInitialsHM.containsKey(w)
												&& !hasInitialsHM
														.containsKey(var)) {
										} else {
											//System.out.println("\tMatch!! for words "+w+" "+var);
											matchingPER = true;
										}
									}
								} 
								else {
									// the var has no initials, e.g. 'de vry'.
									// In this case, any match with the main
									// word is considered okay.
									//System.out.println("\tvar "+var+" has no initials.");
									matchingPER = true;
								}

								if (!matchingPER) {
									//System.out.println("\tRemoving var "+var +" from the list of results for word "+w);
									varIterator2.remove();
								}

							}
						//}
					}
				}
			}
			//System.out.println("Done. This word now has " + results.size() + " variants:");
			//System.out.println("\t"+results);
		}
		
		//finally, results could hold empty Sets, since the variants in them are taken out, but the Sets themselves remain
		Collection<Set<String>> co = results.values();
		Iterator<Set<String>> resItr = co.iterator();
		while(resItr.hasNext()){
			Set<String> vars = resItr.next();
			if(vars.size() == 0){
				resItr.remove();
			}
		}
		//System.out.println("Really done. This word now has " + results.size() + " variants:");
		//System.err.println("Filtering results.. done");
		return results;
	}

	//OLD
	private HashMap<Integer, Set<String>> filterOnInitials(String w,
			HashMap<Integer, Set<String>> results) {

		/*
		 * This method is meant to match PER-names with initials and matching
		 * first names, e.g. Jan van Hout <> J. van Hout.
		 */

		//System.err.println("\t\tparts...");

		String[] partsW = partsHM.get(w);
		for (Iterator<Set<String>> resultIterator = results.values().iterator(); resultIterator
				.hasNext();) {
			Set<String> vars = resultIterator.next();
			Iterator varIterator = vars.iterator();
			while (varIterator.hasNext()) {
				String var = (String) varIterator.next();
				// System.err.println("comparing parts of word "+w+" and var "+var);
				if (printScore) {
					int ind = var.lastIndexOf("(");
					var = var.substring(0, ind - 1);
				}
				Set<String> ty = typeHM.get(var);
				boolean go = false;
				if (useType) {
					for (String t : ty) {
						if (t.equals("PER")) {
							go = true;
						}
					}
				} else {
					go = true;
				}
				if (go) {
					for (String partsV : partsHM.get(var)) {
						// System.err.println("partsV: " + partsV);
					}
				}
			}
		}

		return results;
	}

	private String[] sortSimilarScores(Set<String> results, String ne){
		
		/*	This method takes a Set with results that have similar scores.
		 * 	It sorts the results based on a final similarity check
		 * 	with the NE it is matched with, based on (1) a perfect match, and (2)
		 * 	difference in string length
		 */
		
		String[] sortedRes = new String[results.size()]; 
		sortedRes[0] = "";
		//(1) look for perfect matche(s). They are stored in index 0, and if there's
		//more than one perfect match, in indexes 1, 2, ... etc.
		int len = ne.length();
		int ind = 0;
		Iterator<String> resItr = results.iterator();
		while(resItr.hasNext()){
			String v = resItr.next();
			int t = v.lastIndexOf(" ");
			String v2 = v.substring(0, t);
			if(printScore){
				//remove the score
				t = v2.lastIndexOf(" ");
				v2 = v2.substring(0, t);
			}
			//System.err.println("\tresult: >" + v2 + "< ne: >"+ne+"<");
			if(v2.equals(ne)){
				//System.err.println("\t\tmatch: !");
				sortedRes[ind] = v;
				ind++;
				resItr.remove();
			}
			
		}
		//(2) if relevant, check rest and sort by difference in string length
		if(results.size() > 0){
			//System.err.println("\tchecking leftovers");
			String[] strlengthdiffs = new String[results.size()];
			int counter = 0;
			
			//make new array to which we add string length: "4#Piet"
			for(String r : results){
				int t = r.lastIndexOf(" ");
				String r2 = r.substring(0, t);
				strlengthdiffs[counter] = Integer.toString(Math.abs(r2.length() - len)) + "#" +r;
				counter++;
			}
			//sort alphabetically
			Arrays.sort(strlengthdiffs);
			//remove number and add to sortedRes
			for(int i = 0 ; i < strlengthdiffs.length; i++){
				//strlengthdiffs[i] = strlengthdiffs[i].substring(strlengthdiffs[i].indexOf("#")+1);
				sortedRes[i+ind] = strlengthdiffs[i].substring(strlengthdiffs[i].indexOf("#")+1);
				//System.err.println("\t\tAdding " + sortedRes[i+ind] + " to sortedRes.");
			}
		}
		/*
		System.err.println("\nKLAAR.");
		
		for(int i = 0 ; i < sortedRes.length; i++){
			System.err.println(i +" "+sortedRes[i]);
		}
		*/
		return sortedRes;
	}
	
	
	
	private String normalizeString(String sOld) {

		/*
		 * This method normalizes strings in a series of steps.
		 */

		// change to lowercase (e.g. Zuid-Holland > zuid-holland)
		String sNew = sOld.toLowerCase();
		
		//look for comma's, and flip the order of words: dijk, jan van > jan van dijk
		//int comma  = sNew.indexOf(",");
		//if(comma > -1 && comma != sNew.length()){sNew = convertNEsWithCommas(sNew, comma);}
		
		//System.out.println("To lc: "+sNew);
		sNew = Normalizer.normalize(sNew, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		
		//String regexFinalNonWordChars = "\\W";
		//sNew = sNew.replaceAll(regexFinalNonWordChars, "");
		
		//sNew = Normalizer.normalize(sNew, Normalizer.Form.NFD);
		/*
		//we remove words listed in ignoreWords
		if(ignoreWords.length() > 0){
			sNew = removeIgnoreWords(sNew);
		}
		
		// remove the genetive `s | 's if flag removeGenetiveS is true
		if (removeGenetiveS) {
			sNew = removeGenetiveS(sNew);
		}

		if (removeFinalS) {
			sNew = removeFinalS(sNew);
		}
		
		if (removeFinalN) {
			sNew = removeFinalN(sNew);
		}
		*/
		
		//we first remove non-word characters from the beginning and end of the string,
		//note that this leaves the whitespace inside the string, which enables us
		//to look for certain words (e.g. 'sint') with the phonetic transcription rules.
		//after these rules have been applied, we remove the inside whitespace as well.
		
		//String regex = "\\-|\\s|\\.|,|'|`|’|‘|\"";
		
		//String regexFinalNonWordChars = "\\W$";
		//sNew = sNew.replaceAll(regexFinalNonWordChars, "");
		
		//System.out.println("non word chars at word end: "+sNew);
		
		
		//String regexInitialNonWordChars = "^\\W";
		//sNew = sNew.replaceAll(regexInitialNonWordChars, "");
		
		//System.out.println("Halfway: "+sNew);
		
		// remove diacritics (e.g. abessinië > abessinie). NB only works for
		// Java 1.6 and up
		//sNew = Normalizer.normalize(sNew, Normalizer.Form.NFD);

		//sNew = pattern.matcher(sNew).replaceAll("");
		if (sft != null)
		sNew = sft.convertToSemiFoneticTranscription(sNew);
		//System.out.println("Almost done: "+sNew);
		//String regexGeneralNonWordChars = "\\W";
		//sNew = sNew.replaceAll(regexGeneralNonWordChars, "");
		
		// String regex = "[\\-|'|`|’|‘|\\s|\\.]";
		// String regex = "\\-|\\s|\\.|,|'|";

		/*
		 * // remove dashes (e.g. 'Zuid-Holland' > 'zuidholland') sNew =
		 * sNew.replaceAll("\\-", "");
		 * 
		 * // remove all apostrophes (e.g. new'york > newyork) sNew =
		 * sNew.replaceAll("'", "");
		 * 
		 * // remove all apostrophes (e.g. new`york > newyork) sNew =
		 * sNew.replaceAll("`", "");
		 * 
		 * // remove all apostrophes (e.g. new`york > newyork) sNew =
		 * sNew.replaceAll("’", "");
		 * 
		 * // remove all apostrophes (e.g. new`york > newyork) sNew =
		 * sNew.replaceAll("‘", "");
		 * 
		 * 
		 * // remove all whitespace (e.g. new york > newyork) sNew =
		 * sNew.replaceAll("\\s", "");
		 */

		//if(!sNew.equals(sOld)){System.out.println("old >" + sOld +"< new >"+sNew+"<");}
		return sNew;
	}

	//OLD
	private String convertNEsWithCommas(String s, int pos){
		
		/* 	This method takes a string with a comma and the position of the comma
		 * 	and returns the converted string without the comma.
		 * 	E.g. 'b, a' > 'a b'
		 * 
		 * 	It assumes that pos > -1 and that the comma is not at the end of the string 
		 */
		
		String s1 = s.substring(0, pos);
		String s2 = s.substring(pos+1, s.length());
		
		return (s1+s2);
	}

	private int applyDiffInitialPunishment(String a, String b){
		int punishment = 0;
		if(a.charAt(0) != b.charAt(0)){
			punishment = diffInitialPunishment;
		}
		return punishment;
	}
	
	private String getPERstructure(String line) {

		/*
		 * The PERFilter tries to improve person matching by taking the initials
		 * of all words of the entire string except for the last word, e.g.: Jan
		 * van Hout > JvHout, Adriaan Janszoon van de Ven > AJvdVen.
		 * 
		 * For a subset of names, it makes sense to use the first word instead:
		 * Willem den Tweeden, Willem den II > WillemdT, WillemdII
		 */

		//System.err.println("in: "+line);
		// the incoming lines are not normalized, so we start by applying
		// some simple regexes:
		line = line.replaceAll("^\\s+", "");
		line = line.replaceAll("\\s+$", "");
		// replace dashes surrounded by whitespace: Fred Sta - rink > Fred
		// Starink
		line = line.replaceAll("\\s+-\\s+", "");
		// remove all nonword characters from begin and end of string: Jan
		// Pietersen ... " > Jan Pietersen
		line = line.replaceAll("\\W+$", "");
		line = line.replaceAll("^\\W+", "");

		// initials could be one string, e.g. 'W.W', instead of 'W. W'
		// add the whitespaces
		String filteredName = line;
		String oldLine = line;
		line = oldLine.replaceAll("\\.(\\S)", "\\. $1");
		//if(!oldLine.equals(line)){System.err.println("\t\t"+line+" "+oldLine);}

		// split the string in parts on the whitespace
		String[] parts = line.split("\\s");
		int arraylength = parts.length;
		boolean nameHasInitials = false;
		// System.err.println("PER: >>"+line
		// +"<< last word: "+parts[parts.length-1]);

		boolean exception = false;
		//System.err.println(line+" "+parts[parts.length-1]);
		if (parts[parts.length - 1].toLowerCase().matches(romanRegexShort)) {
			//System.err.println("\t\t\tFound exception in stringpart "+parts[parts.length-1]);
			exception = true;
		}
		if (!exception) {
			if (parts[parts.length - 1].toLowerCase().matches(stringRegex)) {
				// System.err.println("Found exception in stringpart "+parts[parts.length-1]);
				exception = true;
			}
		}

		if (exception) {
			// System.err.println("\tThis string is an exception: "+line);
			// for these cases ('Willem IV, Willem de IVe, Willem IV., Willem de
			// Vierde'),
			// we will only match on the first name, and store the actual
			// 'number' in a HM.
			royaltyHM.put(oldLine, parts[parts.length - 1]);

		}

		else {
			String juniorRegex = "(junior|senior)*";
			if (parts[parts.length - 1].toLowerCase().matches(juniorRegex)) {
				arraylength -= 1;
				// word matches 'junior or senior', chop off this last part
			}

			// Now we want to figure out how this PER is structured: does it
			// have initials, given names, or only a surname?

			// step 0: check if the name consists of multiple words, and check
			// for last name identifiers (for Dutch):
			// van, de, der, den,

			// step 1: if so, see if the name has initials.
			// step 2: if so, hasInitials = the actual initials, hasGivenName =
			// null
			// step 3: if not, remove the last word, the rest is assumed to be
			// given names: hasGivenName = these names. hasInitials = null
			//	

			// step 0: check the structure of the word.
			// check for initials
			int surnameIndex = -1;
			int counter = arraylength - 1;
			String regex = "(\\w{1}\\.*)";
			// "((m{0,4}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})\\.*(e|de|den)*)|(eerste|tweede|derde|vierde|vijfde|zesde|zevende|achtste|negende)*n*)";

			while (surnameIndex == -1 && counter >= 0) {

				if (parts[counter].matches(regex)) {
					// System.err.println("MATCH: "+ parts[counter]);
					// if(parts[counter].length() == 1 || (
					// parts[counter].length()==2 &&
					// parts[counter].substring(1,2).equals("."))){
					surnameIndex = counter;
				} else {
					counter--;
				}
			}
			if (surnameIndex != -1) {
				// we found initials
				// add one to mark the first position of the surname
				if (surnameIndex < arraylength - 1) {
					surnameIndex++;
				}
				nameHasInitials = true;
			}
			if (surnameIndex == -1) {
				//System.err.println("\tFound no initials-identifiers.");
			} else {
				//System.err.println("\tFound initials. surname starts at index " + surnameIndex+" for word " + line + " . nameHasInitials="+nameHasInitials);
			}

			if (surnameIndex == -1) {
				counter = 0;
				while (surnameIndex == -1 && counter < arraylength) {
					if (surnameIdentifiers.contains(parts[counter]
							.toLowerCase())) {
						surnameIndex = counter;
					} else {
						counter++;
					}
				}
				if (surnameIndex == -1) {
					//System.err.println("\tFound no surname identifiers either");
				} else {
					//System.err.println("\tFound surname identifiers. surname starts at index "+ surnameIndex);
				}
			}

			// if surnameIndex == -1, we assume that the last word of the name
			// is the surname, and that the rest are given names.
			// this is also the case if we have names consisting of one word
			// only.
			if (surnameIndex == -1) {
				surnameIndex = arraylength - 1;
				//System.err.println("\tAssuming that the last word of the name is the surname. SurnameIndex=" +surnameIndex);

			}

			//System.err.println("\tDone searching for surname. Found surname starting at index "+ surnameIndex+" for word " + line);

			// We now store the knowledge about this name: we only keep the
			// initials,
			// except when surnameIndex == 0;
			// System.err.println("\tarraylength="+arraylength+" surnameIndex="+surnameIndex);
			if (arraylength > 1 && surnameIndex > 0) {
				String givenInitials = "";
				for (int i = 0; i < surnameIndex; i++) {
					if (parts[i].length() > 0) {
						givenInitials += parts[i].substring(0, 1).toLowerCase();
					} else {
						if(verbose){System.err.println("Problem with PER structure for string >>"+ line + "<< part >>" + parts[i] + "<<");}
					}
				}

				initialsHM.put(oldLine, givenInitials);
				//System.err.println("\tStoring " + givenInitials+" as initials for word " +oldLine);
				if (nameHasInitials) {
					hasInitialsHM.put(oldLine, true);
					//System.err.println("\tStoring " + true+" in hasInitialsHM for word " +oldLine);
				}
			} else {
				//System.err.println("\tStored nothing...");
			}
			filteredName = "";
			if (arraylength > 0) {
				for (int i = surnameIndex; i < arraylength; i++) {
					filteredName += parts[i];
				}
			} 
			else {
				if(verbose){System.err.println("Negative arraylength for string >>" + line + "<<");}
			}

		}
		//System.out.println("##"+line+" initials:"+initialsHM.get(oldLine)+" hasInitialsHM: "+hasInitialsHM.get(oldLine));
		// System.err.println("\tdone");
		//System.err.println("out: "+filteredName);
		return filteredName;
	}
		
	public String cleanUpString(String s){
		//we remove leading and trailing tabs and whitespaces from each entry, to avoid errors in reading
		s = s.replace("^\\s+", "");
		s = s.replace("\\s+$", "");
		s = s.replaceAll("  ", " ");
		return s;
	}
	
	public HashMap<Integer, Set<String>> removeDoubles(HashMap<Integer, Set<String>> results){
		
		/*	This method is called from the print method. It removes all doubles from the resulting hashmap.
		 * 	That is: all matches that have exactly similar strings (e.g. God <> God, but GOD and God are diff.).
		 * 	If showSources = true, we morf together all the sources.
		 * 
		 */
		
		
		for(int i : results.keySet()){
			Set<String> varset = results.get(i);
			HashMap<String, String> temp = new HashMap<String, String>();
			for(String j : varset){
				//System.err.println(i + " matches: " + j);
				//find the index, which is after the last whitespace in the string
				int ind = j.lastIndexOf(" ");
				String k = j.substring(0, ind);
				//if exact name is already in hashmap, just add the index to its value
				if(!temp.containsKey(k)){
					temp.put(k, j.substring(ind+1));
				}
				else{
					String m = temp.get(k);
					m += ";" + j.substring(ind+1);
					temp.put(k, m);
				}
			}
			//now put back the hashmap as a set for this score
			Set<String> newvarset = new HashSet<String>();
			for(String n : temp.keySet()){
				newvarset.add(n + " " + temp.get(n));
				//System.err.println("\t" + i + " matches: " + n + " " + temp.get(n));
			}
			results.put(i, newvarset);
			/*
			Set<String> v = results.get(i);
			for(String q : v){
				System.err.println(i + " matches: " + q);
			}
			*/
		}
		
		return results;
	}
	
	public String removeIgnorableParts(String s){
		
		/*	In this method we remove all parts of the string between the tags in the String ignoreWordsWithTag
		 */
		
		String newS = s;
		if(s.indexOf(ignoreWordsWithTag) > -1)
		{
			int tag1 = s.indexOf(ignoreWordsWithTag);
			int tag2 = s.indexOf(ignoreWordsWithTag, tag1+ignoreWordsWithTag.length()) + ignoreWordsWithTag.length();
			//System.err.println(s + " "+ tag1+" "+tag2);
			if(tag2 <= 0)
			{
				System.err.println("ERROR: found ignorable start tag without closing tag in string >"+s+"<. Ignoring tag.");
			}
			else
			{
				newS = s.substring(0, tag1) + s.substring(tag2);
				while(newS.indexOf(ignoreWordsWithTag) > -1)
				{
					tag1 = newS.indexOf(ignoreWordsWithTag);
					tag2 = newS.indexOf(ignoreWordsWithTag, tag1+ignoreWordsWithTag.length()) + ignoreWordsWithTag.length();
					newS = newS.substring(0, tag1) + newS.substring(tag2);
				}
			}
		}
		//System.err.println(s + " >  "+ newS);
		return newS;
	}
	
	public static void main(String[] args)
	{
		String[] test = 
			{"Jan Zwijnstaart de jongere", 
				"Ian Swijnstaert de ionghere"};
		List<String> inputSet = new ArrayList<String>();
		for (String s: test)
			inputSet.add("#"+ s + "#");
		NEMatcher m = new NEMatcher();
		m.performMatching(inputSet);
	}
}
