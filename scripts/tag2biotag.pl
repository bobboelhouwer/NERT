#!/usr/bin/perl

$\="\n";

while(<STDIN>)
{
    chomp;
    if(/^\s*$/){}
    else
    {
	$\="\n";
	if(/^<NE_([^>]+)>(.*)<\/NE>\s*$/)
	{
	    my(@Wrds);
	    my($thisNE,$thisNETag);
	    $thisNE=$2;
	    $thisNETag=$1;
	    $thisNETag =~ s/ .*$//;
	    $thisNETag =~ s/^PERS$/PER/;
	    @Wrds=split(" ", $thisNE);
	    for $i (0..$#Wrds)
	    {
		$tagPrefix="B-";
		if($i>0)
		{
		    $tagPrefix = "I-";
		};
		print join(" ", $Wrds[$i], "POS", $tagPrefix . $thisNETag);
	    };
	}
	else
	{
	    print join(" ", $_, "POS", "O");
			## print empty line at end of each sentence
			if(/^\./){print " ";}
	}
    };
};
