package nl.inl.impact.ner.spelvar;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.inl.impact.ner.utils.NERTStringUtils;
import nl.inl.impact.ner.utils.SemiFoneticTranscriptor;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.ShapeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.WordAnnotation;
import edu.stanford.nlp.util.Index;


public class SpelVarFactory {

	int minWordLength = 2; 	//	Do not consider words shorter or equal.
	public String printSpelVarPairs = "";
	int maxWordShapeLength = 5;			//	Seems to be the default case for Stanford
	
	HashMap<String, List<String>> wordlistHM = new HashMap<String, List<String>>();
	HashMap<String, List<String>> wordlistFromFeatureIndexHM = new HashMap<String, List<String>>();
	ArrayList<String> wordsFromFeatureIndexList = new ArrayList<String>();
	
	ArrayList<String> gazetteList = new ArrayList<String>();
	HashMap<String, String> convertedGazetteListHM = new HashMap<String, String>();
	
	ArrayList<String> lowercaseWords = new ArrayList<String>();
	public HashMap<String, String> spelvarHM = new HashMap<String, String>();
	HashMap<String, String> spelvarTrainHM = new HashMap<String, String>();
	HashMap<Integer, String> spelvarChangesHM = new HashMap<Integer, String>();
	
	//Patterns
	Pattern lcPattern = Pattern.compile("[a-z]");
	Pattern gazettePattern = Pattern.compile("^(\\S+)\\s+(.+)$");
	Pattern bioPattern = Pattern.compile("\\s");
	Pattern bioPattern2 = Pattern.compile("^(\\S+)\\s+(.+)$");
	Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");	//	used in string normalization
	
	SemiFoneticTranscriptor sft;
	
	//	This list differs a bit from the default rules in the semiFoneticTranscriptor, because
	//	in spelvar (1) we do not have multiple words (from which we can drop e.g. 'gemeente').
	// 	What it can be used for is to add some rules that will increase the chance of a good
	//	match, e.g. for Dutch 'ene=>een', 'den'=>'de' 
	List<String> defaultPhonTransRules = Arrays.asList(new String[]{
			"sch([^aeiouy])=>s$1",
			"sche?\\b=>s",
			"tz=>z",
			"\\W=>",
			"(\\w)\\1=>$1",
			"eij|ij|ei|y|ey=>Y",
			"(u|a|o)e=>$1",
			"ch=>g",
			"ou|au=>u",
			"z=>s",
			"d\\b=>t",
			"ck=>k",
			"uw=>w",
			"ce=>se",
			"ci=>si",
			"ca=>ka",
			"co=>ko",
			"cu=>ku",
			"c=>k",
			"ph=>f",
			"th=>t",
			"j=>i" 	
	});
	
	public void setParameters(HashMap<String, String> props){
		
		/*	Called from ImpactCRFClassifier during initialization.
		 */
		
		String svPhonTransFile = "";
		
		System.err.println("Setting properties for the spelvar factory");
		for(String key : props.keySet()){
			String val = props.get(key);
			if(key.toLowerCase().equals("printspelvarpairs")){
				System.err.println("Printing spelvarpairs to file "+val);
				printSpelVarPairs = val;
			}
			else if(key.toLowerCase().equals("minwordlength")){
				minWordLength = Integer.parseInt(val);
				System.err.println("Minimal word length="+minWordLength);
			}
			else if(key.toLowerCase().equals("svphontrans")){
				svPhonTransFile = val;
				System.err.println("Phonetic transcription file="+svPhonTransFile);
			}
			else{
				System.err.println("Skipping unknown parameter '" + key + "'");
			}
		}
		//initialize the transcriptor
		//load content of phonTrans file into List
		sft = new SemiFoneticTranscriptor();
		if(!svPhonTransFile.equals("")){
			System.err.println("Phonetic transcription file="+svPhonTransFile);
			List<String> li = loadPhonTransRules(svPhonTransFile);
			sft.initTranscriptor(li);
		}
		else{
			System.err.println("No phonetic transcription file found. Using default rules.");
			sft.initTranscriptor(defaultPhonTransRules);
		}
	}
	
	private List<String> loadPhonTransRules(String filename){
		List<String> phonTransList = new LinkedList<String>();
		//String ruleShape = "=>";
		System.err.print("Loading phonetic transcription rules ...");
		//System.err.println("FILE: >"+phonTransFile+"<");
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
						//lines could start with # or have it added to end of string: chop off
						if (!line.substring(0, 1).equals("#")) {
							//rules are in format A=>B
							int ind = line.indexOf("#");
							if(ind > 0){line = line.substring(0, ind);}
							line = line.trim();
							System.err.println("Found rule: "+line);
							phonTransList.add(line);
						}
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Woops. Error reading file. " + e.toString());
		}
		
