#!/bin/bash

##	run_nert.sh 
##	by Frank Landsbergen, INL 2010
##
##	Train and test NERT with example files. Run from main dir 

## 	Train
java -jar tool/nert.jar -t -props data/props/sample_trainerprops.props

##	Test
java -jar tool/nert.jar -e -loadClassifier tool/models/model_dbnl.ser.gz -testFile data/sample_extract/sample_extract_dbnl.txt -sv -svphontrans data/phontrans/phonTrans.txt -in txt -out txt -nelist out/nelist.txt -svlist out/svlist.txt > out/dbnl_extracted.txt
