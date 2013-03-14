/*
 * 	Created by Frank Landsbergen (INL) for IMPACT  20-01-2010
 * 	This is an extra class for the Stanford NER.
 * 
 */


package nl.inl.impact.ner.spelvar;
import java.io.Serializable;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.WordAnnotation;
import edu.stanford.nlp.math.SloppyMath;
import edu.stanford.nlp.util.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;
import java.util.Random;

public class SpelVarFactory_oud implements Serializable{
			
	/* variables */
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;



	HashMap<Integer, String> spelvarChangesHM = new HashMap<Integer, String>();

	
	
	boolean marking = false;
	
	int maxSpelvarNGramLength = 6;	/* for ngram-building */
	int minSpelvarNGramLength = 4;  /* for ngram building, is 2 in the NERFeatureFactory */
	int ngramOverlapThreshold = 2; 	/* Ditch candidate that shares an insufficient number of ngrams */  
	int minimalStringLength = -1; 	/* minimal string length for words to be used. off: -1 */
	static int standardLDWeight = 2;	/* standard cost for LD-deletion, -insertion and substitution */
	boolean compareLowercaseLD = false; /* compare lower case version of words */
	int LDThreshold = 2; 				/* no wordpairs under this threshold, goes hand in hand with weight */
	
	String delimiter = "@";
	boolean trainfileWordsMarked = false; 
	String trainMark = "-train"; 
	
	
	/*****/
	double freqlimit = 0.01;		//	 selects only this % of words based on their freq.
									//		0.01 with a list of 30.000: only top 300 words.
	
	

	HashMap<String, Integer> wordlistHM = new HashMap<String, Integer>();  
	HashMap<String, Integer> wordlistFromFileHM = new HashMap<String, Integer>();
	HashMap<String, Integer> wordlistFromFeatureIndexHM = new HashMap<String, Integer>();
	
	HashMap<String, String> ngramHM = new HashMap<String, String>();
	HashMap<String, String> spelvarHM = new HashMap<String, String>();
	HashMap<String, String> spelvarRulesHM = new HashMap<String, String>();
	HashMap<String, Integer> gazetteListHM = new HashMap<String, Integer>();
	static TreeMap<String, Integer> NEListHM = new TreeMap<String, Integer>();
	Set<String> lowercaseWords = new HashSet<String>();

	/*****************/
	//new stuff
	static TreeMap<Double, String> sortedFreqTM = new TreeMap<Double, String>();
	HashMap<String, Integer> topFreqWordListHM = new HashMap<String, Integer>();
	
	/*****************/
	
	static TreeMap<Integer, String> textToXML = new TreeMap<Integer, String>();
	static int storeTextCounter = 0;
	
	boolean listBasedSpelVar = false;
	boolean gazetteBasedSpelVar = false;
	boolean internalSpelVar = false;
	
	/* 	Strings for xml-coding used in xml-output */ 
		
	String meta1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<?xml-stylesheet type=\"text/css\" href=\"css/styleNE.css\"?>\n<FILE>\n\t<FILEINFO>\n\t\t<SOURCEFILE>NER-results ";
	String meta2 = "</SOURCEFILE>\n\t\t<DATE>";
	String meta3 = "\t\t</DATE>\n\t</FILEINFO>\n";
	String meta4 = "\t<CODEEXAMPLE><NE_PER>PER</NE_PER>/<NE_LOC>LOC</NE_LOC>/<NE_ORG>ORG</NE_ORG></CODEEXAMPLE>\n\t<TEXT>";
	static String[][] xmlEscapeChars = { {"&", "&amp;"}, {"<", "&lt;"}, {">", "&gt;"}, {"\"", "&quot;"}, {"'", "&apos;"}};
	
	
	/* oude rommel */
	// hashmap met kosteloze substituties bij LD
	//HashMap<String, String> distanceWeightsHM = new HashMap<String, String>();

	// eerste lijst met alle unieke namen, uit de test- of trainfile 
	//HashMap<String, Integer> wordlistfileA_HM = new HashMap<String, Integer>();
	// tweede lijst met alle unieke namen uit de testfile 
	//HashMap<String, Integer> wordlistfileB_HM = new HashMap<String, Integer>();
	// hashmap met alle woorden gecombineerd, train en test 
	//HashMap<String, Integer> wordlistTotaalHM = new HashMap<String, Integer>(); // 
	//String gazMark = "-GAZ";
	/* In tempHM, remove candidates with low frequency */ 
	//int variantFrequencyThreshold = 3; // zet op 0 voor UIT

	
	/****************************************************************************************************************************/
	
	/*	First method that is called from CRFClassifier.main(), set variables from flags, if applicable */
	
	public void setVariables(int spelvarMaxNgramLength, int spelvarMinNgramLength, int spelvarNgramOverlapThreshold, int spelvarMinStringLength, int spelvarStandardLDWeight, boolean spelvarCompareLowerCaseLD, int spelvarLDThreshold){
		
		if(spelvarMaxNgramLength != -1){maxSpelvarNGramLength = spelvarMaxNgramLength;}
		if(spelvarMinNgramLength != -1){minSpelvarNGramLength = spelvarMinNgramLength;}
		if(spelvarNgramOverlapThreshold != -1){ngramOverlapThreshold = spelvarNgramOverlapThreshold;}
		if(spelvarMinStringLength != -1){minimalStringLength = spelvarMinStringLength;}
		if(spelvarStandardLDWeight != -1){standardLDWeight = spelvarStandardLDWeight;}
		if(spelvarCompareLowerCaseLD){compareLowercaseLD = spelvarCompareLowerCaseLD;}
		if(spelvarLDThreshold != -1){LDThreshold = spelvarLDThreshold;}
	
		System.err.println("Setting main spelvar variables ...");
		System.err.println("maxSpelvarNGramLength = " + maxSpelvarNGramLength);
		System.err.println("minSpelvarNGramLength = " + minSpelvarNGramLength);
		System.err.println("ngramOverlapThreshold = " + ngramOverlapThreshold);
		System.err.println("minimalStringLength = " + minimalStringLength);
		System.err.println("standardLDWeight = " + standardLDWeight);
		System.err.println("LDThreshold = " + LDThreshold);
		System.err.println("compareLowercaseLD = " + compareLowercaseLD);
		System.err.println("Setting main spelvar variables done.");
		
	}
	
	
	/****************************************************************************************************************************/
	
	
	/* 	Initialization methods. These methods are called in the main-method in 
	 * 	CRFClassifier and load the proper files and wordlists into hashmaps 
	 */

	
	public void initListBasedSpelVar(String spelvarlistfile) {
		
		/* Get spelling variation rules from file and 
		 * generate hashmap 'spelvarRulesHM' 
		 */
		
		System.err.print("Reading spelvar rules from file ... ");
		
		try {
			FileReader doc = new FileReader(spelvarlistfile);
			BufferedReader buff = new BufferedReader(doc);
			boolean eof = false;
			while (!eof) {
				String line = buff.readLine();
				if (line == null) {
					eof = true;
				} else {
					if (!line.equals("")) {
						if (line.indexOf("=>") > 0) { 							/* only rules in correct format*/
							String s1 = line.substring(0, line.indexOf("=>")); 	// key
							String s2 = line.substring(
									(line.indexOf("=>") + 2), line.length()); 	// value
							if (!spelvarRulesHM.containsKey(s1)) {
								spelvarRulesHM.put(s1, s2);
							}
						}
					}
				}
			}
			buff.close();
			listBasedSpelVar = true;
		} catch (IOException e) {
			System.err.println("An error occurred reading file "
					+ spelvarlistfile + ": " + e.toString());
		}

		System.err
				.print("Done. Created rulelist with "
						+ spelvarRulesHM.size() + " rules.");
		System.err.println("");
	}

	
		
	public void initGazetteBasedSpelVar(Map<String, Collection<String>> wordToGazetteEntries){
		
		/*	Get gazetteerlists from file and store 
		 * 	them in 'gazetteListHM' 
		 */ 
		
		System.err.print("Retrieving gazettes from model ...");	
		gazetteBasedSpelVar = true;
		for (String word : wordToGazetteEntries.keySet()) {
			gazetteListHM.put(word, 1);
		}
		System.err.print("Done. Retrieved " + gazetteListHM.size()
				+ " gazettes from model.");
		System.err.println();
	}
	

	
	
	public void initInternalSpelVar(Index<String> featureIndex){
		
		/* 	Get words from trainfile and store 
		 * 	them in wordlistFromFeatureIndexHM 
		 */

		loadWordsFromFeatureIndex(featureIndex);
		internalSpelVar = true;
	}
	
	
	
	public void initInternalSpelVar(){
		internalSpelVar = true;
	}
	
	
	
	/********************************************************************************************************/
	
	/* 	Main control method */
	
	
	public void initSpelVarModule(String Fi, boolean m){
			
		/* 	Main control method for the current SpelVarFactory object. This method
		 * 	calls the appropriate methods for the selected types of SpelVar. The flags 
		 * 	for these types should have been set in one or more of the above initialization
		 * 	methods.
		 */

		marking = m;
		
		if(listBasedSpelVar){
			spelvarHM.putAll(spelvarRulesHM);
		}
		if(gazetteBasedSpelVar){	
			System.err.println("Starting gazette based spelvar module");
			marking = false;
			gazetteBasedSpelVar = true;
			loadWordsFromFile(Fi);
			wordlistHM.putAll(wordlistFromFileHM);
			setDelimiter();
			createNgramHM();					
			createSpelVarRules(gazetteListHM);
		}
		if(internalSpelVar){			
			System.err.println("Starting internal spelvar module");
			loadWordsFromFile(Fi);
			gazetteBasedSpelVar = false;
			wordlistHM.putAll(wordlistFromFileHM);
			if(marking){wordlistHM.putAll(wordlistFromFeatureIndexHM);}
			setDelimiter();
			createNgramHM();

			/***************************/
			/* new stuff: put words from (test)file in treemap */ 
			sortWordsByFreq();
			createSpelVarRules2(wordlistHM);
			createSpelVarRules(topFreqWordListHM);
			/***************************/
			
			//createSpelVarRules(wordlistHM);				
			
			/*	The methods below clean up and improve the spelvarHM that have 
			 * 	been created under the internalSpelVar flag. 
			 */	
			identifyDoublets();
			swapPairs();
			removeMark();
		}
		System.err.println("");
		System.err.println("Created " + spelvarHM.size() + " spelvar pairs.");
		emptyHMs();
		printVariantList();
	}
	

	/************************************************************************************************************/
	
	/* 	Methods for loading files and wordlists and saving word data into hashmaps */

	
	
	public void loadWordsFromFeatureIndex(Index<String> featureIndex) {

		/* Retrieve words from trainfile. Only works when 
		 * flag 'useWords' is true.
		 */

		System.err.println("Retrieving words from train file via feature index...");
		
		
		Pattern pa = Pattern.compile("[a-z]");
		Matcher m;
		
		Iterator<String> it = featureIndex.iterator();
		trainfileWordsMarked = true;
		String wordIdentifier = "-WORD|C";
		while (it.hasNext()) {
			String s = (String) (it.next());
			if (s.length() > 7) {
				String p = s.substring(s.length() - 7, s.length());
				if (p.equals(wordIdentifier)) {
					/* 	Limit useless words */
					boolean thisWordisOK = checkWordType(p);
					if (thisWordisOK) {
						String n = s.substring(0, s.length() - 7) + trainMark;
						wordlistFromFeatureIndexHM.put(n, 1);
						m = pa.matcher(n.substring(0,1));
						if (m.matches()) {
							lowercaseWords.add(n);
						}
					}
				}
			}
		}
		System.err.println("Retrieving words from train file done. Collected " + wordlistFromFeatureIndexHM.size()
				+ " words from train file.");
	}
	
	
	
