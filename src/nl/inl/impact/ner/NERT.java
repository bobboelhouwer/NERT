package nl.inl.impact.ner;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import nl.inl.impact.ner.matcher.NEMatcher;
import nl.inl.impact.ner.stanfordplus.ImpactCRFClassifier;

/*	General main class of NERT for command line use.
 * 
 * 	Frank Landsbergen, INL, 2010
 * 	version 0.1
 * 	
 * 	(for internal use only)
 * 	use without JAR, from directory /bin:
 * 	java -cp /mnt/Projecten/Impact/NER/NER_tools/Stanford_09/stanford-ner-2009-01-16/classes:.:/home/INL/landsbergen/Eclipse/eclipse_workspace/NERT/bin nl.inl.impact.ner.NERT
 * 
 * 	trainer: 	[..] -t -props PROPSFILE (with trainFile, trainFiles or trainDirs)
 * 				trainFiles=A;B;C			# files should be separated by a semi-colon (;)
 * 				useSpelVar=true/false		# default=false
 * 				svPhonTrans=PATH 			# path to phonetic transcription rules for the spelvarmodule
 * 				printspelvarpairs=FILENAME	# path + filename to write created spelvarpairs to	
 * 				minwordlength=				# for spelvarmodule. default=2
 * 				format=bio/txt/xml			# input format of the training file. default=bio
 * 				xmltags=TAG1;TAG2;TAG3		# those xmltags that should be considered. All the rest is discarded.
 * 											# multiple tags should be separated with a semicolon (;)
 * 											# tags should not have '<' and '>' around them
 * 											# trainer can deal with attributes, which do not have to be mentioned, e.g.
 * 											# 'xmltag=unicode' will find text between <unicode> and <unicode attr1="blah">
 * 											# for closing tags, it will assume </unicode> 
 * 
 * 				NB in order for the spelvarmodule to be able to find the words from the trainingfile when TESTING, 
 * 				'useWord=true' is necessary during training
 * 
 * 	extractor: 	[..] -e -loadClassifier model.ser.gz -testFile FILE 		# regular extracting, no spelvar
 * 				[..] -e -sv -loadClassifier model.ser.gz -testFile FILE 	# extracting with spelvar, and default settings
 * 				[..] -e -sv -in bio -out txt -loadCl.... 					# input, output format. default: bio
 * 																			# options: bio, xml, txt
 * 																			# if no output format is specified,
 * 																			# the inputformat is used as outputformat
 * 				[..] -e -in xml -tags Unicode;Text -out xml					# flag 'tag' is similar to 'tags'.
 * 																			# for use of tags, see information with option -t above
 * 				[..] -e -sv -svlist FILE									# print created spelvar pairs to FILE
 * 				[..] -e -NElist FILE										# print extracted NE's to FILE
 * 				[..] -e -svphontrans FILE									# file with phonetic transcription rules for the spelvar module
 * 				
 * 				NB all parameters from the spelvarmodule can be altered by pass them with the correct flag in the command line
 * 				NB the 'regular' flags loadClassifier, testFile have to come at the end
 * 
 *
 * 	matcher: 	[..] -m -props PROPERTIESFILE				# '-prop' or '-properties' also works 
 * 
 * 				The properties file can contain the following arguments:
 * 
 * 				verbose=true/false			# prints extra information to STDERR
 * 				pairview=true/false			# determines output format. default: pairview=true
 * 				groupview=true/false		# NB: in case of conflict, pairview=true
 * 				showScores=true/false		# adds the score after each variant (e.g. Leiden	Leyden (80) 
 * 				minScore=90					# sets the minimal score that is printed. default=50
 * 				punishDiffInitial			# punishment for normalized(!) words that start with a different character.
 * 											# default: punishDiffInitial=10
 * 				hasType=true/false			# tells the matcher that the input format contains types, e.g. LOC Leiden
 * 				useType=true/false			# false: match regardless of type; true: only match with matching type
 * 											# default: useType=false
 * 				printTypes=true/false		# print types of each word and its variants (e.g. LOC Leiden	LOC	Leyden) 
 *				perFilter=true/false		# apply perfilter to the type PER (see documentation). 
 *											# NB only works with exact type PER!
 *				nBest=10					# print n best matches, with minScore as minimum value
 *											# default nBest=-1, which disregards this parameter
 *											# NB if minScore=90 and nBest=10, and we have 10 variants but only 1 with a score > minScore,
 *											# it will only print this one variant. So: minScore determines the actual output, even with nBest.
 *											# If you would want to discard of minScore, do minScore=0
 *
 *			File input:
 *
 *				file=FILE					# file used for matching. All NEs in this file are compared with each other. 
 *											# format: one NE per line: 'Leiden'. If a type is added, make sure to set hasType=true
 *				files=FILE1;FILE2;FILE3		# files used for matching All NEs in these files are compared with each other.
 *				dir=DIR						# all the NEs from all files within this directory are compared with each other
 *				lemmaFile=FILE				# all NEs in file/files/dir are matched to the NEs in file lemmaFile
 *				lemmaDir=DIR				# all NEs in file/files/dir are matched to the NEs in the files in this dir
 *
 * 				phonTrans=FILE				# file with phonetic transcription rules. All NEs are converted to a normalized string
 * 											# according to these rules, and then compared.
 * 				surnameIdentifiers=FILE		# can be used with perFilter. list of words that indicate a following surname, such as 
 * 											# Dutch 'van' or 'de'. (print in lowercase)
 * 				string2roman=FILE			# list of rewrite rules if the matcher comes across roman numbers spelled out,
 * 											# e.g. Johannes de Derde. The rule 'derde=>iii' transforms this to 'Johannes de iii'
 * 											# thus being able to compare it to other strings of this format. 
 * 											
 * 											# NB for the latter 2, no defaults exist, thus enabling the user to include null
 * 											# information.
 * 												
 */	

