package nl.inl.impact.ner.matcher;

import nl.inl.impact.ner.utils.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
//import java.text.Normalizer;	//doesn't work for 1.5
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;


public class MatcherFactory {
	
	//CaseInsensitiveComparator comparator = new CaseInsensitiveComparator();
	
	//holds the main information per loaded NE
	public TreeMap<String,Collection<NEInfo>> NETM = new TreeMap<String,Collection<NEInfo>>();
	public TreeMap<String, String> pairwiseNETM = new TreeMap<String, String>();
	
	//holds the n-grams found in the loaded NE's
	public HashMap<String, ArrayList<String>> ngramHM = new HashMap<String, ArrayList<String>>();
	
	//list of types found in loaded files. Usually PER, LOC and ORG
	public ArrayList<String> NEtypes = new ArrayList<String>();
	
	SemiFoneticTranscriptor sft;
	
	//VARIABLES
	//min and max lengths for ngram creation
	
	public boolean useLemma = false;	//general boolean. If false: compare all NE's with all NE's.
								//if true: use only the NE's marked 'useAsLemma = true'
	
	public HashMap<String, Boolean> filelistHM = new HashMap<String, Boolean>();
	
	int minNLength = 2;
	int maxNLength = 2;
	
	//minimum number of ngrams that have to be shared at first coarse comparison step
	int sharedNgramsMinimum = 2;
	boolean allowSameTypesOnly = true;		//only NE's of same type are compared (PER/LOC/ORG) 
	
	public static int counter = 0;		//keeps track of the matching procedure for the progress bar
	
	public boolean doLoc = false;		//set at the start of matching
	public boolean doOrg = false;
	public boolean doPer = false;
	
	public int numberOfNEsUsedAsLemma = 0;
	
	/* These strings are for dutch only: they convert a found string [0] into [1] for the keyword */
	public String[][] rewriterules = { 
			
			{"St.", "Sint"} 
	
	};
	
	public String[] marker = {"#1", "#2", "#3"};		//the markers for the testphase: #1, #2, #3, for PER
	public boolean matchSurnamesOnly = false;
	

	
	public void totalCleanup(){
		
		/*	Start with this method when the same matching factory object is 
		 * 	used multiple times.
		 */
		
		NETM.clear();
		ngramHM.clear();
		NEtypes.clear();
		pairwiseNETM.clear();
		useLemma = false;
	}
	
	public void variantCleanup(){
		
		/*	This method deletes all the variants that are found after matching,
		 * 	but it leaves intact the NETM list and the uselemma settings.
		 */
		
		for(String word : NETM.keySet()){
			Collection<NEInfo> c = NETM.get(word);
			Iterator<NEInfo> d = c.iterator();	
		    while(d.hasNext()){
		    	NEInfo e = (NEInfo) d.next();
		    	//variants
		    	for(Iterator<String> it = e.variants.keySet().iterator(); it.hasNext(); ){
		    		System.err.println("variant: "+it.next());
		    		it.remove();
		    	}
		    }
		}
		pairwiseNETM.clear();
	}
	
	
	public void loadNEsFromList(TreeMap<String, String> neTM){
		loadNEsFromList(neTM, "<no filename>");
	}	
	
	public void loadNEsFromList(TreeMap<String, String> neTM, String filename){
		for(String line : neTM.keySet()){
			loadNEs(line, filename);
		}
	}
	
	
	
	public void setUseAsLemma(String filename, boolean useAsLem){
		
		/*	Set the correct value for the 'useAsLemma' boolean.
		 * 	Note that an entry can have multiple source files. If one
		 * 	of the files is checked for use a lemma, the entry is marked 
		 * 	as such, even if the word also occurs in files that are not 
		 * 	checked.
		 */
		
		//System.err.println("setLemmaUse. "+filename + "lemma? "+useAsLem);
		filelistHM.put(filename, useAsLem);
		//iterate over the filelist and the lemma settings of all files.
		//if one of the files has lemma use on, set global boolean useLemma = true
		
		useLemma = false;
		for(String a : filelistHM.keySet()){
			System.err.println("Checking file " + a +" for lemma use: " + filelistHM.get(a));
			if(filelistHM.get(a)){
				useLemma = true;
			}
		}
		System.err.println("useLemma: "+useLemma);
		numberOfNEsUsedAsLemma = 0;
		for(String word : NETM.keySet()){
			Collection<NEInfo> c = NETM.get(word);
			Iterator<NEInfo> d = c.iterator();
				
		    while(d.hasNext()){
		    	NEInfo e = (NEInfo) d.next();
		    	for(String files : e.sourcefiles){
		    		System.err.println("files: "+files);
		    		if(files.equals(filename)){
		    			e.useAsLemma = useAsLem;
		    			numberOfNEsUsedAsLemma++;
		    			System.err.println("Word "+word+" from file "+filename+" is marked useAsLemma="+e.useAsLemma);
		    		}
		    	}
		    }
		}
	}
	
	
	//called from matcher from the GUI
	public void loadNEsFromFile(File f){
		String filename = f.getAbsolutePath();
		filelistHM.put(f.getName(), false);
		loadNEsFromFile(filename, f.getName());
	}
	
