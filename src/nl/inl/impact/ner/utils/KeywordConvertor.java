package nl.inl.impact.ner.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeywordConvertor {
	
	public static String getKeyword(String s, String type){
		
		//this method also calls 'normalizeString()'
		
		String keyword = "";
		/*
		if(s.equals("MARTEN HARPERTSZOON TROMP")){
			System.err.println(s);
		}
		*/
		
		
		if(type.equals("LOC")){
			
			/*	LOC is a bit trickier. For now, we trace the following and leave them 
			 * 	out of the keyword:
			 * 
			 * 	St, St., Sint, Saint, San, Santa, Santo
			 * 	Noord, Oost, Zuid, West
			 * 	Nieuw, Oud
			 * 	Republiek, Rijk, Ryk
			 * 	Groot
			 * 	Kaap
			 * 	eilanden
			 * 	
			 * 	NB these words must be followed by a whitespace, dash, or both,
			 * 	e.g. 'Noordpool' is not considered. 
			 * 
			 */
			
			
			
		}
		
		
		if(type.equals("PER")){
			//normalize string a bit, so we can properly deal
			//with strings like VESPA -     SIANUS, which is really
			//a single word.
			//Leave the dash, but take out the whitespaces around it
			s.replaceAll("(\\s+)-(\\s+)", "-");
			
			Pattern pa = Pattern.compile("(\\s+)(-)(\\s+)");
			Matcher m = pa.matcher(s);
			s = m.replaceAll("-");
			
			
			int lastWhitespaceIndex = s.lastIndexOf(" ");
			//NE consists of only one word: keyword = word
			if(lastWhitespaceIndex < 0){
				keyword = NERTStringUtils.normalizeString(s);
				/*
				if(s.equals("MARTEN HARPERTSZOON TROMP")){
					System.err.println("SINGLE NAME "+s+" "+keyword);
				}
				*/
			}
			else{
				//NE consists of more than one word: 
				//default: keyword = last normalized word of NE
				//e.g. Jan Jansen > keyword = jansen
				//unless it fits specific patterns
				
				//TODO: make this a single pattern instead of two
				
				//System.err.println("EN? "+s+" "+keyword);
				//pattern 1: Dirk de III, Jan den IVe 
				Pattern p = Pattern.compile("(.+)\\s{1}(de|den|DE|DEN)\\s+M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})e?$");
				Matcher matcher = p.matcher(s); 
				if(matcher.find()){
					//group(0) is the entire match
					keyword = NERTStringUtils.normalizeString(matcher.group(1));
				}
				else{
					//pattern 2: Willem V
					p = Pattern.compile("(.+)\\s+M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})e?$");
					matcher = p.matcher(s); 
					if(matcher.find()){
						//group(0) is the entire match
						keyword = NERTStringUtils.normalizeString(matcher.group(1));
					}
					else{
						keyword = NERTStringUtils.normalizeString(s.substring(lastWhitespaceIndex+1, s.length()));
						/*
						if(s.equals("MARTEN HARPERTSZOON TROMP")){
							System.err.println("MULTIPLE NAMES "+s+" "+keyword);
						}
						*/
					}
				}
				//System.err.println("EN? "+s+" "+keyword);

			}
			
		}
		
		
		return keyword;
	}
}