	public void loadWordsFromFile(String file) {

		/* 	Retrieve words from file. */
		
		System.err.println("Collecting words from file ...");

		Pattern pa = Pattern.compile("[a-z]");
		Matcher m;

		try {
			FileReader doc = new FileReader(file);
			BufferedReader buff = new BufferedReader(doc);
			boolean eof = false;
			while (!eof) {
				String line = buff.readLine();
				if (line == null) {
					eof = true;
				} else {
					if (!line.equals("")) {
						String s = line.substring(0, line.indexOf(" "));
						/* 	Limit useless words */
						boolean thisWordisOK = checkWordType(s);
						if (thisWordisOK) {
							if (wordlistFromFileHM.containsKey(s)) {
								int v = wordlistFromFileHM.get(s);
								v++;
								wordlistFromFileHM.put(s, new Integer(v));
							} else {
								wordlistFromFileHM.put(s, new Integer(1));
							}
							
							m = pa.matcher(s.substring(0,1));
							if (m.matches()) {
								lowercaseWords.add(s);
							}
						}
					}
				}
			}
			buff.close();
		} catch (IOException e) {
			System.err
					.println("Woops. Error reading file. "
							+ e.toString());
		}
		System.err.println("Collecting words from file done. Created word list with "
				+ wordlistFromFileHM.size() + " unique words.");
			
	}
	
	
	/************************************************************************************************************/
	
	/*	This is where the real spelvarmodule starts. These methods are called 
	 *  from the main control method initSpelVarModule().
	 */
	
	
		
	public void createNgramHM() {

		/* 	Create ngram-hashmap from all words in hashmap 
		 * 	wordlistHM, or, during testing with internalSpelvar, 
		 * 	from the hashmaps wordlistFromFileHM and
		 * 	wordlistFromFeatureIndexHM. These hm's are done
		 * 	separately in order to remove the trainMark from
		 * 	the words that are retrieved from the trainfile.
		 */
				
		System.err.print("Creating ngram list ...");
	
		if(!marking){
			for(String key : wordlistHM.keySet()){
				fillNgramHM(key, key);
			}
		}		
		if(marking){
			for(String key : wordlistFromFeatureIndexHM.keySet()){
				String k = key.substring(0, key.length() - trainMark.length());
				k = normalizeString(k);
				fillNgramHM(key, k);
			}
			for(String key : wordlistFromFileHM.keySet()){
				String k = normalizeString(key);
				fillNgramHM(key, k);
			}
		}
		System.err.print("done. Created ngram list with " + ngramHM.size()
				+ " entries.");
		System.err.println("");

	}

	
	
	public void fillNgramHM(String key_long, String key_short) {
		
		/* 	The normalized string is used for creating the ngram, but the value
		 * 	put in the HM is the actual unnormalized wordform.
		 * 	e.g. bru => brúck
		 */
	
		String word = "<" + key_short + ">";		
		for (int i = 0; i < word.length(); i++) {
			for (int j = i + minSpelvarNGramLength; j <= word.length(); j++) {
				if (j - i > maxSpelvarNGramLength) {
					continue;
				}
				String st = word.substring(i, j);
				if (ngramHM.containsKey(st)) {
					String q = ngramHM.get(st);
					ngramHM.put(word.substring(i, j), (q + delimiter + key_long));
				} else {
					ngramHM.put(word.substring(i, j), key_long);
				}
			}
		}
	}
	
	
	
	public void createSpelVarRules(HashMap<String, Integer> HM){
		
		/*	This method is the heart of the spelvarmodule. For each
		 * 	word from a hashmap, a set of possible variants (tempHM) is 
		 * 	collected on the basis of shared ngrams. These variants
		 * 	are then compared, and, if applicable, variant=>word pairs  
		 * 	are created and put in the hashmap spelvarHM.
		 * 
		 * 	The main problems are speed and accuracy, mainly for the
		 * 	internalSpelVar procedure. 
		 */
		
		System.err.println("Iterating over " + HM.size()
				+ " words to create variant-word pairs ... ");
		
		HashMap<String, Integer> tempHM = new HashMap<String, Integer>();
		int teller = 0;
		
		for (String key : HM.keySet()) {
			if (teller % 1000 == 0) {
				System.err.print("[" + teller + "]");
			}
			String key_short = key;
			if(marking){
				if(key.length()>trainMark.length()){
					if(key.substring(key.length()-trainMark.length(), key.length()).equals(trainMark)){
						key_short = key.substring(0, key.length() - trainMark.length());
					}
				}
			}
			
			String normalizedKey = normalizeString(key);
			String normalizedKey_short = normalizeString(key_short);
			String variantCandidates = "";
			String word = "<" + normalizedKey_short + ">";
			variantCandidates = key_short;			
			//System.out.println("KEY: " + key_short);
			
			for (int i = 0; i < word.length(); i++) {
				for (int j = i + minSpelvarNGramLength; j <= word.length(); j++) {
					if (j - i > maxSpelvarNGramLength) {
						continue;
					}
					String st = word.substring(i, j);
					if (ngramHM.containsKey(st)) {
						//if (variantCandidates.equals("")) {
						//	variantCandidates = ngramHM.get(st);
						//} else {
							variantCandidates += delimiter + ngramHM.get(st);
						//}
					}
				}
			}
			//if(key_short.equals("déjà")){System.out.println("déjà=>"+variantCandidates);}
			
			Pattern p = Pattern.compile(delimiter);
			String[] items = p.split(variantCandidates);

			/*	Impossible matches are blocked later, but it might be good to 
			 * 	block some sooner, e.g. here, to increase speed.
			 * 	For now: in internalSpelVar, only words are allowed that
			 * 	share the same initial two letters.
			 */
			
			for (int c = 0; c < items.length; c++) {
				/*	Put words from items-array in tempHM with their frequency.
				/* 	block words that are not going to make it anyway,
				 *  due to a too large difference in length.
				 */
				if ((Math.abs(items[c].length() - key_short.length()) <= LDThreshold)) {
					boolean verder = false;
					if(gazetteBasedSpelVar){
						verder = true;
					}
					if(internalSpelVar){
						//if wordlength of both >=2
						if((key_short.length()>=2) && (items[c].length()>=2) ){
							//if first two characters are same
							if(key_short.substring(0,2).equals(items[c].substring(0,2))){
								verder = true;
							}
						}
					}
					if(verder){
					//if( (gazetteBasedSpelVar) || ((internalSpelVar) && ( (key_short.charAt(0) == items[c].charAt(0)) && ((key_short.charAt(1) == items[c].charAt(1)))  ) )){				
						if (tempHM.containsKey(items[c])) {
							// System.out.println("Woord >"+items[c]+"< bestaat al.");
							int v = tempHM.get(items[c]);
							tempHM.put(items[c], new Integer(v + 1));
						} else {
							// System.out.println("Woord >"+items[c]+"< bestaat nog niet.");
							tempHM.put(items[c], new Integer(1));
						}
					}
				}
			}
	
			//System.out.println("key="+key_short+" size:"+tempHM.size());
			
			/*	To increase speed: remove all words that share
			 * 	an insufficient amount of ngrams with the main word.
			 */

			for (Iterator<String> it = tempHM.keySet().iterator(); it.hasNext();) {
				if (tempHM.get(it.next()) < ngramOverlapThreshold) {
					it.remove();
				}
			}
			
			//System.out.println("key="+key_short+" size:"+tempHM.size());
		
			int LD = -1;
			
			
			/*	At this point, the procedures for gazetteBasedSpelVar and internalSpelVar
			 * 	diverge. For gazetteBasedSpelVar, only words with capital letters are compared 
			 * 	with the OCR-error "ß" as the only allowed mistake. GazetteBasedSpelVar is 
			 * 	simple because the order of the variant=>word pair is clear from the start.
			 * 	The internalSpelVar is more complex, because the order is not known a priori. Also,
			 * 	when testing, much more words have to compared with one another (from both
			 * 	train- and testfile). 
			 */
			
			/* 	@TODO
			 * 	1) gazetteBasedSpelVar only compares words with capital letter initials: limit 
			 * 	the tempHM hashmap to those words only.
			 * 
			 * 	2) cut this big method up in smaller parts. The difference in methods called
			 * 	for both procedures (compareWords()) is not that elegant.
			 */
				
			for (Iterator<String> it = tempHM.keySet().iterator(); it.hasNext();) {
				String variant = it.next();
				//String variant_origineel = variant;
				if(!variant.equals(key_short)){
					// 	Remove diacritices (é > e)
					String normalizedVariant = normalizeString(variant);
					//System.out.println("Checking: "+key_short+"<>"+variant);
					if(gazetteBasedSpelVar){
						if( (!lowercaseWords.contains(variant.toLowerCase())) && (!gazetteListHM.containsKey(variant)) ){
							if ((variant.indexOf("ß") == 0) || (Character.isUpperCase(variant.charAt(0)))) {
								if (!variant.equals(key_short)) {
									
									if (!compareLowercaseLD) {
										LD = editDistance(normalizedKey_short,
												normalizedVariant);
										/* LD = edu.stanford.nlp.util.StringUtils.longestCommonSubstring(key_short, variant); */
									} else {
										LD = editDistance(normalizedKey_short
												.toLowerCase(), normalizedVariant
												.toLowerCase());
										/* LD = edu.stanford.nlp.util.StringUtils.longestCommonSubstring(key_short.toLowerCase(), variant.toLowerCase());*/
									}
									if ((LD >= 0) && (LD <= LDThreshold)) {
										spelvarHM.put(variant, key_short);
									}
								}
							}
						}
					}
					if(!gazetteBasedSpelVar){
						//System.err.println("Comparing "+key_short+" <> "+variant);
							
						/* 	variant can have -trainMark. If so, remove */
						String normalizedVariant_short = normalizedVariant;
						if (trainfileWordsMarked) {
							if ((variant.length() > trainMark.length())
									&& (variant.substring((variant.length() - trainMark
											.length()), variant.length())
											.equals(trainMark))) {
								normalizedVariant_short = variant.substring(0,
										(variant.length() - trainMark.length()));
							}
						}
						
						int ngramoverlap = tempHM.get(variant);
						/*
						if(key_short.equals("van") || variant.equals("van")){
							System.err.println("A. van: "+key_short+ " " + variant);
						}
						*/
						/*	Submit string pair to a few first checks */ 
						boolean letsgo = checkWordPairValidity(normalizedKey, normalizedKey_short,
								normalizedVariant, normalizedVariant_short, ngramoverlap);
						
						
						if(letsgo){
							if (!compareLowercaseLD) {
								// LD = computeLD(key_short, k);
								LD = editDistance(key_short, normalizedVariant_short);
							} else {
								// LD = editDistance(key_short, k_short);
								LD = editDistance(key_short.toLowerCase(), normalizedVariant_short.toLowerCase());
							}
							
							if ((LD >= 0) && (LD <= LDThreshold)) {
								/*	The original versions of both words have to be used for the actual rewrite rule */
								/*
								if(key_short.equals("van") || variant.equals("van")){
									System.err.println("B. van: "+key_short+ " " + variant + "LD="+LD);
								}
								*/
								
								/*******************/
								//test with topfreqlist, in which the order of the 
								//rewrite rule is predefined.
							
								spelvarHM.put(variant, key_short);
								/*******************/
								
								//writePairToSpelVarHM(key_short, variant);
							}
						}
					}
					/*
					if (letsgo) {
						compareWords(key, key_short, normalizedVariant, normalizedVariant_short);
					}
					*/

				}
			}
			tempHM.clear();
			teller++;
		}
			
	}
	
	
	
	/************************************************************************************************************/
	
