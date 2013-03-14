#!/usr/bin/perl

use strict;
use warnings;

##	fixAbbrev.pl
##	by Frank Landsbergen, INL, for IMPACT
## 
## 	use: perl fixAbbrev.pl < FILE
##	FILE: BIO-file <WORD><TAG>
##
## 	This script locates the abbreviations 'v.' and 'd.' and changes them into 'van' and 'de'.


while(<STDIN>)
{		
	chomp;
	if($_ =~ /^\w+/){
		my ($w, $ref, $res) = split(/\s+/);	
		if($w =~ /^v\.$/){
			$w = "van";			
		}	
		if($w =~ /^d\.$/){
			$w = "de";			
		}
		print $w . " " . $ref . " " . $res . "\n";
	}	
	else{
		print $_ . "\n";
	}
}



