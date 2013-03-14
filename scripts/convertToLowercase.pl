#!/usr/bin/perl

use strict;
use warnings;

##	converToLowercase.pl
##	by Frank Landsbergen, INL, for IMPACT
## 
## 	use: perl converToLowercase.pl < FILE
##	FILE: BIO-file <WORD><TAG>
##
## 	This script locates words written in uppercase (e.g. LEOPOLD) and changes them to lowercase with 
##	an uppercase initial.


while (<STDIN>)
{		
	chomp;
	if($_ =~ /^\w+/){
		my ($w, $ref, $res) = split(/\s+/);	
		if($w =~ /^[A-Z]+$/){
			$w =~ tr/A-Z/a-z/;
			$w = ucfirst($w);			
		}	
		print $w . " " . $ref . " " . $res . "\n";
	}	
	else{
		print $_ . "\n";
	}
}