	//called from NERT with command line use
	public void loadNEsFromFile(String filename, String shortFilename) {

		/* Retrieve NEs from file, "LOC Leiden" */
		System.err.println("Collecting NEs from file ..." +filename);

		//we're adding the type to each NE to enable pseudoduplicate entries
		//(PER Anjou / LOC Anjou
		
		//should work for both tabs and whitespaces
		
		try {
			FileReader doc = new FileReader(filename);
			//FileReader doc = new FileReader(f.getAbsolutePath());
			BufferedReader buff = new BufferedReader(doc);
			boolean eof = false;
			while (!eof) {
				String line = buff.readLine();
				if (line == null) {
					eof = true;
				} else {
					loadNEs(line, shortFilename);
					
				}
			}
			buff.close();
		} catch (IOException e) {
			System.err.println("Woops. Error reading file. " + e.toString());
		}
		System.err.println("Done loading NE's from file "+filename);
	}
	
	
	/* General method, called from either filereader or listreader method */
	
	public void loadNEs(String line, String filename){
		Pattern pa = Pattern.compile("\\w{3}\\s+.+");
		Matcher m = pa.matcher(line);
    	if (m.matches()) {
    		String type = line.substring(0, 3);
    		if(!NEtypes.contains(type)){
    			NEtypes.add(type);
    		}
			String ne = line.substring(4, line.length());
			//here we could have tab-separated variants,
			//find these first
			String[] tabSeparatedVars = ne.split("\\t");
			for(int yep = 0; yep < tabSeparatedVars.length; yep++){
				String key = tabSeparatedVars[0];
				String properString = NERTStringUtils.setProperString(key);
				
				String t = tabSeparatedVars[yep];
				//System.err.println(yep+" "+key +" "+t);
				
				//the first word is the main word, the rest are 
				//given variants
				if(yep==0){
					//now check how this word is presented (ie. comma's)
					
					//testphase: check if there are markers
					/*	In this testphase, PERs can be entered in two ways:
		    		 * 	1) as a single string, e.g. 'Jan van den Boom'
		    		 * 	2) as a string separated by markers for first name, middle part, last name:
		    		 * 		#1Jan#2van den#3Boom
		    		 * 
		    		 * 	NB There is no mark for middle names or extended last names, e.g.
		    		 * 	Jan Piet van den Boom, Jan van den Boom tot Slingeland
		    		 */ 
		    		
		    		//step 1: 	detect markers in the string
		    		boolean hasmarker = hasMarker(properString, marker);
		    		
		    		//step 2: 	if no markers: use entire string as string to match
		    		//			if markers: only use part after '#3' mark, which
		    		//			is the surname.
		    		
		    		//make a temp keyword
		    		String keyword = properString;
		    		
		    		//if there are markers in the string:
		    		if(matchSurnamesOnly){
		    			//user has chosen only to match surnames for PER
			    		if(hasmarker){
			    			//surnames are indicated in the input string with '#3'
			    			//use this information for the keyword
			    	
			    			int i = properString.indexOf(marker[2]);
			    			
			    			//properString = properString.substring(i+marker[2].length());
			    			
			    			//the keyword gets only the substring of marker[2], which
			    			//indicates the surname
			    			keyword = properString.substring(i+marker[2].length());
			    			//step 3: remove the markers from the string
			    			
			    			properString = removeMarkers(properString, marker);
			    		}
			    		else{
			    			//no markers in the inputstring, we
			    			//try to find it with the 'getKeyword' method for PERs
			    			keyword = KeywordConvertor.getKeyword(properString, type);
			    		}
		    		}
		    		else{
		    			//user has chosen to match PERs by the entire string
		    			//there could still be markers in the string, so remove those
		    			properString = removeMarkers(properString, marker);
		    		}
		    		//System.err.println("properString="+properString);
					
					if(!NETM.containsKey(properString+type)){
						//System.err.println("Creating new entry: " + (key+type));
						Collection<NEInfo> neinfo = NETM.get(properString+type);
						if (neinfo == null) {
							neinfo = new HashSet<NEInfo>();
							NETM.put(properString+type, neinfo);
						}
				    	NEInfo info = new NEInfo();
				    	//int r1 = filename.lastIndexOf("_");
				    	//int r2 = filename.lastIndexOf(".");
				    	//info.date = filename.substring(r1+1, r2)+"("+1+")";
				    	info.frequency = 1;
				    	info.type = type;									//a little redundant.
				    	info.normalizedString = NERTStringUtils.normalizeString(properString, rewriterules);
				    	
				    	//if there are markers in the string, use the keyword as the normalizedString:
				    	if(hasmarker){
				    		info.normalizedString = NERTStringUtils.normalizeString(keyword, rewriterules);
				    	}
				    	
				    	if(type.equals("PER")){
				    		//info.keyword = getKeyword(properString, type);
				    		
				    		info.keyword = sft.convertToSemiFoneticTranscription(info.normalizedString);
				    		
				    		//info.keyword = convertToSemiFoneticTranscription(getKeyword(properString, type));
				    		
				    		
				    		//String er = convertToSemiFoneticTranscription(info.keyword);
				    		//String[] test = getPERparts(properString);
				    	}
				    	else{
				    		//info.keyword = info.normalizedString;			//for now
				    		//String er = convertToSemiFoneticTranscription(info.normalizedString);
				    		info.keyword = sft.convertToSemiFoneticTranscription(info.normalizedString);
				    		
				    	}
				    	
				    	//sourcefile
				    	if(!info.sourcefiles.contains(filename)){
				    		//System.err.println("Adding filename for " + ne);
				    		info.sourcefiles.add(filename);
				    	}
				    	//System.err.println(info.normalizedString);
				    	neinfo.add(info);
					}
					else{
						//System.err.println("Adding to Existing entry: " + (key+type));
						//word already exists: add to frequency, sourcefile and add date
						Collection<NEInfo> c = NETM.get(properString+type);
						Iterator<NEInfo> d = c.iterator();	
					    while(d.hasNext()){
					    	NEInfo e = (NEInfo) d.next();
					    	e.frequency++; 
					    	/*
					    	e.date = setDate(filename, e.date);
					    	*/
					    	if(!e.sourcefiles.contains(filename)){
					    		e.sourcefiles.add(filename);
					    	}
					    }
					}
				}
				else{
					//System.err.println("tab-separated variant: "+t);
					Collection<NEInfo> c = NETM.get(properString+type);
					Iterator<NEInfo> d = c.iterator();	
				    while(d.hasNext()){
				    	NEInfo e = (NEInfo) d.next();
				    	if(e.givenVariants.containsKey(t)){
		    				int freq = e.givenVariants.get(t);
		    				freq++;
		    				e.givenVariants.put(t, freq);
				    	}
		    			else{
		    				e.givenVariants.put(t, 1);
		    			}
				    }
				}
			
			}
    	}
	}
	
	
	public boolean hasMarker(String s, String[] marker){
		
		/*	Check if String s contains one of the markers in 
		 * 	array 'marker' 
		 */
		
		boolean hasmarker = false;
		for(int i = 0; i < marker.length; i++){
			if(s.indexOf(marker[i]) >= 0){
				hasmarker = true;
			}
		}
		return hasmarker;
	}
	