	/* 	Supporting methods (pair comparison, LD, cleaning up words, empty HMs, find correct delimiter) */
	
	
	public boolean checkWordPairValidity(String k1, String k1short, String k2,
			String k2short, int ngramoverlap) {
		
		/* 	See if candidates for LD-comparison stand a chance */
		
		boolean letsgo = true;
		
		/* 	Block if both words have -trainMark marking */
		if ((!k1.equals(k1short)) && (!k2.equals(k2short))) {
			letsgo = false;
		}
		/*	Do not compare word with itself. Wrong, because we're comparing normalized versions! 
		 *  Moved this check to actual writing to HM.
		/*
		if (k2short.equals(k1short)) {
			letsgo = false;
		}
		*/
		/* 	Block if one of the words' length equals the length of the longestcommoncontiguoussubstring e.g. lopen <> lopend */
		if(letsgo){
			int LCCSlength = edu.stanford.nlp.util.StringUtils.longestCommonSubstring(k1short, k2short);
			if((LCCSlength == k1short.length()) || (LCCSlength == k2short.length())){
				letsgo = false;
			}
			if(letsgo){
				/*	If both words are longer and the next char for each is in list, extend LCCS */
				
				/*
				 * 	This doesn't work properly.. it blocks 'geestlykc'<>'geestlyke'
				 * 
				 */
				
				
				String s1 = k1short.substring(LCCSlength, LCCSlength+1);
				String s2 = k2short.substring(LCCSlength, LCCSlength+1);
				boolean extendLCCS = compareCharsForOCRErrors(s1, s2);
				if(extendLCCS){
					LCCSlength++;
					if(k1short.length()==k2short.length()){/*ok: 'geestlykc'<>'geestlyke'*/}
					else{
						if((LCCSlength == k1short.length()) || (LCCSlength == k2short.length())){	
							letsgo = false;
						}
					}			
				}
			}
			/* 	Block if the LCCS is wordlength-1 for both words, and at beginning of both words. e.g. lopen <> loper */
			if(letsgo){
				if( (k1short.length() == k2short.length()) && (k1short.length()-1 == LCCSlength) ){
					if(k1short.substring(0, LCCSlength).equals(k2short.substring(0, LCCSlength))){
						letsgo=false;
					}
				}
			}
			/*	Block if the LCCS is wordlength-1 for word1, wordlength-2 for word2, and the remainer are consonants for 
			 * 	both words.
			 */
			/*
			else if( (k1short.length()-1 == LCCSlength) && (k2short.length()-2 == LCCSlength)  ){
				
			}
			*/
		}
		/*	Both words must have a minimum length */
		if( (letsgo) && (minimalStringLength > 0) ){
			if((k1short.length() < minimalStringLength) || (k2short.length() < minimalStringLength) ){ 
				letsgo = false; 
			}
		}
		/*	Difference in length between words must be limited */
		if (letsgo) {
			if (Math.abs(k2short.length() - k1short.length()) > LDThreshold) {
				letsgo = false;
			}
		}
		/*	Only allow words with sufficient ngram-overlap */
		if (letsgo) {
			if (ngramoverlap <= ngramOverlapThreshold) {
				letsgo = false;
			}
		}
		/* 	Block if key-value pair already exists in spelvarHM */
		if (letsgo) {
			if (spelvarHM.containsKey(k1short)) {
				String var2 = spelvarHM.get(k1short);
				if (var2.equals(k2short)) {
					letsgo = false;
				}
			}
		}
		if (letsgo) {
			if (spelvarHM.containsKey(k2short)) {
				String var2 = spelvarHM.get(k2short);
				if (var2.equals(k1short)) {
					letsgo = false;
				}
			}
		}
		/* 	Block if both words appear either as key or value */
		if (letsgo) {
			if ((spelvarHM.containsKey(k1short))
					&& (spelvarHM.containsKey(k2short))) {
				letsgo = false;
			}
		}
		if (letsgo) {
			if ((spelvarHM.containsValue(k1short))
					&& (spelvarHM.containsValue(k2short))) {
				letsgo = false;
			}
		}

		return letsgo;
	}

		
	public void writePairToSpelVarHM(String k1, String k2){
	//public void compareWords(String key, String key_short, String k,
	//		String k_short) {

		/*	This method is only called with internalSpelVar.
		 * 	Get the LD of both words and get correct HM order 
		 * 	(key=>k or k=>key).
		 */
		
		//key_short = removeInitialAndFinalApostrophes(key_short);
		//k_short = removeInitialAndFinalApostrophes(k_short);
		/*
		int LD = -1;
		if (!compareLowercaseLD) {
			// LD = computeLD(key_short, k);
			LD = editDistance(key_short, k_short);
		} else {
			// LD = editDistance(key_short, k_short);
			LD = editDistance(key_short.toLowerCase(), k.toLowerCase());
		}

		key_short = key;
		k_short = k;
	*/

		/* 	k1 already exists as value. Create k2=>k1 */
		if (spelvarHM.containsValue(k1)) {
			spelvarHM.put(k2, k1);
		}
		/* 	k1 already exists as key. Create k2=>k1 and swap existing
			key-value pair.
		 */
		else if (spelvarHM.containsKey(k1)) {
			String tempval = spelvarHM.get(k1);
			spelvarHM.put(tempval, k1);
			spelvarHM.remove(k1);
			spelvarHM.put(k2, k1);
		}
		/*	k2 already exists as value. Create k1=>k2 */
		else if (spelvarHM.containsValue(k2)) {
			spelvarHM.put(k1, k2);
		}
		/* 	k2 already exists as key. Create k1=>k2 and swap existing pair */
		else if (spelvarHM.containsKey(k2)) {
			String tempval = spelvarHM.get(k2);
			spelvarHM.put(tempval, k2);
			spelvarHM.remove(k2);
			spelvarHM.put(k1, k2);
		} else {
		/*	Fresh new pair */
			spelvarHM.put(k2, k1);
		}
	}
	

	
	public static int editDistance(String s, String t) {
		
		/*	This is a modified version of a method in 
		 * 	StringUtils. Weights are added.
		 */
	
		// Step 1
		int n = s.length(); // length of s
		int m = t.length(); // length of t
		if (n == 0) {
			return m;
		}
		if (m == 0) {
			return n;
		}
		int[][] d = new int[n + 1][m + 1]; // matrix
		// Step 2
		for (int i = 0; i <= n; i++) {
			d[i][0] = i;
		}
		for (int j = 0; j <= m; j++) {
			d[0][j] = j;
		}
		// Step 3
		for (int i = 1; i <= n; i++) {
			char s_i = s.charAt(i - 1); // ith character of s
			// Step 4
			for (int j = 1; j <= m; j++) {
				char t_j = t.charAt(j - 1); // jth character of t
				// Step 5
				int cost; // cost

				if (s_i == t_j) {
					cost = 0;
				} 
				/* 	distance weights */
				else if ((s_i == 'f') && (t_j == 's')) {
					cost = 0;
				} else if ((s_i == 's') && (t_j == 'f')) {
					cost = 0;
				} else if ((s_i == 'c') && (t_j == 'k')) {
					cost = 0;
				} else if ((s_i == 'k') && (t_j == 'c')) {
					cost = 0;
				} else if ((s_i == 'c') && (t_j == 'e')) {
					cost = 0;
				} else if ((s_i == 'e') && (t_j == 'c')) {
					cost = 0;
				} else if ((s_i == 'ß') && (t_j == 'B')) {
					cost = 0;
				} else if ((s_i == 'B') && (t_j == 'ß')) {
					cost = 0;
				} else if ((s_i == 'h') && (t_j == 'b')) {
					cost = 0;
				} else if ((s_i == 'b') && (t_j == 'h')) {
					cost = 0;
				} else if ((s_i == '3') && (t_j == 'b')) {
					cost = 0;
				} else if ((s_i == 'b') && (t_j == '3')) {
					cost = 0;
				} else if ((s_i == '.') || (t_j == '.')) {
					cost = 0;
				} else if ((s_i == '\'') || (t_j == '\'')) {
					cost = 0;
				} else if ((s_i == '<') || (t_j == '<')) {
					cost = 0;
				} else if ((s_i == '>') || (t_j == '>')) {
					cost = 0;
				} else if ((s_i == '\\') || (t_j == '\\')) {
					cost = 0;
				} else if ((s_i == '/') || (t_j == '/')) {
					cost = 0;
				} else if ((s_i == '&') || (t_j == '&')) {
					cost = 0;
				} else if ((s_i == '«') || (t_j == '«')) {
					cost = 0;
				} else if ((s_i == '»') || (t_j == '»')) {
					cost = 0;
				} else if ((s_i == '^') || (t_j == '^')) {
					cost = 0;
				} else if ((s_i == 'r') && (t_j == 'n')) {
					cost = 0;
				} else if ((s_i == 'n') && (t_j == 'r')) {
					cost = 0;
				} else if ((s_i == 'i') && (t_j == 'l')) {
					cost = 0;
				} else if ((s_i == 'l') && (t_j == 'i')) {
					cost = 0;
				} else if ((s_i == 'n') && (t_j == 'u')) {
					cost = 0;
				} else if ((s_i == 'u') && (t_j == 'n')) {
					cost = 0;
				} else if ((s_i == 'v') && (t_j == 'y')) {
					cost = 0;
				} else if ((s_i == 'y') && (t_j == 'v')) {
					cost = 0;
				} else if ((s_i == 'b') && (t_j == 'h')) {
					cost = 0;
				} else if ((s_i == 'h') && (t_j == 'b')) {
					cost = 0;
				} else if ((s_i == 'n') && (t_j == 'h')) {
					cost = 0;
				} else if ((s_i == 'h') && (t_j == 'n')) {
					cost = 0;
				} else if ((s_i == 'y') && (t_j == 'i')) {
					cost = 0;
				} else if ((s_i == 'i') && (t_j == 'y')) {
					cost = 0;
				} else if ((s_i == 'l') && (t_j == 's')) {
					cost = 0;
				} else if ((s_i == 's') && (t_j == 'l')) {
					cost = 0;
				} else {
					cost = standardLDWeight;
				}
				// Step 6
				d[i][j] = SloppyMath.min(d[i - 1][j] + 1, d[i][j - 1] + 1,
						d[i - 1][j - 1] + cost);
			}
		}

		// Step 7
		return d[n][m];
	}
	
	
	public boolean compareCharsForOCRErrors(String s1, String s2){
		
		/*	Compare if two Strings are the same or in a set 
		 * 	of probable OCR-errors like 'c>e'. If so, return true
		 * 
		 * 	Currently only compares single chars, so unable to
		 * 	match ij-y or ii-n
		 * 
		 */

		boolean same = false;
		char s_i = s1.charAt(0);
		char t_j = s2.charAt(0);
		if(s_i == t_j){
			same = true;
		}
		else if ((s_i == 'f') && (t_j == 's')) {
			same = true;
		} else if ((s_i == 's') && (t_j == 'f')) {
			same = true;
		} else if ((s_i == 'c') && (t_j == 'k')) {
			same = true;
		} else if ((s_i == 'k') && (t_j == 'c')) {
			same = true;
		} else if ((s_i == 'c') && (t_j == 'e')) {
			same = true;
		} else if ((s_i == 'e') && (t_j == 'c')) {
			same = true;
		} else if ((s_i == 'ß') && (t_j == 'B')) {
			same = true;
		} else if ((s_i == 'B') && (t_j == 'ß')) {
			same = true;
		} else if ((s_i == 'h') && (t_j == 'b')) {
			same = true;
		} else if ((s_i == 'b') && (t_j == 'h')) {
			same = true;
		} else if ((s_i == '3') && (t_j == 'b')) {
			same = true;
		} else if ((s_i == 'b') && (t_j == '3')) {
			same = true;
		} else if ((s_i == '.') || (t_j == '.')) {
			same = true;
		} else if ((s_i == '\'') || (t_j == '\'')) {
			same = true;
		} else if ((s_i == '<') || (t_j == '<')) {
			same = true;
		} else if ((s_i == '>') || (t_j == '>')) {
			same = true;
		} else if ((s_i == '\\') || (t_j == '\\')) {
			same = true;
		} else if ((s_i == '/') || (t_j == '/')) {
			same = true;
		} else if ((s_i == '&') || (t_j == '&')) {
			same = true;
		} else if ((s_i == '«') || (t_j == '«')) {
			same = true;
		} else if ((s_i == '»') || (t_j == '»')) {
			same = true;
		} else if ((s_i == '^') || (t_j == '^')) {
			same = true;
		} else if ((s_i == 'r') && (t_j == 'n')) {
			same = true;
		} else if ((s_i == 'n') && (t_j == 'r')) {
			same = true;
		} else if ((s_i == 'i') && (t_j == 'l')) {
			same = true;
		} else if ((s_i == 'l') && (t_j == 'i')) {
			same = true;
		} else if ((s_i == 'n') && (t_j == 'u')) {
			same = true;
		} else if ((s_i == 'u') && (t_j == 'n')) {
			same = true;
		} else if ((s_i == 'v') && (t_j == 'y')) {
			same = true;
		} else if ((s_i == 'y') && (t_j == 'v')) {
			same = true;
		} else if ((s_i == 'b') && (t_j == 'h')) {
			same = true;
		} else if ((s_i == 'h') && (t_j == 'b')) {
			same = true;
		} else if ((s_i == 'n') && (t_j == 'h')) {
			same = true;
		} else if ((s_i == 'h') && (t_j == 'n')) {
			same = true;
		} else if ((s_i == 'y') && (t_j == 'i')) {
			same = true;
		} else if ((s_i == 'i') && (t_j == 'y')) {
			same = true;
		} else if ((s_i == 'l') && (t_j == 's')) {
			same = true;
		} else if ((s_i == 's') && (t_j == 'l')) {
			same = true;
		}
		
		return same;
	}
	
	
	
	
	
	
	
	
	
	

