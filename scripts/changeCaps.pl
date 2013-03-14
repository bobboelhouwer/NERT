#!/usr/bin/perl

use strict;
use warnings;

##	changeCaps.pl
##	by Frank Landsbergen, INL, for IMPACT
## 
## 	use: perl changeCaps.pl FILE1 FILE2
##	FILE1: BIO-file <WORD><TAG>
##	FILE2: txt-file <WORD>
##
## 	This script changes the lowercase initials of all words in file1 to uppercase if they match a word in file2.
##	It is meant to make it easier for the Stanford NE-recognizer to identify person names with lowercase
##	letters, such as 'de Heer van den Broek'. In order to manage long names like 'Baron van der Voorst tot Voorst', 
##	a maximum distance between the identifier (e.g. 'Heer') and the end of the name is set. Not ideal.


($#ARGV != 2) or die "Usage: $0 <TARGETFILE> <TEXTFILE>\n";

# variabels
my ($targetfile, $listfile) = @ARGV;
my @sWords;
my @sSentence;
my $bFoundIdentifier = 0;
my $iLineCount = 0;
my $iMaxDistance = 5;		#max distance between identifier ('Heer') and the end of name
my $iOnset;



# open listfile
sub readListfile{
	open(FILE, $listfile) or die "Could not read from $listfile, program exiting.";	
	while (<FILE>)
	{		
		chomp;
		push(@sWords, $_);
	}
	close(FILE);
}

# open targetfile
sub readTargetfile{
	open(FILE, $targetfile) or die "Could not read from $targetfile, program exiting.";	
	while (<FILE>)
	{		
		chomp;
		if($_ =~ /^\w+/){
			my ($w, $ref, $res) = split(/\s+/);	
			##	Check for words from listfile. If found: mark line number
			foreach(@sWords){
				if($w eq $_){
					$w = ucfirst($w);
					if($bFoundIdentifier == 0){$iOnset = $iLineCount;}
					$bFoundIdentifier = 1;
				}
			}
			push(@sSentence, $w . " " . $ref . " " . $res . "\n");
			## 	If mark is on and the maximum number of lines has been 
			## 	reached: check this part of the sentence for words
			## 	that should be converted to uppercase;
			if( ($bFoundIdentifier == 1) && ($iLineCount - $iOnset == $iMaxDistance) ){
				toUppercase();
				$bFoundIdentifier = 0;
				$iOnset = -1;				
			}
			$iLineCount++;
		}
		else{
			if($bFoundIdentifier == 1){
				## 	The end of the sentence has been reached before the maximum number of lines.				
				##	Check this part of the sentence.
				toUppercase();
				$bFoundIdentifier = 0;
				$iOnset = -1;	
			}
			##	Print Sentence
			foreach(@sSentence){
				print $_;
			}			
			print $_ . "\n";
			splice(@sSentence);
			$iLineCount = 0;			
		}
	}
	close(FILE);
}


sub toUppercase(){
	my $iOffset = $iOnset + $iMaxDistance;
	my $iNewOffset = $iOffset;	
	if($#sSentence < $iOffset){$iOffset = $#sSentence;}

	## Locate last uppercase word within margins
	for($iOnset..$iOffset){
		if( $sSentence[$_] =~ /^[A-Z]/ ){
			$iNewOffset = $_; 
		}
	}
	$iOffset = $iNewOffset;

	## 	Change all lowercase initials of words within the margins to uppercase
	for($iOnset..$iOffset){
		$sSentence[$_] = ucfirst($sSentence[$_]);
	}
}


## subs
&readListfile;
&readTargetfile;