	public String removeMarkers(String s, String[] marker){
		
		/*	Removes the markers in String[] marker from String s 
		 *	and replaces them with a whitespace 
		 */
		//@TODO only works if each marker appears once in the string
		//@TODO the whitespace is wrong if there is an apostrophe..
		for(int i = 0; i < marker.length; i++){
			if(s.indexOf(marker[i]) >= 0){
				s = s.replaceAll(marker[i], " ");
				System.err.println("string: "+s);
			}
		}
		return s;
	}
	
	
	public String[] getPERparts(String key){
		

		/*	new, adjusted method for PER. Returns the whole string in five parts:
		 * 	1 - entire string in the proper order (Marc van Bommel)
		 * 	2 - initials, first names
		 * 	3 - middlepart (van de, der, de, inde, uit de, etc.)
		 *  4 - last names (Bommel)
		 * 	5 - extra (jr, dr)
		 *	
		 *	This method should be merged with 'setProperString()', which 
		 *	deals with comma's.	
		 *	For now, we only deal with strings without comma's
		 */
		
		String[] nameparts = new String[5];
		
		//1 check for initials
		//J. Jansen, J.F. Jansen, J. F. Jansen, J Jansen, JF Jansen
		//Pattern pa = Pattern.compile("([A-Z]{1}\\.?\\s?)+");
		//Pattern p = Pattern.compile("([A-Z]+|([A-Z]{1}\\.{1})+|([A-Z]{1}\\s{1})+)");
		//Pattern p = Pattern.compile("([A-Z]+|([A-Z]{1}\\.{1}\\s{0,1})+|([A-Z]{1}\\s{0,1})+|([A-Z]{1}\\.}0,1}\\s{0,1})+)\\s+(.+)");
		//Matcher m = p.matcher(key);
		nameparts[0] = "";
		StringTokenizer st = new StringTokenizer(key);
		Pattern pat = Pattern.compile("([A-Z]{1}\\.{0,1})+");
		while (st.hasMoreTokens()) {
			String str = st.nextToken();
			Matcher ma = pat.matcher(str);
			if(ma.matches()){
				//System.err.println("MATCH for "+key+">"+str+"/" +m.group(1)+"/"+m.group(2));
				nameparts[0] += str;
			}
			//System.err.print(str+"/");
		}
		System.err.println("nameparts[0]: "+nameparts[0]);
		
		
		return nameparts;
	}
	
	
	

	
	
