#!/usr/bin/perl

use strict;
use warnings;

##	fixInitials.pl
##	by Frank Landsbergen, INL, for IMPACT
## 
## 	use: perl fixInitials.pl < FILE
##	FILE: BIO-file <WORD><TAG>
##
## 	This script locates periods on separate lines, whereas they should be attached to
##	the initial on the previous line, and fixes this: G . Jansen > G. Jansen.
##	Also, double initials are identified, e.g. 'G.J . Jansen' > 'G.J. Jansen'
##	
##	The array @sKeyWords can be altered, holding all other words such as titles, from which
##	the period can have ended up at the next line.


my @sKeywords = ("mr", "Mr", "dr", "Dr", "st", "St", "ir", "Ir", "jr", "Jr", "wed", "Wed");

my $sPrevWord;
my $sPrevRestOfLine;
my $bHoldPrint = -1;

sub readTargetfile(){
	while (<STDIN>)
	{		
		chomp;
		if($_ =~ /^./){
			my ($w, $ref, $res) = split(/\s+/);	
			## 	Locate initial, e.g. 'G', or other keywords
			my $bF = checkWord($w);
			if($bF == 1){
			#if($w =~ /^[A-Z]{1}$/){
				$sPrevWord = $w;
				$bHoldPrint = 1;
				#print "Found word " . $sPrevWord . "\n";
			}
			if($bHoldPrint == 1){
				$sPrevRestOfLine = " " . $ref . " " . $res . "\n";
			}
			else{
				print $w . " " . $ref . " " . $res . "\n";
			}		
			##	Locate 'false' period
			if( ($w =~ /^\./) && ($bHoldPrint == 1) ){
				print $sPrevWord . "." . $sPrevRestOfLine;
				$sPrevWord = "";
				$sPrevRestOfLine = "";
				$bHoldPrint = -1;
			}
		}	
		else{
			print $_ . "\n";
		}
	}
}

sub checkWord(){
	my ($a, $b, $c) = split(/\s+/); 
	##	Check if this word is an initial or a keyword
	my $bFound = -1;
	## 	Single initials like 'A' or 'P'
	if($a =~ /^[A-Z]{1}$/){
		$bFound = 1;
	}
	## 	Double initials like 'J.R' or 'P.W'
	else{
		if($a =~ /^[A-Z]{1}\.[A-Z]{1}/){
			$bFound = 1;
		}
		else{
			foreach(@sKeywords){
				if($a eq $_){
					$bFound = 1;
				}
			}	
		}
	}
	return $bFound;
}


#subs
readTargetfile();
