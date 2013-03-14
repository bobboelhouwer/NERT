#!/usr/bin/perl

use strict;
use warnings;

##	splitFiles_BIO.pl
##	use: splitFiles_BIO.pl [sourcefile] [nr. of words for file1] [nr. of iterations]
##	
##	input: a sourcefile in BIO-format. 
##
##	This script splits one file into two: file1, file2. File1 consists of the desired number of words. It is constructed
## 	by randomly selecting sentences throughout the source file. The remainer of the sentence go to file2.
##	
##	For testing, multiple files can be created by setting the third argument. E.g. '3' means 3 randomly generated
##	file1 and file2's are created.

($#ARGV != 3) or die "Usage: $0 [sourcefile] [nr. of words for file1] [nr. of iterations]\n";

# variables
my ($inputfile, $iMaxWords, $iNumFiles) = @ARGV;
my @aRegel;
my $iWordCount = 0;
my $sFile1Out = "";
my $sFile2Out = "";

my $iSentenceIndex = 0;					# total number of sentences
my @iRdmList1;
my @iRdmList2;

my @iOnset;
my @iOffset;

my $iTotalWordsList1 = 0;
my $iTeration = 0;


sub iterate{
	for(1..$iNumFiles){
		#reset all relevant variables
		$sFile1Out = "";
		$sFile2Out = "";
		$iSentenceIndex = 0;
		$iTotalWordsList1 = 0;
		undef(@aRegel);
		undef(@iRdmList1);
		undef(@iRdmList2);
		$iTeration++;
		readFile();
	}
}


##	read source file
sub readFile{
	open(FILE, $inputfile) or die "Could not read from $inputfile, program exiting.";	
	$iOnset[$iSentenceIndex] = 0;	
	my $iCounter = 0;
	while (<FILE>)
	{		
		chomp;
		push(@aRegel, $_);

		##	empty line marks end of sentence
		if($_ eq ""){
			$iOffset[$iSentenceIndex] = $iCounter;
			$iSentenceIndex++;
			$iOnset[$iSentenceIndex] = $iOffset[$iSentenceIndex-1];			
		}
	$iCounter++;	
	}
	close(FILE);
	makeRandomLists();
	createOutputFiles();
	printFiles();
}


##	Create two lists of random numbers that do not overlap
sub makeRandomLists(){

	print "Total num of sentences: " . $iSentenceIndex . "\n";

	##	List 1
	my $iCounter = 0;
	while( ($iCounter < $iSentenceIndex) && ($iTotalWordsList1 <= $iMaxWords) ){
		my $randomNr = int(rand($iSentenceIndex));
		my $bUniqueNr = -1;									
		my $iCounting = 0;
		while( ($bUniqueNr == -1) && ($iCounting < $#iRdmList1) ){
			if($iRdmList1[$iCounting] == $randomNr){$bUniqueNr = 1;}
			$iCounting++;
		}
		##	 if new number, add to list
		if($bUniqueNr == -1){
			push(@iRdmList1, $randomNr); 
			$iTotalWordsList1 += (($iOffset[$randomNr]-1) - $iOnset[$randomNr]);
		}		
		$iCounter++;
	}

	##	List 2
	for(0..($iSentenceIndex-1)){
		## 	make sure number is not in list 1 already, if so: skip
		my $p = $_;
		my $bUniqueNr = -1;									
		my $iCounting = 0;
		while( ($bUniqueNr == -1) && ($iCounting < $#iRdmList1) ){
			if($iRdmList1[$iCounting] == $p){$bUniqueNr = 1;}
			$iCounting++;
		}
		if($bUniqueNr == -1){
			push(@iRdmList2, $p); 
		}		
	}
}


sub createOutputFiles(){
	foreach(@iRdmList1){
		my $t = $_;
		my $length = $iOffset[$t] - $iOnset[$t];
		for(($iOnset[$t]+1)..$iOffset[$t]){
			$sFile1Out .= $aRegel[$_] . "\n";
		}
	}
	foreach(@iRdmList2){
		my $t = $_;
		my $length = $iOffset[$t] - $iOnset[$t];
		for(($iOnset[$t]+1)..$iOffset[$t]){
				$sFile2Out .= $aRegel[$_] . "\n";
		}
	}

}


sub printFiles{
	
	## 	write file1
	my $append = 0;
	if ($append){
 		open(MYOUTFILE, "> file1_".$iTeration.".txt"); #open for write, overwrite
 	}
	else{
	 open(MYOUTFILE, ">> file1_".$iTeration.".txt"); #open for write, append
 	}
	print MYOUTFILE "$sFile1Out";
	close(MYOUTFILE);

	##	write file2
	$append = 0;
	if ($append){
 		open(MYOUTFILE, "> file2_".$iTeration.".txt"); #open for write, overwrite
 	}
	else{
	 open(MYOUTFILE, ">> file2_".$iTeration.".txt"); #open for write, append
 	}
	print MYOUTFILE "$sFile2Out";
	close(MYOUTFILE);
}


# subs
&iterate;
#&readFile;
#&makeRandomLists;
#&createOutputFiles;
#&printFile2;