	public String setDate(String filename, String date){
		
		/*	This method is called when a duplicate entry is encountered. 
		 * 	It reads the date string for the original entry,
		 * 	checks if the current date from the filename is already
		 * 	present in this string or not, and returns the modified string.
		 */
		
		int r1 = filename.lastIndexOf("_");
    	int r2 = filename.lastIndexOf(".");
    	String newDate = filename.substring(r1+1, r2);
    	//e.date = oldDate + ", " + filename.substring(r1+1, r2);
		String[] oldDate = date.split(", ");
		//System.err.println("length: "+oldDate.length);
		boolean dateExisted = false;
		for(int da = 0; da < oldDate.length; da++){
			//System.err.println(oldDate[0]);
			String[] dat = oldDate[da].split("\\(");
			if(dat[0].equals(newDate)){
				dateExisted = true;
				int fr = Integer.parseInt(dat[1].substring(0, dat[1].length()-1));
				fr++;
				oldDate[da] = dat[0] + "(" + String.valueOf(fr) + ")";
			}
		}
		if(dateExisted){
			//we've changed a date, now put the whole string back
			for(int da = 0; da < oldDate.length; da++){
				if(da == 0){date = oldDate[da];}
				if(da > 0){date += ", " + oldDate[da];}
			}
		}
		if(!dateExisted){
			//a new date, add to list
			date += ", " + newDate + "(" + 1+ ")";
		}
		return date;
	}
	
	
	
