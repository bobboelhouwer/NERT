#!/bin/bash

##	find_NE_identifiers.sh 
##	by Frank Landsbergen, INL 2010
##
##	This script reads a file with tags in BIO-format and lists all words 
##	that precede words with the tags B-LOC, B-ORG or B-PER to standard output.
##		This list can be used to create a list of spelvar rules for the Named
##	Entity Recognition Tool.
##
##	Use: sh find_NE_identifiers.sh FILENAME


grep -B1 B-LOC < $1 | grep -v B-LOC | grep -v "^$" | grep -v - | cut -d " " -f1 | sort | uniq -c | sort | perl -pe 's/^\s+//' | awk '{print $2, $1}' | sort > tempLOC.txt

grep -B1 B-ORG < $1 | grep -v B-ORG | grep -v "^$" | grep -v - | cut -d " " -f1 | sort | uniq -c | sort | perl -pe 's/^\s+//' | awk '{print $2, $1}' | sort > tempORG.txt

grep -B1 B-PER < $1 | grep -v B-PER | grep -v "^$" | grep -v - | cut -d " " -f1 | sort | uniq -c | sort | perl -pe 's/^\s+//' | awk '{print $2, $1}' | sort > tempPER.txt

#BASEN=`echo $1 | sed 's/\.[^.]*$//'`
#BASENM=`basename $BASEN`
cat tempLOC.txt tempORG.txt tempPER.txt | cut -d " " -f1 | sort | uniq
rm tempLOC.txt
rm tempORG.txt
rm tempPER.txt
