package nl.inl.impact.ner.utils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConvertTxt2Bio {
	
	public static String EOL = "$$$$$$EOL$$$$$$";
	private static String NEOpenTag = "";
	private static String NECloseTag = "";
	
	
	//TODO fix this regex for other tags than <NE_PER|LOC|ORG > </NE>
	//e.g. <NE type="PER">
	static Pattern regexNE = Pattern.compile("<NE_(\\w+)([^>*])>\\s*(.*)</\\w+>\\s*$");
	
	static String NEStartTag = "<NE_";
	static String groupedNEStartTag = "(<NE)";
	static String NEStartTag2 = ">";
	static String NEEndTag = "</NE";
	static String groupedNEEndTag = "(</NE>)";
	
	static String refStartTag = "<REF_";		//is used to mark original gold answer tags			
	static String refEndTag = "</REF>";
	
	
	private static void createTagPattern(){
		
		/*	This method sets the regex pattern with which NEs are recognized
		 * 	from text or xml format. The default pattern recognizes <NE_tag> only	
		 */
		
		//System.err.println("CreateTagPattern() with NEtag1: "+NEOpenTag+" and NEtag2: " + NECloseTag);
		String regexNEString = "";
		if(NEOpenTag.equals("") && NECloseTag.equals("")){
			regexNEString = "<NE_(\\w+)([^>]*)>\\s*(.*)</\\w+>\\s*$";
		}
		else{
			if(!NEOpenTag.equals("")){
				
				/*	Look for the word 'TAG' in the given tagString and get
				 * 	its position. 
				 * 	The tagstring is supposed to hold 'TAG', e.g. <NE_TAG> or <NE type="TAG">
				 */
				
				int openTagIndex = NEOpenTag.indexOf("TAG");
				String tagFollowUpChar = "";
				if(openTagIndex > -1){
					NEStartTag = NEOpenTag.substring(0, openTagIndex);		//gives <NE_ or <NE type="
					groupedNEStartTag = "(" + NEStartTag + ")";
					regexNEString = NEStartTag + "(\\w+)";
					if(NEOpenTag.length() > openTagIndex+3){
						tagFollowUpChar = NEOpenTag.substring(openTagIndex+3, openTagIndex+4); //gives the char following 'TAG' 
						//System.err.println("tagFollowUpChar: "+tagFollowUpChar);
					}
					if(tagFollowUpChar.equals("\"")){
						regexNEString += "\"+";
					}
					//else{
					//	regexNEString += "\\s*";
					//}
					//regexNEString += "(\\s+.+)?";
					regexNEString += "([^>]*)";
					regexNEString += ">\\s*(.*)";
					//regexNEString += "\\s*$";
					
					if(NEStartTag.equals("<")){
						//case when we have <TAG>
						NEStartTag = NEStartTag + "[^>/]+";
						groupedNEStartTag = "(" + NEStartTag + ")";
					}
					//System.err.println("NEStarttag: "+NEStartTag);
					//System.err.println("groupedNEStarttag: "+groupedNEStartTag);
					
					if(!tagFollowUpChar.equals(">")){
						NEStartTag2 = tagFollowUpChar + ">"; 
					}
				}
				else{
					//we have a tag that's always the same..
					NEStartTag = NEOpenTag;
					groupedNEStartTag = "(" + NEStartTag + ")";
					regexNEString = NEStartTag + "([^>\"]+)";
					//regexNEString += "(\\s+.+)?";
					regexNEString += "([^>]*)";
					regexNEString += ">\\s*(.*)";
					
					//System.err.println("NEStarttag: "+NEStartTag);
					//System.err.println("groupedNEStarttag: "+groupedNEStartTag);
				}
			}
			
			if(NECloseTag.equals("")){
				regexNEString += NEEndTag + ">";
				regexNEString += "\\s*$";
				
			}
			else{
				int closeTagIndex = NECloseTag.indexOf("TAG");
				if(closeTagIndex == -1){
					//there is no tag in the closing tag, so we can just use the entire tag, which is simple
					NEEndTag = NECloseTag;
					groupedNEEndTag = "(" + NEEndTag + ")";
					//System.err.println("NEEndTag: "+NEEndTag);
					regexNEString += NEEndTag;
					regexNEString += "\\s*$";
					
					if(NEEndTag.charAt(NEEndTag.length()-1) == '>'){
						NEEndTag = NEEndTag.substring(0, NEEndTag.length()-1);
					}
					//System.err.println("NEEndTag: "+NEEndTag);
					//System.err.println("GroupedNEEndTag: "+groupedNEEndTag);
				}
				else{
					//this is more complicated: the actual NE-tag returns in the closing tag, e.g. <LOC>Leiden</LOC>
					//for which we use a backreference to group 1, but only for the regexString
					//String NEEndTag1 = NECloseTag.substring(0, closeTagIndex); //gives the part before TAG, e.g. '</'
					//String NEEndTag2 = NECloseTag.substring(closeTagIndex+3);	//gives the rest of the closetag, e.g. '>'
					//regexNEString += NEEndTag1 + "\\1" + NEEndTag2;
					//System.err.println("NEEndTag1: "+NEEndTag1+" NEEndTag2: "+NEEndTag2);
					//useStartTag = true;
					
					//the endtag cannot be a regex, because we're using 'indexOf' 
					//NEEndTag = "</[^>]+";
					groupedNEEndTag = "(</[^>]+>)";
					NEEndTag = "</";
					//System.err.println("groupedNEEndTag: "+groupedNEEndTag);
					regexNEString += "</[^>]+>";
					regexNEString += "\\s*$";
					
				}
			}
		}
		//System.err.println("regexNEString: "+regexNEString);
		regexNE = Pattern.compile(regexNEString);
	}
	
	
	public static String txt2bio(String filecontent, String netag1, String netag2, boolean preserveEOL,
			boolean addTags) {
		
		/*	This method is based on the impact perlscripts 'prepTrainFile.pl' and 'tag2biotag.pl'
		 * 
		 */
		
		NEOpenTag = netag1;
		NECloseTag = netag2;
		
		createTagPattern();
		
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		
		int indexCounter = 0;
		int totalStringlength = 0;
		ArrayList<Integer> EOLlist = new ArrayList<Integer>(); 
		
		int flag = 0;
		boolean isNE = false;
		
		String[] lines = filecontent.split("\\n");
		for(String line : lines){
			String oriLine = line;
			//System.err.println("OUD: "+line);
			line = line.replaceAll("\\r\\n", "\n");
			//	s/[\n\r]+//gs;
			line = line.replaceAll("[\\n\\r]+", "");
			   //	s/^\s+$//g;
			line = line.replaceAll("^\\s+$", "");
			//	s/(<NE)/\n$1/g;
			line = line.replaceAll(groupedNEStartTag, "\n$1");
			//System.err.println("OUD2: "+line);
			//	s/(<\/NE>)/$1\n/g;
			line = line.replaceAll(groupedNEEndTag, "$1\n");
			//System.err.println("OUD3: "+line);
			//System.err.println("NEW: "+line);
			String[] tokens = line.split("\n");
			
			//System.err.println("NEStartTag: '"+NEStartTag+"' NEEndTag '"+NEEndTag+"'");
			
			for(int i = 0; i < tokens.length; i++){
				//System.err.println("TOKEN: >>"+tokens[i]+"<<\n");
				//sb.append("(((string>>"+tokens[i]+"<< flag: "+flag+")))");
				String spaceOrNewline = "\n";
				//int flag = 0;
				if(flag > 0){
					spaceOrNewline = " ";
				}
				
				if( (tokens[i].indexOf(NEStartTag) > -1) && (tokens[i].indexOf(NEEndTag) > -1) ){
					//open and close tag, reset isNE
					//sb.append("1 "+flag);
					sb.append(tokens[i] + "\n");
					isNE = false;
				}
				else if( (tokens[i].indexOf(NEStartTag) > -1) && (tokens[i].indexOf(NEEndTag) == -1) ){
					//open tag, set NE
					flag++;
					//sb.append("2 " + flag);
					sb.append(tokens[i] + " ");
					isNE = true;
				}
				else if( (tokens[i].indexOf(NEEndTag) > -1) && (tokens[i].indexOf(NEStartTag) == -1) ){
					//close tag, reset NE
					flag--;
					//sb.append("3 "+flag);
					sb.append(tokens[i] + "\n");
					isNE = false;
				}
				else{
					//	print join($spaceOrNewline, split(/\s/, $S)) . $spaceOrNewline;
					String[] words = tokens[i].split("\\s");
					//sb.append("4 " +flag+" (spaceornewline="+spaceOrNewline+") ");
					String newline = "";
					for(int j = 0; j < words.length; j++){
						newline += words[j] + spaceOrNewline;
					}
					sb.append(newline);
				}
			}
			totalStringlength += line.length();
			//the index of the end of the line is stored in the EOL-index
			if(preserveEOL){
				EOLlist.add(totalStringlength);
				if(isNE){sb.append(EOL+" ");}
				else{sb.append(EOL+"\n");}
			}
		}
		
		//System.err.println("sb: " + sb);
	
		String[] parts = sb.toString().split("\n");
		for(int i = 0; i < parts.length; i++){
			if(!parts[i].equals("")){
				//System.err.println("PART: "+parts[i]);
				//if(/<NE_([^>]+)>(.*)<\/NE>\s*$/)
				parts[i] = parts[i].replaceAll("\\s+", " ");

				Matcher ma = regexNE.matcher(parts[i]);
				if(ma.find()){
		    		//System.err.println("HUH?");
					//my(@Wrds);
		    	    //my($thisNE,$thisNETag);
		    		 //$thisNE=$2;
					String thisNE = ma.group(3);
					//$thisNETag=$1;
					String thisNETag = ma.group(1);
					//System.err.println("ThisNETAG: "+thisNETag);
					//$thisNETag =~ s/ .*$//;
					thisNETag = thisNETag.replaceAll(" .*$", "");
					//$thisNETag =~ s/^PERS$/PER/;
					thisNETag = thisNETag.replaceAll("^PERS$", "PER");
					//@Wrds=split(" ", $thisNE);
					String[] newords = thisNE.split(" ");
					for(int j = 0; j < newords.length; j++){
						//$tagPrefix="B-";
						String tagPrefix = "B-";
						if(j>0){
							tagPrefix = "I-";
						}
						//print join(" ", $Wrds[$i], "POS", $tagPrefix . $thisNETag);
						if(newords[j].indexOf(EOL) > -1){
							//System.err.println("EOL? "+newords[j]);
							sb2.append(newords[j]+"\n");
						}
						else{
							sb2.append(newords[j]+" POS " + tagPrefix +thisNETag+"\n");
						}
					}
				}
				else{
					//print join(" ", $_, "POS", "O");
					if(parts[i].indexOf(EOL) > -1){
						sb2.append(parts[i]+"\n");
						//System.err.println("EOL2? "+parts[i]);
					}
					else{
						sb2.append(parts[i] + " POS O\n");
					}
					//from script 'witregeltoevoegen.pl'
					//if($_ =~ /^([\.|?|!])[\s*]POS[\s*]O/ ){
					if(parts[i].equals(".") || parts[i].equals("?") || parts[i].equals("!") ){
						sb2.append("\n");
					}
		    	}
		    }
		}
		//System.err.println("sb2: " + sb2);
		return sb2.toString();
	}
	
	public static String bio2txt(String sentence, ArrayList<Integer> eol){
		//System.out.println("ZIN: >>"+sentence+"<<");
		String newSentence = "";
		String[] words = sentence.split("\\n");
		String prevTagRef = "O";
		String prevTagRes = "O";
		//System.err.println("eol: "+eol);
		if(NEStartTag.equals("<[^>/]+")){
			NEStartTag = "<";
		}
		for(int i = 0; i < words.length; i++){
			String filler = "";
			if(eol.contains(i)){
				filler = "\n";
			}
			else if(i > 0 && i < words.length){
				filler = " ";
			}
			String[] parts = words[i].split(" ");
			//System.out.println("word: "+parts[0]+" reftag: "+parts[1]+" tooltag: "+parts[2]);
			
			if(parts[2].equals("O")){
				//tool response tag = O
				if(!prevTagRes.equals("O")){
					if(parts[1].indexOf("I-") == -1){newSentence += refEndTag;}
				}
				if(!prevTagRef.equals("O")){
					newSentence += NEEndTag;
					if(NEEndTag.equals("</")){
						newSentence += prevTagRef.substring(2);
					}
					newSentence += ">";
				}
				if(parts[1].indexOf("B-") > -1){
					int ind = parts[1].indexOf("B-");
					String tag = parts[1].substring(ind+2);
					newSentence += filler + refStartTag+tag+">";
					newSentence += parts[0];
				}
				else{
					newSentence += filler + parts[0];
				}
			}
			else{ 
				if(parts[2].indexOf("B-") > -1){
					int ind = parts[2].indexOf("B-");
					String tag = parts[2].substring(ind+2);
					newSentence += filler + NEStartTag+tag+NEStartTag2;
					if(parts[1].indexOf("B-") > -1){
						ind = parts[1].indexOf("B-");
						tag = parts[1].substring(ind+2);
						newSentence += refStartTag+tag+">";
					}
					newSentence += parts[0];
				}
				else{
					//I-tag
					newSentence += filler + parts[0];
				}
			}
			
			prevTagRes = parts[1];
			prevTagRef = parts[2];
		}
		//add close tags at end of chunk if needed
		if(!prevTagRes.equals("O")){
			//System.out.println("case1");
			newSentence += refEndTag;
		}
		if(!prevTagRef.equals("O")){
			//System.out.println("case2");
			newSentence += NEEndTag;
		}
		return newSentence;
	}
	
}