	public boolean initNEMatcher(boolean doLOC, boolean doORG, boolean doPER){
		
		
		//start with a clean-up: if matching has been done already on 
		//a different text, old stuff seems to stick around (the TM variants)
		
		//initialize the transcriptor
		//load content of phonTrans file into List
		//List<String> li = loadPhonTransRules();
		sft = new SemiFoneticTranscriptor();
		sft.initTranscriptor();
		
		doLoc = doLOC;
		doOrg = doORG;
		doPer = doPER;
		
		int w = 0;
		
		for (Iterator<String> it = NETM.keySet().iterator(); it.hasNext();) {
			String s = it.next();
			String type = s.substring(s.length()-3, s.length());
			boolean goOn = false;
        	if((type.equals("LOC"))&&(doLoc)){goOn=true;}
        	else if((type.equals("ORG"))&&(doOrg)){goOn=true;}
        	else if((type.equals("PER"))&&(doPer)){goOn=true;}
        	if(goOn){
				Collection<NEInfo> infos = NETM.get(s);
				for (NEInfo gInfo : infos) {
					//with useLemma = true,
					//we only build n-grams of the lemma's
					//if(useLemma && gInfo.useAsLemma){
						
						//ngram building is done with the keyword
						//buildNGramHM(gInfo.type, gInfo.normalizedString, s);
						if(w==1){System.err.println(">"+s+" "+gInfo.keyword+" w="+w);}
						//if(gInfo.keyword.equals("tromp")){System.err.println(">"+s+" "+gInfo.keyword+" w="+w);}
						
						buildNGramHM(gInfo.type, gInfo.keyword, s);
						
						if(w%1000==0){
							System.err.print("["+w+"]");
						}
					//}
					w++;
				}				
        	}
		}
		System.err.println("Done building NgramHM");
		System.err.println("NgramHM size: "+ngramHM.size());
		/*
		for(String k : ngramHM.keySet()){
			System.err.print(k+" | ");
			ArrayList<String> a = ngramHM.get(k);
			for(String a1 : a){
				System.err.print(a1+"~");
			}
			System.err.println("");
			
		}*/
		
		matchNEs();
		
		/* 	When we're done, we also fill a TM with word <> variant pairs */
		createPairWiseTM();
		System.err.println("Done matching.");
		
		return true;
	}
	
	
	public void printResults(String view){
		/* 	Called from command line NERT, prints results to STDERR
		 * 	view = "p" > pairview
		 *  view = "g" > groupview
		 *  
		 *  useLemma:
		 *  !useLemma: 	print all words and their variants, in group- or pairview
		 *  useLemma: 	print only the words marked !lemma, and their possible lemmas
		 *  			as 'variants'
		 */
		
		System.err.println("uselemma: "+useLemma);
		for(String word : NETM.keySet()){
			boolean goOn = true;
			List<String> vars = new ArrayList<String>();
			String[] t = NERTStringUtils.splitListEntry(word, true);
			Collection<NEInfo> c = NETM.get(word);
			Iterator<NEInfo> d = c.iterator();
			while (d.hasNext()) {
				//go on and collect all the variants, if this word is not a lemma, or if there is no lemma use
				NEInfo e = (NEInfo) d.next();
				if(useLemma && e.useAsLemma){goOn = false;}
				if(goOn){
					for(String a: e.variants.keySet()){
						vars.add(a.substring(0, a.length()-3));
					}
				}
			}
			//System.err.println("goOn="+goOn);
			if(goOn){
				//group view: print word with all variants on one line, tab separated
				if(view.equals("g")){
					String v = "";
					for(String var : vars){
						if(!v.equals("")){ v += "\t"; }
						v += var;
					}
					System.err.println(t[0]+ "\t"+ t[1] + "\t" + v);
				}
				//pair view: print each word-variant pair on a new line	
				else{
					for(String v : vars){
						System.err.println(t[0]+ "\t"+ t[1] + "\t" + v);
					}
					//no variants
					if(vars.size() == 0){
						System.err.println(t[0]+ "\t"+ t[1]);
					}
				}	
			}
		}
	}
	
	
	public void createPairWiseTM(){
		
		/* 	Create a second TM that holds pairs of word <> variants. Because 
		 * 	the TM can only hold unique keys, we add an index to each string.  
		 */
		
		System.err.println("Creating pairwise TM ... ");
		ArrayList <String> tempList = new ArrayList<String>();
		for(String word : NETM.keySet()){
			tempList.clear();
			//String thistype = word.substring(word.length()-3, word.length());
			Collection<NEInfo> c = NETM.get(word);
			Iterator<NEInfo> d = c.iterator();	
		    while(d.hasNext()){
		    	NEInfo e = (NEInfo) d.next();
		    	//given variants
		    	for(Iterator<String> it = e.givenVariants.keySet().iterator(); it.hasNext(); ){
		    		tempList.add(it.next());
		    	}
		    	//matched variants
		    	for(Iterator<String> it = e.variants.keySet().iterator(); it.hasNext(); ){
		    		tempList.add(it.next());
		    	}
		    }
		    int counter = 0;
		    for(String k : tempList){
		    	pairwiseNETM.put(word+"_"+Integer.toString(counter), k);
		    	counter++;
		    	//System.err.println(word+" "+k.substring(0, k.length()-3));
		    }
		    //also put the word in the pairwise TM if it has no variants
		    if(tempList.size()<1){
		    	pairwiseNETM.put(word+"_"+Integer.toString(counter), "");
		    }
		}
		System.err.println("Created pairwise TM of size "+pairwiseNETM.size()+ ". Size of regular NEMT: "+NETM.size());
	}
	
	
	public void buildNGramHM(String type, String str, String NE){
		
		/*	Only a limited (max=8) number of ngrams are used, 
		 * 	See paper by Harding, Croft & Weir.
		 */
		
		//Ngrams are made on the basis of the normalized NE,
		//but the original version of the NE, including the type-tag
		//at the end, is stored in the ngramHM
		//System.err.println("Build Ngram: "+str+" "+NE+" "+type);
		//add begin and end tags to string
		String ngramNE = "<" + str + ">";
		/*
		// calculate max num of n-grams
		int n = getMaxNGramNum(ngramNE);

		// only collect limited number of positions:
		int[] pos = new int[8];

		// first 3
		pos[0] = 0;
		pos[1] = 1;
		pos[2] = 2;
		// last 2
		pos[3] = n - 2;
		pos[4] = n - 1;
		// first of middle three n-grams at:
		pos[5] = ((n - 4) / 3) + 2;
		// second of middle three n-grams at:
		pos[6] = ((n - 4) / 2) + 2;
		// third of middle three n-grams at:
		pos[7] = (((n - 4) / 3) + 2) * 2;
		
		// 	there could be doubles in the pos[],(due to 
		//	limited string length). Make those invalid
		for (int i = 0; i < pos.length; i++) {
			for (int j = 0; j < pos.length; j++) {
				if ((i != j) && (pos[i] == pos[j])) {
					pos[j] = -1;
				}
			}
		}
		*/
		int counter = 0;
		for (int i = 0; i < ngramNE.length(); i++) {
			for (int j = i + minNLength; j <= ngramNE.length(); j++) {
				if (j - i > maxNLength) {
					continue;
				}
				String st = ngramNE.substring(i, j);
				//System.err.println("word: "+str+" ngram: "+st+" counter: "+counter);
				//for (int b = 0; b < pos.length; b++) {
					//if (pos[b] == counter) {
						// 	store this n-gram of the word
						ArrayList<String> ng = ngramHM.get(st);
						if(ng == null){
							ng = new ArrayList<String>();
							ngramHM.put(st, ng);
						}
						if(!ng.contains(NE)){ng.add(NE);}
						//System.err.println("\tword: "+str+" n: "+n+ " b: "+b+" pos[b]: "+pos[b]+" st: "+st);
						//System.err.println("\tword: "+str+" st: "+st);
						//store all ngrams in the NETM as well
						Collection<NEInfo> c = NETM.get(NE);
						Iterator<NEInfo> d = c.iterator();	
					    while(d.hasNext()){
					    	NEInfo e = (NEInfo) d.next();
					    	e.ngrams.add(st);
					    }
					//}
				//}
				counter++;
			}
		}
		
		
	}