	public boolean checkWordType(String w) {
		
		/* 	Filter out useless words. Called
		 * 	when loading words into wordlistHM.
		 */
		
		boolean gaVerder = true;
		Pattern pa;
		Matcher m;

		/* 	No words shorter than minimal ngram-length */
		if (w.length() < minSpelvarNGramLength) {
			gaVerder = false;
		}
		/*	No words consisting of only non-word characters */
		if (gaVerder) {
			pa = Pattern.compile("\\W+");
			m = pa.matcher(w);
			if (m.matches()) {
				gaVerder = false;
			}
		}
		/*	No words existing of only 2 characters, one of which a non-word */
		if (gaVerder) {
			if (w.length() == 2) {
				pa = Pattern
						.compile("((\\W|\\d){1}(\\w|\\d){1})|((\\w|\\d){1}(\\W|\\d){1})");
				m = pa.matcher(w);
				if (m.matches()) {
					gaVerder = false;
				}
			}
		}
		if (gaVerder) {
			/* 	No numbers */
			pa = Pattern.compile("[0-9]+");
			m = pa.matcher(w);
			if (m.matches()) {
				gaVerder = false;
			}
		}

		if (gaVerder) {
			/* 	No numbers like 30,000 or 10.2 */
			pa = Pattern.compile("[0-9]+(,|.)[0-9]+(,|.)*[0-9]*(,|.)*[0-9]*");
			m = pa.matcher(w);
			if (m.matches()) {
				gaVerder = false;
			}
		}
		if (gaVerder) {
			/*	No Roman numbers */
			pa = Pattern
					.compile("^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$");
			m = pa.matcher(w);
			if (m.matches()) {
				gaVerder = false;
			}
		}
		return gaVerder;
	}
	
	
	
	public static String normalizeString(String s) {

		/*	Remove diacritics from string */
		
		String temp = Normalizer.normalize(s, Normalizer.Form.NFD);

		/*	Remove dashes from within string, except when followed by a capital, e.g. ge-looven, but *Saint-Kitts */
		
		int t = temp.indexOf('-');
		if((t>0)&&(t<(temp.length()-1))){
			if(!edu.stanford.nlp.util.StringUtils.isCapitalized(temp.substring(t+1, t+2))){
				temp = temp.substring(0, t) + temp.substring(t + 1);
			}
		}
		
		/* 	Remove initial and final apostrophes */
		temp = removeInitialAndFinalApostrophes(temp);
		return temp.replaceAll("[^\\p{ASCII}]", "");
	}

	
	
	public static String removeInitialAndFinalApostrophes(String s) {

		/*
		 * Wordt aangeroepen bij lezen file. Verwijder apostrof aan begin en
		 * einde van woord bij woorden van > 2 tekens
		 */

		String s_oud = s;

		if (s.length() > 2) {

			int ap = s.indexOf('\'');
			if (ap == (s.length() - 1)) {
				s = s.substring(0, (s.length() - 1));
			}

		}
		return s;
	}
	
	
	
	public void emptyHMs(){
		
		/* 	Empty all HMs except spelvarHM */
		
		ngramHM.clear();
		wordlistHM.clear();
		wordlistFromFileHM.clear();
		wordlistFromFeatureIndexHM.clear();
		lowercaseWords.clear();
	}
	
	
	
	public void setDelimiter(){
		
		/* 	Check wordlists to make sure that the delimiter used to separate
		 * 	words in the ngramHM is not actually present in one of these words.
		 */
		
		boolean goodDelimiter = false;
		while(!goodDelimiter){
			boolean foundHim = false;
			for (Iterator<String> it = wordlistHM.keySet().iterator(); it.hasNext();) {
				String variant = it.next();
				if(variant.indexOf(delimiter)>0){
					foundHim = true;
				}
			}
			if(!foundHim){
				goodDelimiter = true;
			}
			if(foundHim){
				goodDelimiter = false;
				delimiter += delimiter;
			}
		}
		System.err.println("Delimiter="+delimiter);
	}


	
	/**********************************************************************************************************/

	/*	A rather ugly set of methods for the internalSpelVar. When work's done, these
	 * 	methods try to tidy up the spelVarHM a bit. Problems are pairs like
	 * 	A=>B, B=>C (should be A=>C) and cases of A=>B, C=>A
	 */
	
	
	public void identifyDoublets() {

		/*	Turn A=>B, B=>C into A=>C */
		
		for (Map.Entry<String, String> e : spelvarHM.entrySet()) {
			boolean gadoor = true;
			if ((e.getKey().length() > trainMark.length())
					&& (e.getKey().substring(
							(e.getKey().length() - trainMark.length()),
							e.getKey().length()).equals(trainMark))) {
				gadoor = false;
			}
			if (gadoor) {
				if ((e.getValue().length() > trainMark.length())
						&& (e.getValue().substring(
								(e.getValue().length() - trainMark.length()),
								e.getValue().length()).equals(trainMark))) {
					gadoor = false;
				}
			}
			if (gadoor) {
				if (spelvarHM.containsKey(e.getValue())) {
					/*
					System.out.println("CONTROLEREN: " + e.getKey() + ": "
							+ e.getValue());
					*/
					String newval = spelvarHM.get(e.getValue());
					String newkey = e.getKey();
					/*
					System.out.println("FOUND pair " + e.getValue() + "=>"
							+ newval + ". Replaced " + e.getKey() + "=>"
							+ e.getValue() + " with " + newkey + "=>" + newval);
					*/
					spelvarHM.remove(e);
					spelvarHM.put(newkey, newval);
				}
			}
		}
	}

	
	
