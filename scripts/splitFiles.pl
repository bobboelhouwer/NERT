#!/usr/bin/perl

use strict;
use warnings;

##	splitFiles.pl
##	use: splitFiles.pl [sourcefile] [nr. of words for file1] [nr. of iterations]
##	
##	input: a sourcefile with a sentence on each line. 
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

my $iSentenceIndex = 0;					# number of lines
my @iRdmList1;
my @iRdmList2;

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


## 	read file
sub readFile{
	open(FILE, $inputfile) or die "Could not read from $inputfile, program exiting.";	
	while (<FILE>)
	{		
		chomp;
		##	 a sentence on each line
		push(@aRegel, $_);
			$iSentenceIndex++;
	}
	close(FILE);
	makeRandomLists();
	createOutputFiles();
	printFiles();
}

##	Create two lists of random numbers that do not overlap
sub makeRandomLists(){

	## 	List 1
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
			## total number of words for this sentence eq the number of whitespaces - 1 
			my $count = () = $aRegel[$randomNr] =~ /\s+/g;
			$iTotalWordsList1 += ($count-1);
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
		$sFile1Out .= $aRegel[$_] . "\n";		
	}
	foreach(@iRdmList2){
			$sFile2Out .= $aRegel[$_] . "\n";
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
#&printFiles;