	public int getMaxNGramNum(String s) {

	/*
	 * Returns the number of ngrams for a word, based on min-max ngram
	 * length and wordlength.
	 */

		int n = ((s.length() - maxNLength) + 1)
				* ((maxNLength - minNLength) + 1);
		for (int i = (maxNLength - minNLength); i > 0; i--) {
			n += i;
		}
	return n;

}
	

	
	
	public void matchNEs(){
		
		/* 	This is where the actual matching of NE's takes place.
		 * 	This is done is two steps. First, in a coarse step, 
		 * 	all words are collected that share > 1 n-gram. These are
		 * 	put in the ArrayList 'variants' in the NETM for each word.
		 * 	Second, these lists are iterated and in a finer step, 
		 * 	the real variants are picked out.
		 */
		
		/*	In this method, the check for the booleans doLoc, doOrg, doPer
		 * 	is omitted. This check is used in buildNgram(), and therefore
		 * 	all words of a 'wrong' type have no ngram-variants, and will 
		 * 	therefore not be considered in the method below.
		 * 
		 */
		
		/*	If the user has selected one or more of the loaded files in the filelist
		 * 	to be used as 'lemma', only the NE's in this file are considered.
		 * 	If no file is selected as 'lemma', all NE's are considered. 
		 * 
		 */
		
			
		//Note that the word itself will also occur in the variants-list
		System.err.println("Starting matching " + NETM.size() + " NE's ...");
		System.err.println("Use lemma = "+useLemma);
		int counting = 0;
		for(String word : NETM.keySet()){
			if(counting%1000==0){
				System.err.print("["+counting+"]");
			}
			String thistype = word.substring(word.length()-3, word.length());
			System.err.println("WORD: "+word);
			//	get all n-grams from this word
			//	and put them in the variant ArrayList
			Collection<NEInfo> c = NETM.get(word);
			Iterator<NEInfo> d = c.iterator();	
		    while(d.hasNext()){
		    	NEInfo e = (NEInfo) d.next();
		    	
		    	//only continue if keyword is not empty
		    	if(e.keyword.equals("")){
		    		d.remove();
		    	}
		    	else{
			    	/*	Only continue if: 
			    	 * 	1) useLemma == false, or
			    	 * 	2) useLemma == true, and e.useAsLemma == false 
			    	 */
		    		
		    		if( (!useLemma) || ( useLemma && !e.useAsLemma ) ){
		    			
		    			//the for-loop below goes over all the n-grams and fills the current word's
			    		//variant list with a subset of those words that have enough n-grams in common
			    		//with the current word
			    		
		    			/*
		    			if(e.useAsLemma){
		    				System.err.println("\tWord " +word+ " is a lemma.");
		    			}
		    			else{
		    				System.err.println("\tWord " +word+ " is not a lemma.");
		    			}
		    		*/
			    		for(String ngram : e.ngrams){
				    		//iterate over the variants that are found for ngram 'ngram'
				    		ArrayList<String> vars = ngramHM.get(ngram);
				    		//System.err.println("\tngram: "+ngram);
				    		//dump all the variants in ArrayList 'vars' in the ArrayList 'variants'
				    		
				    		//dumping them all in one time gives out-of-bounds problems, so for now
				    		//Collections.copy(e.variants, vars);
						    //we'll do it step by step...
				    		for(String v : vars){
				    			
				    			//System.err.println("\tvariant: "+v);
				    			if(e.variants.containsKey(v)){
				    				//System.err.println("Variant already exists: "+v+"<>"+e.variants.get(v));
				    				//gives first index of ";". The first part of the info is the frequency,
				    				//which we need here, it has the form "f=2"
				    				int freqindex = e.variants.get(v).indexOf(";");
				    				int freq = Integer.parseInt(e.variants.get(v).substring(2, freqindex));
				    				freq++;
				    				String newFreq = "f=" + String.valueOf(freq) + e.variants.get(v).substring(freqindex);
				    				//System.err.println("\t\tExisting variant. Increased ngramfreq for this variant: "+newFreq);
				    				//int freq = e.variants.get(v);
				    				//freq++;
				    				e.variants.put(v, newFreq);
				    			}
				    			else{
				    				//e.variants.put(v, 1);
				    				e.variants.put(v, "f=1;jac=-1;ld=-1;lcs=-1;strdiff=-1");
				    				//System.err.println("\t\tNew variant: "+e.variants.get(v));
				    			}
				    		}
				    	}
				    	
			    		/*
			    		//the method below does not use ngrams, but looks at differences in string length 
			    		//of the keywords and type.
			    		for(String word2 : NETM.keySet()){
			    			//only similar types
		    		    	if(word2.substring(word2.length()-3, word2.length()).equals(thistype)){
					    		Collection c2 = NETM.get(word2);
				    			Iterator d2 = c2.iterator();	
				    		    while(d2.hasNext()){
				    		    	NEInfo e2 = (NEInfo) d2.next();
				    		    	//max difference in string length between the keywords: 3
				    		    	int difflength = Math.abs(e2.keyword.length() - e.keyword.length());  
				    		    	if(difflength <= 3 ){
				    		    		e.variants.put(word2, "f=1;jac=-1;ld=-1;lcs=-1;strdiff=-1");
				    		    	}
				    		    
				    		    }
		    		    	}
			    		}
			    		
			    		*/
			    		
				    	//dump all variants with a different type
				    	if(allowSameTypesOnly){
				    		//String thistype = word.substring(word.length()-3, word.length());
				    		for(Iterator<String> it = e.variants.keySet().iterator(); it.hasNext(); ){
				    			String r = it.next();
				    			if(!r.substring(r.length()-3, r.length()).equals(thistype)){
				    				it.remove();
				    			}
				    		}
				    	}
				    	
				    	//System.err.println("Iterating over all variants");
				    	for(Iterator<String> it = e.variants.keySet().iterator(); it.hasNext(); ){
				    		String r = it.next();
				    		int freqindex = e.variants.get(r).indexOf(";");
		    				int freq = Integer.parseInt(e.variants.get(r).substring(2, freqindex));
		    				if(freq <= sharedNgramsMinimum){
		    					//System.err.println("\t\tVariant with too few n-grams.... word="+word+" "+r+" "+freqindex+" "+freq);
				    		//if(e.variants.get(r) <= sharedNgramsMinimum){
				    			it.remove();
				    		}
				    		else if(r.equals(word)){
				    			it.remove();
				    		}
				    	}
				    	
				    	
				    	
				    	//System.err.println("Number of variants for word "+word+": "+e.variants.size());
				    	
				    	//check
				    	/*
				    	if(word.equals("TROMPPER")){
					    	for(String w : e.variants.keySet()){
					    		int w2 = e.variants.get(w);
					    		System.err.println(w+"("+w2+")");
					    	}
				    	}
				    	*/
				    	
				    	//Step 2: refine the crude variant list by comparing the s-grams.
				    	String w1 = "";
				    	Collection<NEInfo> infoW = NETM.get(word);
						for (NEInfo gInfo : infoW) {
							w1 = gInfo.keyword;
						}
				    	
				    	for(Iterator<String> it = e.variants.keySet().iterator(); it.hasNext(); ){
				    		String r = it.next();
				    		String w2 = "";
				    		boolean variantIsLemma = false;
							Collection<NEInfo> infoR = NETM.get(r);
							for (NEInfo gInfo : infoR) {
								w2 = gInfo.keyword;
								if(gInfo.useAsLemma){
									variantIsLemma = true;
								}
							}
							//System.err.println("\tChecking variant "+r +" lemma? "+variantIsLemma);
							
							//int ld = computeLD(w1, w2); 		//Levenshtein Distance
							int ld = 2;
							//System.err.println("\tuseLemma? " + useLemma+" variantIsLemma? " + variantIsLemma);
							//if useAsLemma=true we only want lemma's as variants.
							if(useLemma && !variantIsLemma){
								ld = 100;
								//System.err.println("A");
							}
							else{
								System.err.println("B");
								/* no match, now try a few allowed substitutes */
								if(w1.equals(w2)){
									ld = 0;
									System.err.println("C");
									//System.err.println("match! Twee gelijke woorden: "+w1+"<>"+w2);
								}
								else if(NERTStringUtils.stringLengthDiff(w1, w2) == 1 ){
									System.err.println("D");
									//System.err.println("diff is 1: "+w1+"<>"+w2);
									//the two strings differ 1 in length, so it could be an insertion, 
									//and we go on
									if(NERTStringUtils.diffIsInsertion(w1, w2, false)){
										System.err.println("E");
										ld = 0;
										//System.err.println("match! insertie: "+w1+"<>"+w2);
									}
									
								}
								
								//System.err.println("geen match. "+w1+"<>"+w2);
							}
							
							
							
				    		//int ld = -1;
							
				    		//double sim = compareGrams(w1, w2);	
				    		//int jac = (int)(100*sim);				//jaccard similarity
				    		int jac = 100;
				    		//turned off because it takes too long now..
				    		//int diff = Math.abs(w1.length() - w2.length());
				    		//int lcs = longestCommonSubstring(w1, w2);					//longest common substring
				    		int diff = -1;
				    		int lcs = -1;
				    		
				    		//double diff = (Math.abs(w1.length() - w2.length())) + 1.0 ;	//diff in string length
				    		//divide 'jac' with the difference in string length:
				    		//int weightedJac = (int) (jac * ( (lcs + 0.0) / w1.length() + 0.0 )); 
				    		//int j2 = (int) (jac / diff);
				    		
				    		//int j2 = (int) (jac / Math.abs(w1.length() - w2.length()));
				    		String variantInfo = e.variants.get(r);
				    		//only the frequency is useful to keep
				    		int freqindex = variantInfo.indexOf(";");
		    				int freq = Integer.parseInt(variantInfo.substring(2, freqindex));
		    				variantInfo = "f=" + String.valueOf(freq) + ";jac=" + jac + ";ld=" + ld + ";lcs=" + lcs + ";strdiff="+diff;
		    				//System.err.println(word+"/"+r+"/"+sim+"/"+jac+"/"+diff+"/"+lcs+"/"+weightedJac);
				    		//e.variants.put(r, (int)(jac));
		    				//only add if ld is below threshold, we use <=2 for now:
		    				if(ld<=1){
		    					System.err.println("\tREAL VARIANT: "+r);
		    					System.err.println("\tNUM OF VARIANTS: "+e.variants.size());
		    				}
		    				else{
		    					//System.err.println("\tNOT A REAL VARIANT: "+r);
		    					it.remove();
		    				}
		    			}
		    		}
		    	}
		    }
			counting++;
		}
		
		/* 	We first do a separate clean-up of the NETM,
		 * 	if useLemma==true, removing all NE's not marked
		 * 	for 'useAsLemma'.
		 */
		
		System.err.println("NETM size: "+NETM.size());
		
		/*
		if(useLemma){
			for (Iterator<String> it = NETM.keySet().iterator(); it.hasNext();) {
				String s = it.next();
				Collection c = NETM.get(s);
				Iterator d = c.iterator();	
			    while(d.hasNext()){
			    	NEInfo e = (NEInfo) d.next();
			    	System.err.println("testje: "+s+" "+e.variants);
			    	//if(!e.useAsLemma){
			    		//it.remove();
			    	//}
			    }
			}
		}
		 */
		System.err.println();
		System.err.println("New NETM size: "+NETM.size());
		
		
	}
	
	
	
	
	public int getCounter(){
		return counter;
	}
	
	
	
	
	
	
	
}

/*
class CaseInsensitiveComparator implements Comparator{

	public int compare(Object o1, Object o2) {
		String s1 = ((String)o1).toLowerCase();
		String s2 = ((String)o2).toLowerCase();
		System.err.println("comparing "+s1+" and "+s2);
		
		System.err.println("B:"+s1.compareTo(s2));
        return s1.compareTo(s2);
	}
	
}

*/

//example for iterating over class within HM:
/*
Collection<NgramInfo> infos = ngramHM.get(k);
System.err.print(k+" | ");
if(infos != null) {
	Iterator gInfo = infos.iterator();	
    while(gInfo.hasNext()){
    	NgramInfo n = (NgramInfo) gInfo.next();
    	for(String a : n.words){
    		System.err.print(a+"~");
    	}
    }
}
System.err.println("");
*/