		System.err.println("Done. Loaded " + phonTransList.size() + " rules.");
		
		
		//for(String a : phonTransList){
		//	System.err.println(a);
		//}
		return phonTransList;
	}
	
	public void loadWordsFromTxtFile(String filename){
		String s = nl.inl.impact.ner.io.FileHandler.readFile(filename);
		//	Boolean 'false' means no end-of-line tokens are returned, because 
		//	we are only interested in the 'words'. 
		ArrayList<String> list = nl.inl.impact.ner.utils.SimpleTok.simpletokWithNETags(s, false);
		loadWordsFromList(list, filename);
	}
	
	public void loadWordsFromBioString(String s, boolean isGazette){
		//input is biostring. load relevant words
		
		String[] line = s.split("\\n");
		for(String li : line){
			loadWord(li, false);
		}
	}
	
	
	public void loadWord(String line, boolean isGazette){
		if(isGazette){
			Matcher m = gazettePattern.matcher(line);
			if (m.matches()) {
				String phrase = m.group(2);
				gazetteList.add(phrase);
				//String convertedPhrase = sft.convertToSemiFoneticTranscription(normalizeString(phrase));
				String convertedPhrase = normalizeString(phrase);
				if(NERTStringUtils.hasCapitalizedInitial(phrase)){
					convertedPhrase = NERTStringUtils.capitalizeInitial(convertedPhrase);
				}
				convertedGazetteListHM.put(convertedPhrase, phrase);
			}
		}
		else{
			
			/*	Lines can be whitespace or tab-separated.
			 * 	BIO-format can be both "Jan POS B-PER" and "JAN B-PER",
			 * 	so we split with regex on \\s and use the first string 
			 * 	from the resulting array	
			 */
		
			Matcher b = bioPattern2.matcher(line);
			if(b.matches()){
				//String bi = b.group(1);
				//System.err.println("Splitsen bioregel: " + line + " >" + bi + "<");
				//Check type of word: we don't store numbers etc.
				//boolean thisWordisOK = checkWordType(b.group(1));
				if (checkWordType(b.group(1))) {
					//System.err.println("Adding word " + b.group(1));
					addWordToHM(b.group(1), wordlistHM);
				}
			}

			/*
			//Pattern bioPattern = Pattern.compile("\\s");
			if (!line.equals("")) {
				
				
				System.err.println("Splitsen bioregel: " + line);
				String[] c = bioPattern.split(line);
				String s = c[0];
			
				//	Check type of word: we don't store numbers etc.
				boolean thisWordisOK = checkWordType(s);
				if (thisWordisOK) {
					addWordToHM(s, wordlistHM);
				}
			}
			*/
		}
	}
	
	
	
	public void loadWordsFromFile(String file, boolean isGazette){
		
		/* 	Retrieve words from a file, either the train- or test file, or gazette files.
		 * 	This method works for gazette files of format "NE-TYPE word", e.g. "LOC Japan",
		 * 	and for train- and test files of the BIO-type, of the following types:
		 * 		Jan POS B-PER 	(word[whitespace]tag[whitespace]answer)
		 * 		Jan B-PER		(word[whitespace]answer)
		 * 		Jan\tPOS\tB-PER	(word[tab]tag[tab]answer)
		 * 		Jan\tB-PER		(word[tab]answer)
		 */
		
		Matcher lcMatcher;
		System.err.print("Loading words from file " + file + " ... ");
		
		try {
			FileReader doc = new FileReader(file);
			BufferedReader buff = new BufferedReader(doc);
			boolean eof = false;
			while (!eof) {
				String line = buff.readLine();
				if (line == null) {
					eof = true;
				} 
				else {	
					loadWord(line, isGazette);
				}
				
			}
			buff.close();
		} catch (IOException e) {
			System.err
					.println("Woops. Error reading file. "
							+ e.toString());
		}
		if(!isGazette){System.err.print(" done. Total of loaded unique words: "
			+ wordlistHM.size() + "\n");}
		else{System.err.print(" done. Total of loaded gazettes: "+gazetteList.size()+"\n");}		
	}

	
	
	public void loadWordsFromList(ArrayList<String> list, String file) {
		System.err.print("Loading words from file " + file + " ... ");
		for(String s : list){
			boolean thisWordisOK = checkWordType(s);
			if (thisWordisOK) {
				addWordToHM(s, wordlistHM);
			}
		}
		System.err.print(" done. Total of loaded unique words: " + wordlistHM.size() + "\n");
	}
	
	
	private void addWordToHM(String s, HashMap<String, List<String>> hm) {
		Matcher lcMatcher;
		String s2 = s;
		//	If s2 is CAPITALIZED, turn into initial capital only
		if(NERTStringUtils.isCapitalized(s)){
			s2 = NERTStringUtils.decapitalizeString(s); 
			s2 = NERTStringUtils.capitalizeInitial(s2);
		}
		//String s3 = sft.convertToSemiFoneticTranscription(normalizeString(s2));
		String s3 = normalizeString(s2);
		if(NERTStringUtils.hasCapitalizedInitial(s2)){
			s3 = NERTStringUtils.capitalizeInitial(s3);
		}
		//System.err.println("s="+s+" s2="+s2+" s3="+s3);
		if(hm.containsKey(s3)){
			List<String> val = hm.get(s3);
			if(!val.contains(s)){
				val.add(s);
				hm.put(s3, val);
			}
		}
		else{
			List<String> val = new ArrayList<String>();
			val.add(s);
			hm.put(s3, val);
		}
		
		lcMatcher = lcPattern.matcher(s.substring(0,1));
		if (lcMatcher.matches()) {
			lowercaseWords.add(s);
		}
	}
	
	
	
	
	public void loadWordsFromFeatureIndex(Index<String> featureIndex) {

		/*	Retrieve words from trainfile. Only works when 
		 *	flag 'useWords' is true. The words are stored in a special
		 *	HM.
		 */
	
		System.err.print("Loading words from training file ...");
		Pattern pa = Pattern.compile("[a-z]");
		Matcher ma;
		
		Iterator<String> it = featureIndex.iterator();
		String wordIdentifier = "-WORD|C";
		while (it.hasNext()) {
			String s = (String) (it.next());
			if (s.length() > 7) {
				String p = s.substring(s.length() - 7, s.length());
				if (p.equals(wordIdentifier)) {
					// 	Limit useless words
					boolean thisWordisOK = checkWordType(p);
					if (thisWordisOK) {
						
						String n = s.substring(0, s.length() - 7); //+ trainMark;
						if (thisWordisOK) {
							wordsFromFeatureIndexList.add(n);
							addWordToHM(n, wordlistFromFeatureIndexHM);
						}
					}
				}
			}
		}
		System.err.print(" done. Loaded " + wordlistFromFeatureIndexHM.size()
				+ " unique words.\n");
	}
	
	public void loadGazettesFromModel(List<String> gaz){
		
		/*	Get gazetteerlists from file and store 
		 * 	them in 'gazetteListHM' 
		 */ 
		
		System.err.print("Loading gazettes ...");	
		for (String word : gaz) {
			gazetteList.add(word);
			String convertedWord = normalizeString(word);
			if(NERTStringUtils.hasCapitalizedInitial(word)){
				convertedWord = NERTStringUtils.capitalizeInitial(convertedWord);
			}
			convertedGazetteListHM.put(convertedWord, word);
		}
		System.err.print(" done. Loaded " + gazetteList.size()
				+ " gazettes.\n");
	}
	
	public void loadSpelVarPairs(List<String> spelvarlist){
		System.err.print("Loading spelling variation pairs from training ...");
		for(String a : spelvarlist){
			String[] b = a.split("@@@@");
			spelvarTrainHM.put(b[0], b[1]);
		}
		System.err.print(" done. Loaded " + spelvarTrainHM.size()+" spelling variation pairs.\n");
	}
	
		
	public void findSpelVars(){
		System.err.print("Spelvar2\nIterating over " + wordlistHM.size() + " words and " + gazetteList.size()
				+ " gazettes to create variant-word pairs ... \n");
	
		
		for (String key : wordlistHM.keySet()) {
			
			List<String> val = wordlistHM.get(key);
		
			//option 1: spelvarrules were created for the trainfile. Look there first:
			//if(spelvarTrainHM.size() > 0){
		/*
				System.err.println("Case: one word and there are spelvarTrain rules");
				Set<Map.Entry<String, String>> trainset = spelvarTrainHM.entrySet();
				String testw = val.get(0);
				String spelvarValue = "";
				
				for (Map.Entry<String, String> trainvalues : trainset) {
					if(trainvalues.getKey().equals(testw)){
						System.err.println("Found an applicable spelvartrain KEY for testword "+testw+": "+trainvalues.getKey()+"=>"+trainvalues.getValue());
						spelvarValue = trainvalues.getValue();
					}
				}
				if(!spelvarValue.equals("")){
					System.err.println("Found a spelvarvalue, now create rules");
					if(!testw.equals(spelvarValue)){
						System.err.println("Created spelvarpair "+testw+"=>"+spelvarValue);
						spelvarHM.put(testw, spelvarValue);
					}
				}
			//}
			*/
			//else{
				//System.err.println("one testword, no trainfilewords...");
				
				/*	If we are here, we have a situation in testing with one testword and no candidates
				 * 	from the trainfile. We now only create a rule if the word is UPPERCASE:
				 * 	UPPERCASEWORD => Uppercaseword
				 */
				
			//	for(String onlyWord : val){
			//		if(NERTStringUtils.isCapitalized(onlyWord)){
			//			spelvarHM.put(onlyWord, NERTStringUtils.capitalizeInitial(NERTStringUtils.decapitalizeString(onlyWord)));
						//System.err.println("\t\t\t\t\tCreated rule "+onlyWord+"=>"+NERTStringUtils.capitalizeInitial(NERTStringUtils.decapitalizeString(onlyWord)));
			//		}
			//	}	
			//}
		

		
			/* 'key' is a normalized version of all NEs in the list 'val' */
			System.err.println("----\n"+val+" ("+key+")");
			for(String v : val){
				if(!v.equals(key)){
					System.err.println("Created spelvarpair "+v+"=>"+key);
					spelvarHM.put(v, key);					
				}
				
			}
			
		}
	}
	
	
	public void findSpelVarsTest(){
		
		/*	This is the main method in which spelling variants are located
		 * 	and rewrite rules are created.
		 * 	All words that are stored as values with the same keys (which
		 * 	give the semi-fonetic transcription of all these words)
		 * 	are considered variants. The main challenge is to select the 
		 * 	proper word from the values-list to be used as the 'main' variant
		 * 	to which all other words have to be rewritten.
		 * 	If there are gazettes, we try to find a word that is also a 
		 * 	gazette. If we are extracting, we try to find a word from 
		 * 	the training input. Else, we choose a random word.
		 * 	If only one word is listed as a value and this word is CAPITALIZED,
		 * 	we create spelvar pair CAPITALIZED => Capitalized
		 */
		
		System.err.print("Iterating over " + wordlistHM.size() + " words and " + gazetteList.size()
				+ " gazettes to create variant-word pairs ... ");
	
		
		for (String key : wordlistHM.keySet()) {
			
			List<String> val = wordlistHM.get(key);
			
			/* 'key' is a normalized version of all NEs in the list 'val' */
			//System.err.println("----\n"+val+" ("+key+")");
			
			/*	
			 * 	First check if there is a word in the gazette list with the
			 * 	same converted shape. If so, this is the main word all variants
			 * 	have to be converted to.
			 */
			
			if(convertedGazetteListHM.containsKey(key)){
				for(String vars : val){
								
					//	Do not create pair if key and value are the same
					if(!vars.equals(convertedGazetteListHM.get(key))){
						
						/* 	Do not create pair if the main word is written with a
						 * 	capital, and if vars also occurs as 
						 *  lowercase word - this is in an indication
						 *  that the word is accidentally written with 
						 *  a capital, e.g. because it is sentence-initial.
						 *  
						 *  E.g. 'kent' vs. 'Kent' in Dutch
						 */
						
						boolean yes = NERTStringUtils.hasCapitalizedInitial(convertedGazetteListHM.get(key));
						if(yes && lowercaseWords.contains(vars)){}
						else{
							spelvarHM.put(vars, convertedGazetteListHM.get(key));
						}
					}
				}
			}
			
			else{
	
				/*	Here we check words with other words. There are three options:
				 * 	1) 	during training: compare all words in list 'var' with each other
				 * 	2) 	during testing: spelvarpairs have been created during training: use these as base
				 * 		and else, base new ones on the words in the trainfile as much as possible
				 * 	3)	during testing: no spelvarpairs have been created during training: use words
				 * 		from train file as bas.
				 */
				
				/*	If we find a word with only one variant, check if this word is capitalized. If so,
				 * 	we create a spelvar rule which converts it to an initial capital shape only.
				 * 	e.g. SPAIN > Spain
				 */
				
				/*
				if(val.size() == 1){
					for(String onlyWord : val){
						if(NERTStringUtils.isCapitalized(onlyWord)){
							spelvarHM.put(onlyWord, NERTStringUtils.capitalizeInitial(NERTStringUtils.decapitalizeString(onlyWord)));
							System.err.println("c)");
						}
					}
				}
				*/
				
				if(val.size() == 1){
					//System.err.println("one word.");
					//only one word. if we're training, do nothing (since there are no alternatives). If we're testing, 
					//look for a word in the trainfile
					
					//option 1: spelvarrules were created for the trainfile. Look there first:
					if(spelvarTrainHM.size() > 0){
						
						/*	Here, we check if a spelvarTrainHM rule exists.
						 * 	If a rule exists key => value and the value is the 
						 * 	testword, we are fine.
						 * 
						 * 	If a rule exists key => value and the key is the
						 * 	testword, we choose the value from this pair and use this 
						 * 	as our value for a new rewrite rule for the testword.
						 * 
						 */
						
						//System.err.println("Case: one word and there are spelvarTrain rules");
						Set<Map.Entry<String, String>> trainset = spelvarTrainHM.entrySet();
						String testw = val.get(0);
						String spelvarValue = "";
						
						for (Map.Entry<String, String> trainvalues : trainset) {
							if(trainvalues.getKey().equals(testw)){
								//System.err.println("Found an applicable spelvartrain KEY for testword "+testw+": "+trainvalues.getKey()+"=>"+trainvalues.getValue());
								spelvarValue = trainvalues.getValue();
							}
						}
						if(!spelvarValue.equals("")){
							//System.err.println("Found a spelvarvalue, now create rules");
							if(!testw.equals(spelvarValue)){
								System.err.println("Created spelvarpair "+testw+"=>"+spelvarValue);
								spelvarHM.put(testw, spelvarValue);
							}
						}
					}
					
					//option 2: no spelvarrules were created during training. Look in featureindex of trainfile for words
					if(spelvarTrainHM.size() == 0){
						//System.err.println("No spelvarrules from training.");
						if(wordlistFromFeatureIndexHM.containsKey(key)){
							List<String> trainwords = wordlistFromFeatureIndexHM.get(key); 
							//System.err.println("train words with key "+key+": "+trainwords);
							if(trainwords.size() == 1){
								//	Simple case: we have one word from the testfile and one matching word from the trainfile
								for(String a : trainwords){
									for(String b : val){
										if(!a.equals(b)){
											//if( (NERTStringUtils.isCapitalized(a) && NERTStringUtils.isCapitalized(b)) || ( !NERTStringUtils.isCapitalized(a) && !NERTStringUtils.isCapitalized(b))){
												spelvarHM.put(b, a);
												//System.err.println("\tCreated rule "+b+"=>"+a);
											//}
										}
									}
								}
							}
							else{	
								
								/* 	A more tricky case. We have one word from the testfile and multiple words from the trainfile.
								 * 	An example is a) "insgelyks" <> "Insgelijks", "insgelyks", "insgelijks"
								 * 	Another is: b) "insgelyks" <> "Insgelijks", "Insgelyks", "insgelijks"
								 * 
								 * 	Since capitals are an important feature for Stanford, we do not create rewriterules that change
								 * 	only capitals. We therefore only create a rule for case (b): insgelyks > insgelijks
								 */
								for(String a : trainwords){
									for(String b : val){
										//System.err.println("\ttrainword="+a+" testword="+b);
										if(!a.equals(b)){
											if( (NERTStringUtils.hasCapitalizedInitial(a) && NERTStringUtils.hasCapitalizedInitial(b)) || ( !NERTStringUtils.hasCapitalizedInitial(a) && !NERTStringUtils.hasCapitalizedInitial(b))){
												spelvarHM.put(b, a);
												//System.err.println("\t\tCreated rule "+b+"=>"+a);
											}
										}
									}
								}
								
								
							}
						}
						else{
							//System.err.println("one testword, no trainfilewords...");
							
							/*	If we are here, we have a situation in testing with one testword and no candidates
							 * 	from the trainfile. We now only create a rule if the word is UPPERCASE:
							 * 	UPPERCASEWORD => Uppercaseword
							 */
							
							for(String onlyWord : val){
								if(NERTStringUtils.isCapitalized(onlyWord)){
									spelvarHM.put(onlyWord, NERTStringUtils.capitalizeInitial(NERTStringUtils.decapitalizeString(onlyWord)));
									//System.err.println("\t\t\t\t\tCreated rule "+onlyWord+"=>"+NERTStringUtils.capitalizeInitial(NERTStringUtils.decapitalizeString(onlyWord)));
								}
							}	
						}
					}
					else{
						//System.err.println("\nthere are spelvarrules from training");
					}
					
					
				}
		
				
				//	We only come here if there is more than one variant
				else if(val.size() > 1){
				
					//System.err.println("more than one word");
					
					/*	We are here if there are multiple matcher words. If we're testing, we first need to check
					 * 	if there are spelvarrules from the train file.
					 * 
					 */
					
					boolean doneHere = false;
					if(spelvarTrainHM.size() > 0){
						//System.err.println("there are spelvarrules from training..."+spelvarTrainHM.size());
						
						/*	Here, we check if a spelvarTrainHM rule exists.
						 * 	If a rule exists key => value and the value is one of the 
						 * 	testwords, we choose this testword as the value for a rewriterule
						 * 	and the other testwords as key.
						 * 
						 * 	Else if a rule exists key => value and the key is one of the
						 * 	testwords, we choose the value from this pair and use this 
						 * 	as our value for new rewrite rules for the testwords.
						 * 
						 *  Else: there is no correspondence between the trainRules and the testwords.
						 *  The next step is to look if there are single trainwords that correspond
						 *  to the testwords. If not, we link the testwords with themselves.
						 */
						
						String spelvarValue = "";
						Set<Map.Entry<String, String>> trainset = spelvarTrainHM.entrySet();
						
						for(String testw : val){
							for (Map.Entry<String, String> trainvalues : trainset) {
								if(trainvalues.getValue().equals(testw)){
									//System.err.println("Found an applicable spelvartrain VALUE for testword "+testw+": "+trainvalues.getKey()+"=>"+trainvalues.getValue());
									spelvarValue = testw;
								}
								if(trainvalues.getKey().equals(testw)){
									//System.err.println("Found an applicable spelvartrain KEY for testword "+testw+": "+trainvalues.getKey()+"=>"+trainvalues.getValue());
									spelvarValue = trainvalues.getValue();
								}
							}
						}
						if(!spelvarValue.equals("")){
							doneHere = true;
							//System.err.println("Found a spelvarvalue, now create rules");
							for(String testw : val){
								if(!testw.equals(spelvarValue)){
									//System.err.println("\tCreated spelvarpair "+testw+"=>"+spelvarValue);
									spelvarHM.put(testw, spelvarValue);
								}
							}
						}
						
					}
					if( (spelvarTrainHM.size() == 0) || (!doneHere)){
						//	There are no spelvarrules from the trainfile.
						//System.err.println("No useful spelvarrules from training");
						if(wordlistFromFeatureIndexHM.containsKey(key)){
							List<String> trainwords = wordlistFromFeatureIndexHM.get(key); 
							//System.err.println("train words with key "+key+": "+trainwords);
							
							/*	The only test we need to do here is to see if there are test words that are
							 * 	not present in the trainwords list. If so, we rewrite these to 
							 * 	another testword that IS present in the trainwords list. If no testword
							 * 	is present in the trainwords list, we create a rule to link the testwords,
							 * 	to a trainword.
							 * 	
							 * 	e.g. 	TEST				TRAIN
							 * 			vrienden, vrinden	vronden, vranden
							 * 
							 * 			rules: 	vrienden => vronden
							 * 					vrinden	 => vronden
							 * 
							 * 
							 * 			TEST				TRAIN
							 * 			vrienden, vrinden	vrenden, vroenden
							 * 			vroenden, vrenden
							 * 
							 * 			rules: vrienden => vrinden
							 * 
							 * 			TEST				TRAIN
							 * 			vrienden, vrinden	vronden, vrinden
							 * 			
							 * 			rules: vrienden => vrinden
							 */
							
							ArrayList<String> nonMatches = new ArrayList<String>();
							String testmatch = "";
							
							for(String testw : val){
								//for(String trainw : trainwords){
								//System.err.println("testw="+testw+" trainw="+trainw);
								if(!trainwords.contains(testw)){
									nonMatches.add(testw);
								}
								else{
									testmatch = testw;
									//System.err.println("testmatch="+testmatch);
								}
								//randomtrainword = trainw;
							//}
							}
							if(nonMatches.size() == val.size()){
								//System.err.println("no match between testwords and trainwords");
								String randomtrainword = "";
								for(String trainw : trainwords){
									randomtrainword = trainw;
								}
								//	None of the testwords matches a trainword.
								//	Pick a random trainword and connect all testwords to this trainword.
								for(String nonmatch : nonMatches){
									spelvarHM.put(nonmatch, randomtrainword);
									//System.err.println("\t\tCreated rule "+nonmatch+"=>"+randomtrainword);
								}
							}
							else{
								//System.err.println("at least one match between testwords and trainwords");
								//	Case: at least one of the testwords matches a trainword.
								for(String nonmatch : nonMatches){
									spelvarHM.put(nonmatch, testmatch);
									//System.err.println("\t\tCreated rule "+nonmatch+"=>"+testmatch);
								}
								
							}
						}
						else{
							//	If we are here, we have multiple test words but no train words to compare them to.
							//	We choose a random word from the test words and use this as value for the rewrite rules.
							//System.err.println("train words do not contain key "+key);
							String spelvarKey = "";
							
							Random rdm = new Random();
							int r = rdm.nextInt(val.size());
							spelvarKey = val.get(r);
							int tel = 0;
							
							while(tel < 10 && (NERTStringUtils.isCapitalized(spelvarKey) || containsDiacriticsOrApostrophes(spelvarKey)) ){
								r = rdm.nextInt(val.size());
								spelvarKey = val.get(r);
								tel++;
							}
							
							for(String vars : val){
								if(!spelvarKey.equals(vars)){
									//System.err.println("\tCreated spelvarpair "+vars+"=>"+spelvarKey);
									spelvarHM.put(vars, spelvarKey);
								}
							}
						}
					}
				}
			}
		}
		System.err.print(" done.\nCreated " + spelvarHM.size() + " spelling variation pairs.\n");
	}
	
	
	
	
	
	public boolean checkWordType(String w) {
		
		/* 	This method filters out useless words. Called
		 * 	when loading words into wordlistHM.
		 */
		
		boolean gaVerder = true;
		Pattern pa;
		Matcher m;

		/* 	No words shorter than minimal ngram-length */
		if (w.length() < minWordLength) {
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
	
	
	public boolean containsDiacriticsOrApostrophes(String s){
		String s2 = normalizeString(s);
		if(s.equals(s2)){
			return false;
		}
		else{
			return true;
		}
	}
	
	
	private String normalizeString(String sOld) {

		/*
		 * This method normalizes strings in a series of steps.
		 */
		String sNew = sOld;
		// change to lowercase (e.g. Zuid-Holland > zuid-holland)
//		//sNew = sOld.toLowerCase();
		//sNew = Normalizer.normalize(sNew, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		sNew = sft.convertToSemiFoneticTranscription(sNew);

		//if(!sNew.equals(sOld)){System.out.println("old >" + sOld +"< new >"+sNew+"<");}
		return sNew;
	}

	
	
	public static String removeInitialAndFinalApostrophes(String s) {

		/*
		 * Remove apostrophe from beginning and end of words
		 * with words of > 2 characters
		 */

		if (s.length() > 2) {

			int ap = s.indexOf('\'');
			if (ap == (s.length() - 1)) {
				s = s.substring(0, (s.length() - 1));
			}

		}
		return s;
	}
	
	
	//	Kan waarschijnlijk weg.
	public List<CoreLabel> applySpelVarRulesToDocument2(List<CoreLabel> document) {
		int teller = 0;
		//System.err.println("applySpelVarRulesToDocument");
		for (CoreLabel fl : document) {
			String woordOud = fl.get(WordAnnotation.class);		//	get word
			String woordNieuw = getVariant(woordOud);		//	get variant
			if(!woordNieuw.equals(woordOud)){
				System.err.println("oud: "+woordOud+" nieuw: "+woordNieuw);
				fl.set(WordAnnotation.class, woordNieuw);
				String p = fl.get(WordAnnotation.class);
				spelvarChangesHM.put(teller, woordOud);			//	change variant=>word
			}
			teller++;
			/*
			String t = removeInitialAndFinalNonWordChars(fl.get(WordAnnotation.class));
			if(!t.equals(fl.get(WordAnnotation.class))){
				fl.set(WordAnnotation.class, t);
			}
			*/		
		}
		return document;
	}
	
	
	
	//copied partly from stanford process/WordShapeClassifier
	private String getWordShape(String s, Set<String> knownLCWords) {
		int len = s.length();
		
		//max length for wordshape seems to be 5, plus 1 longer for markKnownLC
		if (len > maxWordShapeLength){ len = maxWordShapeLength;}
		
		int sbLen = (knownLCWords != null) ? len + 1: len;  // markKnownLC makes String 1 longer
	    final StringBuilder sb = new StringBuilder(sbLen);
	    boolean nonLetters = false;
	    for (int i = 0; i < len; i++) {
	      char c = s.charAt(i);
	      char m = c;
	      if (Character.isDigit(c)) {
	        m = 'd';
	      } else if (Character.isLowerCase(c)) {
	        m = 'x';
	      } else if (Character.isUpperCase(c) || Character.isTitleCase(c)) {
	    	  //m = 'X';						//aanpassing FL: toon geen hoofdletters midden in het woord (XxxXX)
	    	  if(i==0){
	    		  m = 'X';
	    	  }
	    	  else{
	    		  m = 'x';
	    	  }
	      } else {								//toevoeging FL: OCR-fouten geven anders te veel rare tekens in de wordshape ('Xxx/-xx')
	    	  if((c!='.')&&(i<(len-1))){
	    		  m = 'x';
	    	  }
	      }
	      if (m != 'x' && m != 'X') {
	        nonLetters = true;
	      }

	      sb.append(m);
	    }

	    if (knownLCWords != null) {
	      if ( ! nonLetters && knownLCWords.contains(s.toLowerCase())) {
	        sb.append('k');
	      }
	    }
	    //System.out.println(s + " became " + sb);
	    return sb.toString();
	  }
	
	
	
	public List<? extends CoreLabel> applySpelVarRulesToDocument(List<? extends CoreLabel> document, Set<String> knownLCWords) {
		int teller = 0;
		for (CoreLabel fl : document) {
			String woordOud = fl.get(WordAnnotation.class);		//	get word
			String shapeOud = fl.get(ShapeAnnotation.class);
			String woordNieuw = getVariant(woordOud);		//	get variant
			if(!woordNieuw.equals(woordOud)){
				/*	We also change the shapeannotation according
				 * 	to the new word. 
				 */
				String shape = getWordShape(woordNieuw, knownLCWords);
				fl.set(ShapeAnnotation.class, shape);
				
				fl.set(WordAnnotation.class, woordNieuw);
				String p = fl.get(WordAnnotation.class);
				spelvarChangesHM.put(teller, woordOud);			//	change variant=>word
				
			}
			
			teller++;
		}
		return document;
	}
	
	public String getVariant(String w1) {
		
		String w2 = w1;
		if (spelvarHM.containsKey(w1)) {
			w2 = spelvarHM.get(w1);
		} else {
		}
		return w2;
	}

	public List<? extends CoreLabel> resetSpelVarChanges(List<? extends CoreLabel> document) {
		
		/*	After every sentence (document) we put back the changes in the WordAnnotation.
		 * 	Notice that the ShapeAnnotation might also have changed, but we leave this
		 * 	change the way it is, since it is not printed anyway.
		 */
		
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
		return document;
	}
	
	public void findVariants(List<CoreLabel> doc, int docSize, Set<String> knownLCWords) {
		//System.err.println("dostuff3");
		for (int j = 0; j < docSize; j++) {
			String woordOud = doc.get(j).get(WordAnnotation.class);		
			String woordNieuw = getVariant(woordOud);
			if(!woordNieuw.equals(woordOud)){
				if(knownLCWords.contains(doc.get(j).get(WordAnnotation.class))){		//change knownLCWords if necessary
					knownLCWords.remove(doc.get(j).get(WordAnnotation.class));
					knownLCWords.add(woordNieuw);
				}
				doc.get(j).set(WordAnnotation.class, woordNieuw);
			}
		}
	}
	
	public void writePairListToFile(String filename, boolean train){
		
		FileOutputStream out;
		PrintStream p;
		String fileOut = printSpelVarPairs;
		if(!filename.equals("")){
			fileOut = filename;
		}	
		if(fileOut.equals("")){
			fileOut = "SpelvarPairsList.txt";
		}
		System.err.println("Writing spelling variation pairlist to file " + fileOut);
		
		try
        {
        	out = new FileOutputStream(fileOut);
        	p = new PrintStream( out );   
        	//print gebruikte regels
        	p.println("## USED SPELVAR RULES");
        	for(int i = 0; i < sft.ruleArray.length; i++){
    			p.println(sft.ruleArray[i][0] + "=>" + sft.ruleArray[i][1]);
        	}
        	
        	
        	if(spelvarTrainHM.size()>0){
	        	p.println("## SPELVARPAIRS FROM TRAINER");
	        	Iterator<Entry<String, String>> it = spelvarTrainHM.entrySet().iterator();
	            while (it.hasNext()) {
	            	Map.Entry<String, String> pairs = (Map.Entry<String, String>)it.next();
	            	p.println(pairs.getKey() + "=>" + pairs.getValue());
	            }
        	}
        	if(train){p.println("## SPELVARPAIRS FROM TRAINER");}
        	else{p.println("## SPELVARPAIRS FROM EXTRACTOR");}
            Iterator<Entry<String, String>> itr = spelvarHM.entrySet().iterator();
            while (itr.hasNext()) {
            	Map.Entry<String, String> pairs = (Map.Entry<String, String>)itr.next();
             	p.println(pairs.getKey() + "=>" + pairs.getValue());
            }
           
            p.close();
            
        }
        catch (Exception e)
        {
        	System.err.println ("  Error writing spelling variation pairlist to file: "+e);
        }     
        
	}
	
	
}