	public void swapPairs() {

		/*	Remove pairs in which they key has a -trainMark. These
		 * 	words should not be allowed to be key, but they can be value.
		 * 	Swap pair around if allowed (if value does not exist multiple
		 * 	times).
		 */
		
		ArrayList<String> swaplist = new ArrayList<String>();
		for (Map.Entry<String, String> e : spelvarHM.entrySet()) {

			if ((e.getKey().length() > trainMark.length())
					&& (e.getKey().substring(
							(e.getKey().length() - trainMark.length()),
							e.getKey().length()).equals(trainMark))) {
				String shortkey = e.getKey().substring(0,
						(e.getKey().length() - trainMark.length()));
						String thisval = e.getValue();
				int tel = 0;
				for (String tr : spelvarHM.keySet()) {
					String value = spelvarHM.get(tr);
					if (value.equals(thisval)) {
						/* 	A case such as 'hypotheken => hijpotheken'(A) and
						 * 	'hypotheken-train => hijpotheken' (B) is possible.
						 */
						if (!shortkey.equals(tr)) {
							tel++;
						}
					}
				}
				if (tel <= 1) {
					/*	Value only occurs once: put in list. */
					String thiskey = e.getKey();
					swaplist.add(thiskey);
				}

			}
		}

		/*	Iterate over swaplist and make final adjustments */
		for (Iterator<String> it = swaplist.iterator(); it.hasNext();) {
				String kee = (String) it.next();
			String val = spelvarHM.get(kee);
			spelvarHM.remove(kee);
			spelvarHM.put(val, kee.substring(0, kee.length()
					- trainMark.length()));
		}
	}

	
	
	
	public void removeMark() {

		/* 	Final method. Remove any remaining trainMarks
		 * 	from keys and values, just to be sure.
		 */

		String shortkey1 = "";
		String shortval1 = "";
		String shortkey2 = "";
		String shortval2 = "";

		HashMap<String, String> templistHM = new HashMap<String, String>();

		Iterator<Map.Entry<String,String>> it = spelvarHM.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			shortkey1 = (String) pairs.getKey();
			shortval1 = (String) pairs.getValue();
			shortkey2 = shortkey1;
			shortval2 = shortval1;
			if ((shortkey1.length() > trainMark.length())
					&& (shortkey1.substring((shortkey1.length() - trainMark
							.length()), shortkey1.length()).equals(trainMark))) {
				shortkey2 = shortkey1.substring(0,
						(shortkey1.length() - trainMark.length()));
			}
			if ((shortval1.length() > trainMark.length())
					&& (shortval1.substring((shortval1.length() - trainMark
							.length()), shortval1.length()).equals(trainMark))) {
				shortval2 = shortval1.substring(0,
						(shortval1.length() - trainMark.length()));
			}
			if (!shortkey1.equals(shortkey2) || !shortval1.equals(shortval2)) {
				if (spelvarHM.get(shortkey1) != null) {
					it.remove();
					if (!spelvarHM.containsKey(shortkey2)) {
						templistHM.put(shortkey2, shortval2);

					}
				}
			}
		}
		/*
		System.out.println("TEMPLIST");
		for (String k : templistHM.keySet()) {
			String v = templistHM.get(k);
			System.out.println(">" + k + "=>" + v);
		}

		System.out.println("SPELVARLIST");
		for (String k : spelvarHM.keySet()) {
			String v = spelvarHM.get(k);
			System.out.println(k + "=>" + v);
		}
		*/
		spelvarHM.putAll(templistHM);
		templistHM.clear();
	}



	/**********************************************************************************************************/
	
	
	
	public void writeSpelVarHMToFile(String type){
		
		FileOutputStream out;
		PrintStream p;

        try
        {
            if(type.equals("train")){out = new FileOutputStream("tool/results/svpairsTrain.txt");}
            else if(type.equals("test")){out = new FileOutputStream("tool/results/svpairsTest.txt");}
            else{ out = new FileOutputStream("tool/results/svpairs.txt");}
            p = new PrintStream( out );                
            Iterator it = spelvarHM.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                p.println(pairs.getKey() + "=>" + pairs.getValue());
            }
            p.close();
            System.err.println("Wrote spelvar pair list to file 'spelvarpairs.txt'");
        }
        catch (Exception e)
        {
        	System.err.println ("Error writing spelvar pair list to file");
        }
	}
	

	
	
	/**********************************************************************************************************/
	
	
	/*	Called from Sequences.ColumnDocumentReader.printAnswers(): stores all text including tags */	
	
	public static void storeText(String s, String tag){
		
		if(s.equals("X")){
			/*	End of sentence: add line break */
			String t = "\n";
			textToXML.put(storeTextCounter, t);
			storeTextCounter++;
		}
		else{
			/*	Output: <tag>text</tag> */
			if(!tag.equals("O")){tag = "NE_" + tag;}
			String t = checkForXML(s);
			t = "<" + tag + ">" + t + "</"+tag+"> ";
			textToXML.put(storeTextCounter, t);
			storeTextCounter++;
		}
	}
	
	/***********************************************************************************************************/
	
	public void writeTextToXMLFile(String file){
		
		FileOutputStream out;
		PrintStream p;
		
		String metaBegin = meta1 + file + meta2 + getDateTime() + meta3 + meta4;
		String metaEnd = "\t</TEXT>\n</FILE>";
		
		String name = (new File(file)).getName();
		name = name.substring(0, name.length()-4) + ".xml";
				
	    try
        {
        	out = new FileOutputStream(name);
        	p = new PrintStream( out );   
        	p.println(metaBegin);
        	Iterator itr = textToXML.entrySet().iterator();
        	while(itr.hasNext()){        		
        		Map.Entry pairs = (Map.Entry)itr.next();
        		p.print(pairs.getValue());
            }
        	p.println(metaEnd);
            p.close();
            System.err.println("Wrote tagged text to XML file " + name);
        }
        catch (Exception e)
        {
        	System.err.println ("Error writing tagged text to XML file");
        }
	}
	
	
	/**********************************************************************************************************/
	
	
	public void writeNEsToFile(String file){
		
		FileOutputStream out;
		PrintStream p;

		String name = (new File(file)).getName();
		name = name.substring(0, name.length()-4) + "_NElist.txt";
		
        try
        {
        	out = new FileOutputStream("tool/results/" + name);
        	p = new PrintStream( out );                
        	Iterator it = NEListHM.entrySet().iterator();
            while (it.hasNext()) {
            	Map.Entry pairs = (Map.Entry)it.next();
             	p.println(pairs.getKey() + " " + pairs.getValue());
            }
            p.close();
            System.err.println("Wrote named entity list to file " + name);
        }
        catch (Exception e)
        {
        	System.err.println ("Error writing named entity list to file");
        }     
        
	}
	

	
	
	/**********************************************************************************************************/
	
	/*	Called from Sequences.ColumnDocumentReader.printAnswers(): stores found NE's */
	
	public static void storeNEs(String w, String ans){
		
		String total = ans + " " +w;
		if(NEListHM.containsKey(total)){
			int v = NEListHM.get(total);
			v++;
			NEListHM.put(total, v);
		}
		else{
			NEListHM.put(total, 1);
		}
	}
	
	
	/**********************************************************************************************************/
	
	
	/*	The actual method that is called when the CRFClassifier starts dealing with 
	 * 	the words. Before dealing with them, they are replaced on the basis of the 
	 * 	spelvarHM in this method.
	 */
	
	
	public String getVariant(String w1) {
	
		String w2 = w1;
		if (spelvarHM.containsKey(w1)) {
			w2 = spelvarHM.get(w1);
		} else {
			// System.out.println("No variant found");
		}
		return w2;
	}
	
	
	
	/*********************************************************************************************************************/
	
	
	public String removeInitialAndFinalNonWordChars(String s) {

		/*	Called from CRFClassifier: removes initial and
		 * 	final non-word characters from all words.
		 */
		String sOud = s;
		String sInit = "";
		String sFinal= "";
		
		if (s.length() > 2) {
	
			sInit = normalizeString(s.substring(0, 1));
			//System.err.println("s="+s+" sFinal="+sFinal+" s.sub="+s.substring(s.length()-1, s.length()));
			if(s.substring(0, 1).equals(sInit)){
				Pattern pa = Pattern.compile("\\W{1}\\w+");
				Matcher m = pa.matcher(s);
				if (m.matches()) {
					s = s.substring(1, s.length());
				}
			}
			
			
			sFinal = normalizeString(s.substring(s.length()-1, s.length()));
			//System.err.println("s="+s+" sFinal="+sFinal+" s.sub="+s.substring(s.length()-1, s.length()));
			if(s.substring(s.length()-1, s.length()).equals(sFinal)){
				//System.err.println("BOEH");
				Pattern pa = Pattern.compile("\\w+\\W{1}");
				Matcher m = pa.matcher(s);
				if (m.matches()) {
					s = s.substring(0, (s.length()-1));
				}
			}
		}
		//if(!sOud.equals(s)){System.err.println("OUD: "+sOud+" NIEUW: "+s+" sInit: "+sInit+" sFinal: "+sFinal);}
		return s;
	}

	
	/*********************************************************************************************************************/
	
	
	private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
	
	
	/*	Replace special characters &, <, >, ", ' for xml-output */
	
	private static String checkForXML(String s){
		
		for(int i = 0 ; i < xmlEscapeChars.length; i++){
			s = s.replace(xmlEscapeChars[i][0], xmlEscapeChars[i][1]);
		}
		return s;
	}
	

	/*********************************************************************************************************************/

		
	public void sortWordsByFreq(){
		
		/* 	Put words from wordfileHM into a sorted TreeMap,
		 * 	in which words are sorted in frequency
		 */
		//	iterate over wordlistFromFileHM, put reverse in TreeMap
		//	add random double to frequency integer in order
		//	to secure a unique value. Just round up to ints
		//	again for comparison!
		
		Random r = new Random();
		
		for(String key : wordlistFromFileHM.keySet()){
			int v = wordlistFromFileHM.get(key);
			double v2 = v + r.nextDouble();
			sortedFreqTM.put(v2, key);
		}
	
		// 	Now put only top X% in regular HM
		int counter = 0;
		int limit = sortedFreqTM.size() - (int)(sortedFreqTM.size()*freqlimit);
		System.err.println("Deleting first " + limit+" of the list.");
		for(double v : sortedFreqTM.keySet()){
			String key = sortedFreqTM.get(v);
			int v2 = (int)v;
			if(limit<counter){
				topFreqWordListHM.put(key, v2);
			}
			counter++;
		}
		System.err.println("Created a topfreq list with " + topFreqWordListHM.size() +" words.");
		
	}
	
	
	
	/***************************************************************/
	
	//
	public void createSpelVarRules2(HashMap<String, Integer> HM){
			
		//step 1: look for words with diacritics or in CAPITALS. Normalize them.
		//if a key exists equal to the normalized string, write
		//rewrite rule, remove original diacritic from main list.
	
		
		for (Iterator<String> it = HM.keySet().iterator(); it.hasNext();) {
			String key = it.next();
			//	identify diacritics and funny non-word characters
			
			String normalizedKey = normalizeString(key);
			if(!key.equals(normalizedKey)){
				if(HM.containsKey(normalizedKey)){
					spelvarHM.put(key, normalizedKey);
					System.err.println("Created: "+key+"=>"+normalizedKey);
				}
			}
		}

		System.err.println("Old size of wordlistHM: " + wordlistHM.size());
		// remove all words that are now values in the spelvarHM from the main list
		for(String key : spelvarHM.keySet()){
			wordlistHM.remove(key);
		}
		System.err.println("New size of wordlistHM: " + wordlistHM.size());		
		
		for (Iterator<String> it = HM.keySet().iterator(); it.hasNext();) {
			String key = it.next();
			//	identify words in CAPITALS, look for same word in non-capitals
			//	and with initial Capital
	
			boolean capitalized = isCapitalized(key);
			if(capitalized){
				String keyInitCap = decapitalizeString(key); 
				keyInitCap = capitalizeInitial(keyInitCap);
				if(!key.equals(keyInitCap)){
					if(HM.containsKey(keyInitCap)){
						spelvarHM.put(key, keyInitCap);
						System.err.println("Created: "+key+"=>"+keyInitCap);
					}
					else{
						String keyNoCap = decapitalizeString(key);
						if(!key.equals(keyNoCap)){
							if(HM.containsKey(keyNoCap)){
								spelvarHM.put(key, keyNoCap);
								System.err.println("Created: "+key+"=>"+keyNoCap);
							}
						}
					}
				}
			}
		}

		
		/*	It could be the case that rewrite rules such as A > B, B > C exist.
		 * 	Check for those and change them into A > C, B > C.
		 */
		
		for(String key : spelvarHM.keySet()){
			String val = spelvarHM.get(key);
			if(spelvarHM.containsKey(val)){
				String newVal = spelvarHM.get(val);
				spelvarHM.put(key, newVal);
			}
		}
		
		
		System.err.println("Old size of wordlistHM: " + wordlistHM.size());
		// remove all words that are now values in the spelvarHM from the main list
		for(String key : spelvarHM.keySet()){
			wordlistHM.remove(key);
		}
		System.err.println("New size of wordlistHM: " + wordlistHM.size());
		
		
		//	Now check for variation pairs, these should be given in a list in the final version

		String a = "f";		//cannot be word final: should be a way to limit that
		String b = "s";
		
		for (Iterator<String> it = HM.keySet().iterator(); it.hasNext();) {
			String key = it.next();
			//if(key.indexOf(a) != -1){
			int c = key.replace("[^f]", "").length();
			if(c>0){
				//not at word end
				String key2 = key.replace(a, b);
				if(key.indexOf(a)==(key.length()-1)){
					String keyPart = key.substring(0, key.length()-1);
					key2 = keyPart.replace(a, b) + key.substring(key.length()-1);
				}
				if(!key2.equals(key)){
					if(HM.containsKey(key2)){
						spelvarHM.put(key, key2);
						System.err.println("Created: "+key+"=>"+key2);
					}
				}
				else{
					// it didn't help to replace all chars 
					//more than one occurrence of String a, see first
					//if it helps to replace them all, then go on
					/*
					String key2 = key.replace(a, b);
					if(!key2.equals(key)){
						if(HM.containsKey(key2)){
							spelvarHM.put(key, key2);
							System.err.println("Created: "+key+"=>"+key2);
						}
					}
					*/
				}
			}
		}
		
		System.err.println("Old size of wordlistHM: " + wordlistHM.size());
		// remove all words that are now values in the spelvarHM from the main list
		for(String key : spelvarHM.keySet()){
			wordlistHM.remove(key);
		}
		System.err.println("New size of wordlistHM: " + wordlistHM.size());

		
		a = "c";		//cannot be word final: should be a way to limit that
		b = "e";
		
		for (Iterator<String> it = HM.keySet().iterator(); it.hasNext();) {
			String key = it.next();
			//if(key.indexOf(a) != -1){
			int c = key.replace("[^c]", "").length();
			if(c>0){
				String key2 = key.replace(a, b);
				if(!key2.equals(key)){
					if(HM.containsKey(key2)){
						spelvarHM.put(key, key2);
						System.err.println("Created: "+key+"=>"+key2);
					}
				}
				else{
				}
			}
		}
		
		
		//String key2 = replaceChars(key, a, b);
		
		
		/*	It could be the case that rewrite rules such as A > B, B > C exist.
		 * 	Check for those and change them into A > C, B > C.
		 */
		
		for(String key : spelvarHM.keySet()){
			String val = spelvarHM.get(key);
			if(spelvarHM.containsKey(val)){
				String newVal = spelvarHM.get(val);
				spelvarHM.put(key, newVal);
			}
		}
		
		
		System.err.println("Old size of wordlistHM: " + wordlistHM.size());
		// remove all words that are now values in the spelvarHM from the main list
		for(String key : spelvarHM.keySet()){
			wordlistHM.remove(key);
		}
		System.err.println("New size of wordlistHM: " + wordlistHM.size());
		
		
		
		
		
	}
	
	
	public String replaceChars(String s, String a, String b){
		int[] pos = new int[s.length()];
		String sNew = s;
		for(int i = 0; i < s.length(); i++){
			if(Character.toString(s.charAt(i)).equals(a)){
				sNew = s.substring(0, (i-1)) + b;
				if(i!=(s.length()-1)){
					sNew += s.substring(i+1);
				}
			}
		}
		return sNew;
	}
	
	
	
	
	public static boolean isCapitalized(String s){
		boolean yo = true;
		for(int i = 0; i < s.length(); i++){
			if(!Character.isUpperCase(s.charAt(i))){
				yo = false;
			}
		}	
		return yo;
	}
	
	
	public static String decapitalizeString(String s) {
		String s2 = "";
		for(int i = 0; i < s.length(); i++){
			s2 += Character.toLowerCase(s.charAt(i));
		}
		return s2;
	}
	
	public static String capitalizeInitial(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
	
	
	
	
	
	
	
	
	/* oude meuk */

	
	
	public void printVariantList() {
		
		//Print spelVarListHM to Standard output. good for testing

		System.out.println("****************************");
		System.out.println("LIST OF WORD-VARIANT PAIRS");
		for (String k : spelvarHM.keySet()) {
			String v = spelvarHM.get(k);
			System.out.println(k + "=>" + v);
		}
		System.out.println("****************************");
	}
	
	/*
	public int calcNumNGrams(int wordlength){
		int num = 0;
		
		int t = maxSpelvarNGramLength - 2;
		for(int v = 0; v < wordlength; v++){
			int w = v - t;
			if(w>0){
				num +=w;
			}
		}
		
		return num;
	}
	*/
	
	
		/*
		 //	Deze methode berekent de afstand tussen 2 strings op basis van
		 //	herschrijfregels voor letters, zoals f > s en c > k, gegeven in de array
		//varSet[][] (kan misschien ook sneller met hashmap)
		 

		public int computeDistance(String s1, String s2) {
			int dist = -1;
			// kijk of s1 een van de karakters uit de lijst bevat
			for (int a = 0; a < varSet.length; a++) {
				// probleem hiermee: replace vervangt ALLE relevante karakters
				// tegelijkertijd

				String s1_diff = s1.replace(varSet[a][0], varSet[a][1]);
				if (s1_diff.equals(s2)) {
					// System.out.println("PAAR GEVONDEN: >"+s1+"< >"+s2+"< met paar "+varSet[a][0]+"-"+varSet[a][1]);
					dist = 0;
					break;
				} else {
					// andere kant op vergelijken
					s1_diff = s1.replace(varSet[a][1], varSet[a][0]);
					if (s1_diff.equals(s2)) {
						// System.out.println("PAAR GEVONDEN: >"+s1+"< >"+s2+"< met paar "+varSet[a][1]+"-"+varSet[a][0]);
						dist = 0;
						break;
					}
				}
			}

			return dist;
		}

		/*
		// bron: http://en.literateprograms.org/Levenshtein_distance_(Java)
		// LD zonder gewichten
		public int computeLD(String s1, String s2) {
			int[][] dp = new int[s1.length() + 1][s2.length() + 1];
			for (int i = 0; i < dp.length; i++) {
				for (int j = 0; j < dp[i].length; j++) {
					dp[i][j] = i == 0 ? j : j == 0 ? i : 0;
					if (i > 0 && j > 0) {
						if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
							dp[i][j] = dp[i - 1][j - 1];
						} else {
							dp[i][j] = Math.min(dp[i][j - 1] + 1, Math.min(
									dp[i - 1][j - 1] + 1, dp[i - 1][j] + 1));
						}
					}
				}
			}
			return dp[s1.length()][s2.length()];
		}

		

		/*
		 * public void createwordlistfileB_HM(ArrayList<String> data) {
		 * 
		 * 
		 * // Maak een hashmap aan van de (unieke) woorden in een file, key: woord,
		 * // value: woordfrequentie.
		 * 
		 * 
		 * System.out.print("Creating unique word list from testfile ...");
		 * 
		 * // zet woorden testfile in hashmap 'wordlistfileB_HM' Iterator<String> it
		 * = data.iterator(); while (it.hasNext()) { String s = (String)
		 * (it.next()); // sluit bepaalde woorden (punten, komma's, getallen, enz.)
		 * uit boolean thisWordisOK = checkWordType(s);
		 * 
		 * if (thisWordisOK){ // de string s bevat nu het woord: voeg toe aan
		 * hashMap // 'wordlistfileB_HM' als woord nieuw is // anders: frequentie
		 * verhogen
		 * 
		 * if (wordlistfileB_HM.containsKey(s)) { int v = wordlistfileB_HM.get(s);
		 * v++; wordlistfileB_HM.put(s, new Integer(v)); } else {
		 * wordlistfileB_HM.put(s, new Integer(1)); } } }
		 * System.out.print("done. Created word list with "
		 * +wordlistfileB_HM.size()+" unique words."); System.out.println("");
		 * 
		 * // testprint //System.out.println("Hashmap 'wordlist':"); //for ( String
		 * key : wordlistfileB_HM.keySet() ){ //Integer value =
		 * wordlistfileB_HM.get( key ); //System.out.println( key + " " + value );
		 * //} }
		 */

		/*
		 * public int getDistanceWeightHM(char s, char t){
		 * 
		 * int cost = standardLDCost;
		 * 
		 * if ( ( s == '.' ) || ( t == '.' ) ){ cost = 0; } if ( ( s == '\'' ) || (
		 * t == '\'' ) ){ cost = 0; }
		 * if(distanceWeightsHM.containsKey(String.valueOf(s))){ String t2 =
		 * distanceWeightsHM.get(s); if(String.valueOf(t).equals(t2)){ cost = 0; } }
		 * if(distanceWeightsHM.containsKey(String.valueOf(t))){ String s2 =
		 * distanceWeightsHM.get(t); if(String.valueOf(s).equals(s2)){ cost = 0; } }
		 * 
		 * return cost; }
		 * 
		 * public void fillDistanceWeightsHM(){
		 * 
		 * // Vul de distanceWeightsHM met alle kosteloze paren
		 * 
		 * distanceWeightsHM.put("r", "n"); distanceWeightsHM.put("i", "l");
		 * distanceWeightsHM.put("n", "u"); distanceWeightsHM.put("v", "y");
		 * distanceWeightsHM.put("b", "h"); distanceWeightsHM.put("n", "h");
		 * distanceWeightsHM.put("y", "i"); distanceWeightsHM.put("l", "s");
		 * distanceWeightsHM.put("t", "f"); distanceWeightsHM.put("f", "s");
		 * distanceWeightsHM.put("c", "e"); distanceWeightsHM.put("ß", "B");
		 * distanceWeightsHM.put("3", "b"); distanceWeightsHM.put("3", "B");
		 * distanceWeightsHM.put("b", "h"); distanceWeightsHM.put("n", "h");
		 * 
		 * }
		 */

		/*
		public void doSpelVarTrainfile() {

			
			 // Er wordt alleen getraind, vergelijk woorden van de trainfile met
			 // elkaar, en vermijd conflicten met al opgestelde regels uit
			 // spelvarlistHM, die inmiddels ook in spelvarHM staan
			 //

			// maak ngramHM aan uit de woordenlijst van de trainfile
			createNgramHM(wordlistfileB_HM);
			// stel spelvarparen op adhv de ngramHM
			createSpelvarHM(wordlistfileB_HM);
		}
		
		*/
	/*
		public void createNgramHM(HashMap<String, Integer> wordlistHM) {

			System.out.print("Creating ngram list from trainfile ...");

			for (String key : wordlistHM.keySet()) {
				fillNgramHM(key, "nonsens");
			}

			System.out.print("done. Created ngram list from trainfile with "
					+ ngramHM.size() + " entries.");
			System.out.println("");
		}
	*/
		
		/*

		public void createSpelvarHM(HashMap<String, Integer> wordlistHM) {

			
			// Maak spelvarparen aan. Itereer over wordlistHM en doorzoek ngramHM.
			

			System.out.print("Iterating over " + wordlistHM.size()
					+ " words to create word-variant pairs ... ");

			HashMap<String, Integer> tempHM = new HashMap<String, Integer>();
			int teller = 0;

			for (String key : wordlistHM.keySet()) {

				boolean gaVerder = false;
				if (teller % 1000 == 0) {
					System.out.print("[" + teller + "]");
				}

				String key_short = key;
				String variantCandidates = "";

				// Stap 1: maak ngrams van woord en sla van elke ngram de
				// bijbehorende woorden op in een String

				String word = "<" + key + ">";
				if (trainfileWordsMarked) {
					// kijk of woord op '-train' (de trainMark) eindigt, zo ja,
					// verwijderen

					if ((key.length() > trainMark.length())
							&& (key.substring((key.length() - trainMark.length()),
									key.length()).equals(trainMark))) {
						word = "<"
								+ key.substring(0, key.length()
										- trainMark.length()) + ">";
						key_short = key.substring(0, (key.length() - trainMark
								.length()));
						gaVerder = true;
					}
					gaVerder = true;
				}

				for (int i = 0; i < word.length(); i++) {
					for (int j = i + minSpelvarNGramLength; j <= word.length(); j++) {
						if (j - i > maxSpelvarNGramLength) {
							continue;
						}
						String st = word.substring(i, j);

						if (variantCandidates.equals("")) {
							variantCandidates = ngramHM.get(st);
						} else {
							variantCandidates += delimiter + ngramHM.get(st);
						}
					}
				}

				// Stap 2: splits de string 'variantCandidates' en zet ze in hashmap
				// 'tempHM'
				// met ngram-frequentie als value
				Pattern p = Pattern.compile(delimiter);
				String[] items = p.split(variantCandidates);

				for (int c = 0; c < items.length; c++) {
					if (tempHM.containsKey(items[c])) {
						// System.out.println("Woord >"+items[c]+"< bestaat al.");
						int v = tempHM.get(items[c]);
						tempHM.put(items[c], new Integer(v + 1));
					} else {
						// System.out.println("Woord >"+items[c]+"< bestaat nog niet.");

						tempHM.put(items[c], new Integer(1));

					}
				}

				// Tussenstap: verwijder alle woorden met maar 1 overlappende ngram
				for (Iterator<String> it = tempHM.keySet().iterator(); it.hasNext();) {
					if (tempHM.get(it.next()) < ngramOverlapThreshold) {
						it.remove();
					}
				}

				// testje
				// System.out.println("-----------------");
				// for ( String tr : tempHM.keySet() ){
				// int value = tempHM.get( tr );
				// System.out.println( ">>>>>>>>>>>> "+tr + "-" + value );
				// }

				// Stap 3: zoek woord-variantparen.

				if (!trainfileWordsMarked) {
					gaVerder = true;
				}

				if (gaVerder) {

					// System.out.println("Vergelijk >"+key_short+"< met andere woorden...."
					// + tempHM.size());

					int f1 = wordlistHM.get(key); // woordfrequentie van dit woord
					for (String key2 : tempHM.keySet()) {

						// ook key2 kan achtervoegsel '-train' hebben
						String key2_short = key2;
						if (trainfileWordsMarked) {
							// kijk of woord op '-train' (de trainMark) eindigt,
							// zo ja, verwijderen
							if ((key2.length() > trainMark.length())
									&& (key2.substring((key2.length() - trainMark
											.length()), key2.length())
											.equals(trainMark))) {
								key2_short = key2.substring(0,
										(key2.length() - trainMark.length()));
							}
						}

						int ngramoverlap = tempHM.get(key2); // aantal ngrams
																// overlap
						boolean letsgo = checkWordPairValidity(key, key_short,
								key2, key2_short, ngramoverlap);
						if (letsgo) {
							compareWords_OUD(key, key_short, f1, key2, key2_short);
						}

					}

				}

				tempHM.clear();
				teller++;
			}
			identifyDoublets();
			swapPairs();
			removeMark();

			// leegmaken HMs
			wordlistfileA_HM.clear();
			wordlistfileB_HM.clear();
			wordlistTotaalHM.clear();
			ngramHM.clear();

			System.out.print("done. Created " + spelvarHM.size()
					+ " word-variant pairs.");
			System.out.println("");
		}
		*/
	
	
	/*
	public void createGazBasedSpelvarHM_OUD() {
		// Itereer over de HM gazetteListHM, maak Ngrams aan en voeg de juiste
		// Ngrams bij elkaar.

		System.out.print("Iterating over " + wordlistTotaalHM.size()
				+ " words to create variant(trainf)-word(gaz) pairs ... ");

		HashMap<String, Integer> tempHM = new HashMap<String, Integer>();
		int teller = 0;

		for (String key : gazetteListHM.keySet()) {

			if (teller % 1000 == 0) {
				System.out.print("[" + teller + "]");
			}
			String key_short = key
					.substring(0, key.length() - gazMark.length());
			String normalizedKey = normalizeString(key);
			String normalizedKey_short = normalizeString(key_short);
			String variantCandidates = "";

			System.out.println("KEY: " + key_short);

			// Stap 1: maak ngrams van woord en sla van elke ngram de
			// bijbehorende woorden op in een String

			// verwijder gazMark
			String word = "<"
					+ normalizedKey.substring(0, normalizedKey.length()
							- gazMark.length()) + ">";

			for (int i = 0; i < word.length(); i++) {
				for (int j = i + minSpelvarNGramLength; j <= word.length(); j++) {
					if (j - i > maxSpelvarNGramLength) {
						continue;
					}
					String st = word.substring(i, j);
					if (ngramHM.containsKey(st)) {
						if (key_short.equals("Osnabrück")) {
							System.out.println("GEVONDEN voor key " + key
									+ ": " + st + " / " + ngramHM.get(st));
						}
						if (variantCandidates.equals("")) {
							variantCandidates = ngramHM.get(st);
						} else {
							variantCandidates += delimiter + ngramHM.get(st);
						}
					}
				}
			}

			// Stap 2: splits de string 'variantCandidates' en zet ze in hashmap
			// 'tempHM'
			// met ngram-frequentie als value
			Pattern p = Pattern.compile(delimiter);
			String[] items = p.split(variantCandidates);

			for (int c = 0; c < items.length; c++) {
				if (tempHM.containsKey(items[c])) {
					// System.out.println("Woord >"+items[c]+"< bestaat al.");
					int v = tempHM.get(items[c]);
					tempHM.put(items[c], new Integer(v + 1));
				} else {
					// System.out.println("Woord >"+items[c]+"< bestaat nog niet.");

					tempHM.put(items[c], new Integer(1));

				}
			}

			// Tussenstap: verwijder alle woorden met maar 1 overlappende ngram
			for (Iterator<String> it = tempHM.keySet().iterator(); it.hasNext();) {
				if (tempHM.get(it.next()) < ngramOverlapThreshold) {
					it.remove();
				}
			}

			// Stap 3: zoek woord-variantparen.

			// In tempHM staan geen woorden uit de gazlist, dus niet nodig om
			// zoekactie
			// te beperken tot bepaalde woorden

			int LD = -1;
			for (Iterator<String> it = tempHM.keySet().iterator(); it.hasNext();) {
				String variant = it.next();
				String variant_origineel = variant;

				// vergelijk geen varianten uit de trainfile bij het testen
				if ((variant.length() > trainMark.length())
						&& (variant.substring((variant.length() - trainMark
								.length()), variant.length()).equals(trainMark))) {
				} else {
					// doe hier wat acties met de string, moet later elders
					// 1) verwijder non-word tekens
					// verwijderd: deze tekens leveren nu kosteloze
					// LD-substitutie op

					// 2) verwijder diacritische tekens (é > e)
					String normalizedVariant = normalizeString(variant);
					// if(!variant.equals(variant3)){System.out.println("diacritics: replaced "+variant+" with "+variant3);}

					// vergelijk alleen als variant begint met een hoofdletter
					// (alle gazetteers hebben een hoofdletter)
					// NB uitzondering voor 'ß'
					// en vergelijk geen gelijke woorden

					if (variant.equals("Osnabrug")) {
						System.out.println("////////// vergelijk: "
								+ normalizedKey_short + " met "
								+ normalizedVariant);
					}
					if ((variant.indexOf("ß") == 0)
							|| (Character.isUpperCase(variant.charAt(0)))) {

						if (!variant.equals(key_short)) {
							// System.out.println("tempset: "+variant);
							if (!compareLowercaseLD) {
								LD = editDistance(normalizedKey_short,
										normalizedVariant);
								// LD =
								// edu.stanford.nlp.util.StringUtils.longestCommonSubstring(key_short,
								// variant);
							} else {
								LD = editDistance(normalizedKey_short
										.toLowerCase(), normalizedVariant
										.toLowerCase());
								// LD =
								// edu.stanford.nlp.util.StringUtils.longestCommonSubstring(key_short.toLowerCase(),
								// variant.toLowerCase());
							}
							// testje
							// LD = key_short.length() - LD;
							System.out.println("KEY: " + key_short
									+ " VARIANT: " + normalizedVariant + "("
									+ variant_origineel + ") LD: " + LD);
							if ((LD >= 0) && (LD <= LDThreshold)) {
								spelvarHM.put(variant, key_short);
							}
						}
					}
				}
			}
			tempHM.clear();
			teller++;
		}
	}
	*/
	
	/*

	public void setGazettes(Map<String, Collection<String>> wordToGazetteEntries) {
		System.out.print("Retrieving gazettes from model ...");
		for (String word : wordToGazetteEntries.keySet()) {
			// System.out.println("Adding "+word+" to gazetteListHM");
			gazetteListHM.put((word + gazMark), 1);
		}
		System.out.print("Done. Retrieved " + gazetteListHM.size()
				+ " gazettes from model.");
		System.out.println();
	}
	*/

	
	
	/*
	
	public void createGeneralWordlist() {

		System.out.print("Creating combined word list ...");
		// Combineer de wordlists van de train- en testfile in één wordlist
		wordlistTotaalHM.putAll(wordlistfileA_HM);
		// System.out.println("Hallo "+wordlistTotaalHM.size());
		wordlistTotaalHM.putAll(wordlistfileB_HM);
		// System.out.println("Hallo "+wordlistTotaalHM.size());

		// wordlistTotaalHM.putAll(gazetteListHM);

		// testprint
		// System.out.println("Hashmap 'wordlist':");
		// for ( String key : wordlistTotaalHM.keySet() ){
		// Integer value = wordlistTotaalHM.get( key );
		// System.out.println( key + " " + value );
		// }
		// System.out.println("");

		System.out.print("done. Created a combined word list with "
				+ wordlistTotaalHM.size() + " unique words.");
		System.out.println("");
	}

	*/
	
	/*
	public void createNgramHM_OUD() {

		
		 // Iterate over de wordlists en maak nieuwe hashmap aan met
		 //	ngram-woordparen. Resultaat: <Leid => Leiden_Leids_Leider
		//
		// Hier worden ipv wordlistTotaal de aparte HMs gazetteList, wordlistsA
		 // en B gebruikt, omdat dit het makkelijker maakt bij de woorden uit de
		 // trainfile de markering te verwijderen.
		 

		System.out.print("Creating ngram list ...");
		
		for (String key : wordlistfileB_HM.keySet()) {
			fillNgramHM(key, "nonsens");
		}

		// voeg nu de woorden van de trainfile toe, indien relevant. Markeer de
		// woorden, zodat
		// duidelijk is dat ze uit de trainfile afkomstig zijn.
		for (String key : wordlistfileA_HM.keySet()) {
			fillNgramHM(key, trainMark);
		}

		System.out.print("done. Created ngram list with " + ngramHM.size()
				+ " entries.");
		System.out.println("");
		// testprint
		// System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>Hashmap 'ngramHM':");
		// for ( String key : ngramHM.keySet() ){
		// String value = ngramHM.get( key );
		// System.out.println( key + " " + value );
		// }
	}
*/
	

	/*	
		public void compareWords_OUD(String key, String key_short, int f1, String k,
				String k_short) {

			// System.out.println("Kandidaat woord >"+k+"< met ngramfreq: "+value);

			key_short = removeInitialAndFinalApostrophes(key_short);
			k_short = removeInitialAndFinalApostrophes(k_short);

			int LD = -1;
			if (!compareLowercaseLD) {
				// LD = computeLD(key_short, k);
				LD = editDistance(key_short, k_short);
			} else {
				// LD = editDistance(key_short, k_short);
				LD = editDistance(key_short.toLowerCase(), k.toLowerCase());
			}
			
			if ((LD >= 0) && (LD <= LDThreshold)) {
				
				// Bepaal nu welk woord key en welk value wordt
				// mogelijkheid 1 bij testfile: 2 woorden uit de trainfile. Dit kan
				// niet: weggefilterd in checkWordPairValidity() NOG DOEN
				// mogelijkheid 2 bij testfile: 1 woord uit de trainfile. Dit woord
				// wordt value
				// mogelijkheid 3 bij testfile: 2 woorden uit testfile. Meest
				// frequente woord wordt value
				// mogelijkheid 1 bij trainfile: 2 woorden uit trainfile. Meest
				// frequente woord wordt value.

				key_short = key;
				k_short = k;

				// key_short bestaat als value. Maak k => key_short
				if (spelvarHM.containsValue(key_short)) {
					spelvarHM.put(k_short, key_short);
				}
				// key_short bestaat als key. Maak k => key_short en draai bestaand
				// key-value paar om
				else if (spelvarHM.containsKey(key_short)) {
					String tempval = spelvarHM.get(key_short);
					spelvarHM.put(tempval, key_short);
					spelvarHM.remove(key_short);
					spelvarHM.put(k_short, key_short);
				}
				// k bestaat als value: maak key_short => k
				else if (spelvarHM.containsValue(k_short)) {
					spelvarHM.put(key_short, k_short);
				}
				// k bestaat als key
				else if (spelvarHM.containsKey(k_short)) {
					String tempval = spelvarHM.get(k_short);
					spelvarHM.put(tempval, k_short);
					spelvarHM.remove(k_short);
					spelvarHM.put(key_short, k_short);
				} else {
					// nieuw paar
				}
			}
		}
	*/	
	
	/*
	
	public void createInternalSpelvarHM() {

		System.out.print("Iterating over " + wordlistTotaalHM.size()
				+ " words to create word-variant pairs ... ");

		HashMap<String, Integer> tempHM = new HashMap<String, Integer>();
		int teller = 0;

		for (String key : wordlistTotaalHM.keySet()) {

			boolean gaVerder = false;
			if (teller % 1000 == 0) {
				System.out.print("[" + teller + "]");
			}

			String key_short = key;
			String variantCandidates = "";

			// Stap 1: maak ngrams van woord en sla van elke ngram de
			// bijbehorende woorden op in een String

			String word = "<" + key + ">";
			if (trainfileWordsMarked) {
				// kijk of woord op '-train' (de trainMark) eindigt, zo ja,
				// verwijderen

				if ((key.length() > trainMark.length())
						&& (key.substring((key.length() - trainMark.length()),
								key.length()).equals(trainMark))) {
					word = "<"
							+ key.substring(0, key.length()
									- trainMark.length()) + ">";
					key_short = key.substring(0, (key.length() - trainMark
							.length()));
					gaVerder = true;
				}
				gaVerder = true;
			}

			for (int i = 0; i < word.length(); i++) {
				for (int j = i + minSpelvarNGramLength; j <= word.length(); j++) {
					if (j - i > maxSpelvarNGramLength) {
						continue;
					}
					String st = word.substring(i, j);

					if (variantCandidates.equals("")) {
						variantCandidates = ngramHM.get(st);
					} else {
						variantCandidates += delimiter + ngramHM.get(st);
					}
				}
			}

			// Stap 2: splits de string 'variantCandidates' en zet ze in hashmap
			// 'tempHM'
			// met ngram-frequentie als value
			Pattern p = Pattern.compile(delimiter);
			String[] items = p.split(variantCandidates);

			for (int c = 0; c < items.length; c++) {
				if (tempHM.containsKey(items[c])) {
					// System.out.println("Woord >"+items[c]+"< bestaat al.");
					int v = tempHM.get(items[c]);
					tempHM.put(items[c], new Integer(v + 1));
				} else {
					// System.out.println("Woord >"+items[c]+"< bestaat nog niet.");

					tempHM.put(items[c], new Integer(1));

				}
			}

			// Tussenstap: verwijder alle woorden met maar 1 overlappende ngram
			for (Iterator<String> it = tempHM.keySet().iterator(); it.hasNext();) {
				if (tempHM.get(it.next()) < ngramOverlapThreshold) {
					it.remove();
				}
			}

			// testje
			// System.out.println("-----------------");
			// for ( String tr : tempHM.keySet() ){
			// int value = tempHM.get( tr );
			// System.out.println( ">>>>>>>>>>>> "+tr + "-" + value );
			// }

			// Stap 3: zoek woord-variantparen.

			if (!trainfileWordsMarked) {

				gaVerder = true;
			}

			if (gaVerder) {

				// System.out.println("Vergelijk >"+key_short+"< met andere woorden...."
				// + tempHM.size());

				int f1 = wordlistTotaalHM.get(key); // woordfrequentie van dit
				// woord
				for (String key2 : tempHM.keySet()) {

					// ook key2 kan achtervoegsel '-train' hebben
					String key2_short = key2;
					if (trainfileWordsMarked) {
						// kijk of woord op '-train' (de trainMark) eindigt,
						// zo ja, verwijderen
						if ((key2.length() > trainMark.length())
								&& (key2.substring((key2.length() - trainMark
										.length()), key2.length())
										.equals(trainMark))) {
							key2_short = key2.substring(0,
									(key2.length() - trainMark.length()));
						}
					}

					int ngramoverlap = tempHM.get(key2); // aantal ngrams
					// overlap
					// if((key.equals("partij")) || (key.equals("party")) ){
					// System.out.println("Vergelijk "+key+" ("+f1+") met "+key2+" ("+ngramoverlap+")");
					// }
					boolean letsgo = checkWordPairValidity(key, key_short,
							key2, key2_short, ngramoverlap);
					if (letsgo) {
						compareWords_OUD(key, key_short, f1, key2, key2_short);
					}

				}

			}

			if (variantFrequencyThreshold > 0) {


			}

			tempHM.clear();
			teller++;
		}
	
		identifyDoublets();
		swapPairs();
		removeMark();

		// leegmaken HMs
		wordlistfileA_HM.clear();
		wordlistfileB_HM.clear();
		wordlistTotaalHM.clear();
		ngramHM.clear();

		System.out.print("done. Created " + spelvarHM.size()
				+ " word-variant pairs.");
		System.out.println("");
	}

	*/

	/*

	public void checkSpelVar(String testfile, Index<String> featureIndex,
			String spelvarlistfile) {

		// vul LDweights als nog niet gedaan is
		// if(distanceWeightsHM.size() == 0){
		// fillDistanceWeightsHM();
		// }

		// Lees de regels uit de file spelvarlistfile
		//readSpelVarList(spelvarlistfile);

		// Lees de andere files
		checkSpelVar(testfile, featureIndex);

	}

	public void checkSpelVar(String testfile, Index<String> featureIndex) {

		// Deze methode wordt aangeroepen bij het testen en leest de woorden uit
		// de trainfile

		// vul LDweights als nog niet gedaan is
		// if(distanceWeightsHM.size() == 0){
		// fillDistanceWeightsHM();
		// }

		// Haal woorden op uit model, dit zijn de woorden uit de trainfile
		Iterator<String> it = featureIndex.iterator();

		trainfileWordsMarked = true;

		String wordIdentifier = "-WORD|C";
		System.out.print("Spelvarmodule. Collecting words from model ...");
		while (it.hasNext()) {
			String s = (String) (it.next());
			if (s.length() > 7) {
				// System.out.println(">"+s+"<");
				String p = s.substring(s.length() - 7, s.length());
				// System.out.println(">"+p);
				if (p.equals(wordIdentifier)) {
					// sluit bepaalde woorden uit
					boolean thisWordisOK = checkWordType(p);

					if (thisWordisOK) {
						String n = s.substring(0, s.length() - 7) + trainMark;
						// System.out.println(">"+n+"<");
						// woorden uit model krijgen frequentie 1
						wordlistfileA_HM.put(n, 1);
					}
				}
			}
		}
		System.out.print("done. Collected " + wordlistfileA_HM.size()
				+ " words from model.");
		System.out.println("");

		// haal woorden uit testfile
		checkSpelVar(testfile);
	}
	*/
	
	/*
	public void checkSpelVar(String file, String spelvarlistfile) {

		// Deze methode wordt aangeroepen als er een spelvarlist in de propsfile
		// staat (met flag 'spelvarlist')

		// vul LDweights als nog niet gedaan is
		// if(distanceWeightsHM.size() == 0){
		// fillDistanceWeightsHM();
		// }

		// lees spelvarlist, ga dan naar de andere CheckSpelVar() methode
		//readSpelVarList(spelvarlistfile);

		// lees de trainfile
		checkSpelVar(file);
	}

	public void checkSpelVar(String file) {

		// vul LDweights als nog niet gedaan is
		// if(distanceWeightsHM.size() == 0){
		// fillDistanceWeightsHM();
		// }

		// Deze methode leest de woorden uit de train- of testfile

		System.out.print("Spelvarmodule. Collecting words from file ...");

		// ArrayList<String> data = new ArrayList<String>();
		try {
			FileReader doc = new FileReader(file);
			BufferedReader buff = new BufferedReader(doc);
			boolean eof = false;
			while (!eof) {
				String line = buff.readLine();
				if (line == null) {
					eof = true;
				} else {
					if (!line.equals("")) {
						String s = line.substring(0, line.indexOf(" "));
						// sluit bepaalde woorden (punten, komma's, getallen,
						// enz.) uit

						boolean thisWordisOK = checkWordType(s);
						if (thisWordisOK) {
							// de string s bevat nu het woord: voeg toe aan
							// hashMap
							// 'wordlistfileB_HM' als woord nieuw is
							// anders: frequentie verhogen

							if (wordlistfileB_HM.containsKey(s)) {
								int v = wordlistfileB_HM.get(s);
								v++;
								wordlistfileB_HM.put(s, new Integer(v));
							} else {
								wordlistfileB_HM.put(s, new Integer(1));
							}
						}
						// data.add(line.substring(0, line.indexOf(" ")));
					}
				}
			}
			buff.close();
		} catch (IOException e) {
			System.out
					.println("Fout met lezen testfile voor spellingsvariatiemodule ... "
							+ e.toString());
		}

		// System.out.print("done. Collected "+data.size()+" words from file.");
		System.out.print("Done. Created word list with "
				+ wordlistfileB_HM.size() + " unique words.");
		System.out.println("");

		// Maak hashMap van alle (unieke) woorden uit de testfile 
		// createwordlistfileB_HM(data);
		// nieuwe aanpak: haal 'regels' uit file
		spelvarHM.putAll(spelvarRulesHM);

		// als er alleen getraind wordt, volg dit pad:
		// doSpelVarTrainfile();

		// voeg woorden uit gazlist toe aan totale HM

		// hieronder staat de 'oude' variant, met het clusteren van mogelijke
		// varianten

		
		createGeneralWordlist();
		createNgramHM();

		// even alleen de gazlist testen
		//createGazBasedSpelvarHM();

		// createSpelvarHM();
		printVariantList();

		System.out.println("End of spelvar module.");
	}
*/
/*
	
	public void createGeneralWordlist_OUD() {

		System.out.print("Creating combined word list ...");
		// Combineer de wordlists van de train- en testfile in één wordlist
		wordlistTotaalHM.putAll(wordlistfileA_HM);
		// System.out.println("Hallo "+wordlistTotaalHM.size());
		wordlistTotaalHM.putAll(wordlistfileB_HM);
		// System.out.println("Hallo "+wordlistTotaalHM.size());

		// wordlistTotaalHM.putAll(gazetteListHM);

		// testprint
		// System.out.println("Hashmap 'wordlist':");
		// for ( String key : wordlistTotaalHM.keySet() ){
		// Integer value = wordlistTotaalHM.get( key );
		// System.out.println( key + " " + value );
		// }
		// System.out.println("");

		System.out.print("done. Created a combined word list with "
				+ wordlistTotaalHM.size() + " unique words.");
		System.out.println("");
	}

*/

	/*
	// methode aangeroepen bij de trainfile
	// gazettes komen als volgt: "LOC Groot Tjytsjerksteradiel"
	public void readGazettes(BufferedReader in) throws IOException {
		Pattern p = Pattern.compile("^(\\S+)\\s+(.+)$");
		for (String line; (line = in.readLine()) != null;) {
			Matcher m = p.matcher(line);
			if (m.matches()) {
				// String type = m.group(1); // type niet nodig voor spelvar
				String phrase = m.group(2) + gazMark;
				gazetteListHM.put(phrase, 1);
				// gazetteList.add(phrase);
			}
		}
	}

	*/
	
	
	
	/*
	public SpelVarFactory() {
	}

	public SpelVarFactory(ObjectBank<List<CoreLabel>> documents) {

	}
*/
	
	/*****************************************************************************************************************************/
		
	public void setSVparameters(HashMap<String, String> props){
		System.err.println("Alhier in de spelvarfactory, met de spelvarprops");
		for(String key : props.keySet()){
			String val = props.get(key);
			System.err.println(key + "=" + val);
			if(key.toLowerCase().equals("spelvarmaxngramlength")){
				maxSpelvarNGramLength = Integer.parseInt(val);
			}
			if(key.toLowerCase().equals("spelvarminngramlength")){
				minSpelvarNGramLength = Integer.parseInt(val);
			}
			if(key.toLowerCase().equals("spelvarngramoverlapthreshold")){
				ngramOverlapThreshold = Integer.parseInt(val);
			}
			if(key.toLowerCase().equals("spelvarminstringlength")){
				minimalStringLength = Integer.parseInt(val);
			}
			if(key.toLowerCase().equals("spelvarstandardldweight")){
				standardLDWeight = Integer.parseInt(val);
			}
			if(key.toLowerCase().equals("spelvarcomparelowercaseld")){
				compareLowercaseLD = Boolean.parseBoolean(val);
			}
			if(key.toLowerCase().equals("spelvarmaxldthreshold")){
				LDThreshold = Integer.parseInt(val);
			}
		}
	}
	
	public void applySpelVarRulesToDocument(List<? extends CoreLabel> document) {
		int teller = 0;
		//System.err.println("applySpelVarRulesToDocument");
		for (CoreLabel fl : document) {
			String woordOud = fl.get(WordAnnotation.class);		//	get word
			String woordNieuw = getVariant(woordOud);		//	get variant
			if(!woordNieuw.equals(woordOud)){
				fl.set(WordAnnotation.class, woordNieuw);
				String p = fl.get(WordAnnotation.class);
				spelvarChangesHM.put(teller, woordOud);			//	change variant=>word
			}
			teller++;
			String t = removeInitialAndFinalNonWordChars(fl.get(WordAnnotation.class));
			if(!t.equals(fl.get(WordAnnotation.class))){
				fl.set(WordAnnotation.class, t);
			}		
		}
	}

	public void resetSpelVarChanges(List<? extends CoreLabel> document) {
		//System.err.println("resetSpelVarChanges");
		int teller2 = 0;
		for (CoreLabel fl : document) {
			String w1 = fl.get(WordAnnotation.class);
		
			if(spelvarChangesHM.containsKey(teller2)){
				String w = spelvarChangesHM.get(teller2);
				String woordOud = fl.get(WordAnnotation.class);
				fl.set(WordAnnotation.class, w);
			} 
			teller2++;
		}
		spelvarChangesHM.clear();
	}
	
	
}