public class NERT {
	
	NERT(String[] args) throws IOException{
		
		/*	args[0] signifies the type of NE-work the user
		 * 	wants to do. If this argument is not given
		 * 	properly, NERT stops here.
		 */
				
		System.err.println("Reading first argument ...");
		
		if(args[0].length() > 0 && args[0].charAt(0) == '-'){
			//	strip the hyphen
			args[0] = args[0].substring(1);	
			if(args[0].equals("m")){
				//	user calling the matcher
				System.err.println("NERT module: matcher");
				initMatcher(args);
			}
			else if(args[0].equals("e")){
				//	user calling the extractor
				System.err.println("NERT module: extractor");
				initExtractor(args);
			}
			else if(args[0].equals("t")){
				//	user calling the trainer
				System.err.println("NERT module: trainer");
				initTrainer(args);
			}
			else{
				System.err.println("Quitting NERT because of incorrect first argument.\nPlease use -t(rainer), -e(extractor) or -m(atcher) as initial flag.");
			}
		}
	}
	
	public void initExtractor(String[] args){
		
		/*	Init the extractor.
		 * 	After training, Stanford stores all the main variables in the serialized model.
		 * 	The spelvar variables are not in there, since Stanford does not recognize them.
		 * 	We therefore pass on the spelvar variables through command line arguments. 
		 */
		
		boolean useSpelVar = false;
		String svPhonTrans = "";
		String printSVList = "";
		String printNEList = "";
		String inputFormat = "";
		String outputFormat = "";
		String xmltag = "";
		String netag1 = "";			//the shape of the opening ne-tag, e.g. <NE_PER> or <NE type="PER">
		String netag2 = "";			//the shape of the closing ne-tag, e.g. </NE> or </LOC>
		//	find arg "-loadClassifier": all args in between are spelvar flags
	
		HashMap<String, String> svPropsHM = new HashMap<String, String>();
		List<String> stanfordArgs = new LinkedList<String>();
		//testje
		/*
		String a = "[ Args: ";
		for(String s : args){
			a += " / " + s;
		}
		a += " ]";
		System.err.println(a);
		*/
		//System.err.println("ARG: ");
		
		//for(int i = 1; i < stopAt; i++){
		for(int i = 1; i < args.length; i++){
			//System.err.println("["+i+"] >" + args[i]);
			//if(args[i].substring(0,1).equals("-")){
			//if(args[i].charAt(0) == '-'){
				if(args[i].equalsIgnoreCase("-loadClassifier")){
					stanfordArgs.add(args[i]);
					stanfordArgs.add(args[i+1]);
				}
				else if(args[i].equalsIgnoreCase("-testfile")){
					stanfordArgs.add(args[i]);
					stanfordArgs.add(args[i+1]);
				}
				else if(args[i].equalsIgnoreCase("-testfiles")){
					stanfordArgs.add(args[i]);
					stanfordArgs.add(args[i+1]);
				}
				else if(args[i].equalsIgnoreCase("-testdirs")){
					System.err.println("Using directory "+args[i]);
					stanfordArgs.add(args[i]);
					stanfordArgs.add(args[i+1]);
				}
				else if(args[i].equalsIgnoreCase("-sv")){
					useSpelVar = true;
					System.err.println("do spelvarreduction="+useSpelVar);
				}
				else if(args[i].equalsIgnoreCase("-svphontrans") || args[i].equalsIgnoreCase("-svPhonTrans")){
					svPhonTrans = args[i+1];
					svPropsHM.put("svPhonTrans", svPhonTrans);
					System.err.println("phontransfile="+svPhonTrans);
				}
				else if(args[i].equalsIgnoreCase("-svlist")){
					if(args.length > i+1 && args[i+1].charAt(0) != '-'){
						printSVList = args[i+1];
					}
					System.err.println("print spelling variation pairlist to="+printSVList);
				}
				else if(args[i].equalsIgnoreCase("-NElist")){
					if(args.length > i+1 && args[i+1].charAt(0) != '-'){
						printNEList = args[i+1];
					}
					System.err.println("print list of extracted NEs to="+printNEList);
				}
				//	input format
				else if(args[i].indexOf("-in") > -1){
					if(args.length > i+1 && args[i+1].charAt(0) != '-'){
						inputFormat = args[i+1];
					}
				}
				//	output format
				else if(args[i].indexOf("-out") > -1){
					if(args.length > i+1 && args[i+1].charAt(0) != '-'){
						outputFormat = args[i+1];
					}
				}
				//	relevant tag in input xml; the rest is ignored
				else if(args[i].equalsIgnoreCase("-xmltag") || args[i].equalsIgnoreCase("-xmltags")){
					if(args.length > i+1 && args[i+1].charAt(0) != '-'){
						xmltag = args[i+1];
					}
				}
				// phontrans rules for the spelvarmodule
				//else if(args[i].equalsIgnoreCase("-svphontrans")){
				//	if(args.length > i+1 && args[i+1].charAt(0) != '-'){
				//		svPropsHM.put("svphontrans", args[i+1]);
				//		//svPhonTrans = args[i+1];
				//	}
				//}
				else if(args[i].equalsIgnoreCase("-starttag")){
					if(args.length > i+1 && args[i+1].charAt(0) != '-'){
						netag1 = args[i+1];
					}
				}
				else if(args[i].equalsIgnoreCase("-endtag")){
					if(args.length > i+1 && args[i+1].charAt(0) != '-'){
						netag2 = args[i+1];
					}
				}
				else{
					if(args[i].charAt(0) == '-'){
						System.err.println("Found argument: " + args[i]);
						String[] f = args[i].split("=");
						if(f[0].charAt(0) == '-'){f[0] = f[0].substring(1);}
						System.err.println("  "+f[0]+"="+f[1]);
						svPropsHM.put(f[0], f[1]);
					}
				}
			//}
		}
		//dit toegevoegd omdat soms (bv na het argument -sv) de string '-svphontrans' niet herkend
		//blijkt te worden - en het lijkt te liggen aan het streepje..
		for(int i = 0; i < args.length;i++){
			//System.err.println(i + ": >" + args[i]+"<");
			if(args[i].substring(1).equals("svphontrans") || args[i].substring(1).equals("-svPhonTrans") || args[i].substring(1).equals("-svphonTrans")){
				svPhonTrans = args[i+1];
				svPropsHM.put("svPhonTrans", svPhonTrans);
				System.err.println("phontransfile="+svPhonTrans);
			}
		}
		
		//test
		/*
		String k = "[ sv props: ";
		for(String s : svPropsHM.keySet()){
			k += " / " + s;
		}
		k += " ]";
		System.err.println(k);
		*/
		
		
		
		//if(stopAt == -1){
		//	stopAt = 2;
		//}
		
		//	Give a warning if inputformat is xml but no xmltag is given
		if(inputFormat.equalsIgnoreCase("xml") && (xmltag.equalsIgnoreCase("")) ){
			System.err.println("Warning: xml-inputformat but no relevant xml-tag given.");
		}
		
		//put (sorted) stanford arguments in an array 
		String extractorArgs[] = new String[stanfordArgs.size()];
		int c = 0;
		for(String ar : stanfordArgs){
			extractorArgs[c] = ar;
			c++;
		}
		
		/*	We start the CRFClassifier with all 
		 * 	arguments except the first flag.
		 */
		//String extractorArgs[] = new String[args.length-stopAt];
		//
		//for(int i = stopAt; i < args.length; i++){
		//	extractorArgs[i-stopAt] = args[i];
		//}
		ImpactCRFClassifier.initExtracting(extractorArgs, useSpelVar, svPropsHM, printSVList, printNEList, inputFormat, outputFormat, xmltag, netag1, netag2);
	}
	
	
	public void initTrainer(String[] args) throws IOException{
		
		/*	Starting training module. We use all arguments
		 * 	except the first one.
		 */
		
		String trainerArgs[] = new String[args.length-1];
		for(int i = 1; i < args.length; i++){
			trainerArgs[i-1] = args[i];
		}
		ImpactCRFClassifier.initTraining(trainerArgs);
	}
	
	
	public void initMatcher(String[] args){
		
		/*	Starting the matcher module.
		 * 
		 * 	Argument
		 * 
		 * 
		 * 	Argument 1 gives the output format: pairview (-p)
		 * 	or groupview (-g). Arguments 2-3 are the files
		 * 	with the NE's to be matched.
		 * 	The flag 'lemma' is used for those NE's that 
		 * 	should be taken as a sort of 'modern lemma' 
		 * 	to which the non-lemma's are compared. If no lemma
		 * 	file is specified, all NE's are compared to each other.
		 */
		
		String propertiesFile = "";
		
		for(int i = 1; i < args.length; i++){
			if(args[i].equalsIgnoreCase("-props") || args[i].equalsIgnoreCase("-prop") || args[i].equalsIgnoreCase("-properties")){
				propertiesFile =  args[i+1];
			}
		}
		
		
		NEMatcher nematcher = new NEMatcher();
		HashMap<String, String> arguments = nematcher.loadProperties(propertiesFile);
		nematcher.initMatcher(arguments);
	
	}
	
	public static void main(String[] args) throws IOException {
		System.err.println("\n### NERT ###\nNamed Entity Recognition and Matching Tool\nCreated by INL - using Stanford's NE-classifier");
		System.err.println("version 3.0, November 2011\n");
		NERT nert = new NERT(args);
	}

}
