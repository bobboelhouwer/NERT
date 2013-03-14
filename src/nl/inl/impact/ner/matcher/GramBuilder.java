package nl.inl.impact.ner.matcher;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class GramBuilder {
	
	
	
	
	
	
	public static double compareGrams(String w1, String w2){
		
		/*	As in Jarvelin et al. paper.
		 * 	We use a set of 's-grams' to compare two strings.
		 *  s-grams are n-grams in which the n characters are 
		 *  not necessarily adjacent (s: 'skip').
		 *  s(a, b) => a: number of characters, b: number of skipped chars
		 *  e.g. Leiden with s(2,1): Li, ed, ie, dn
		 *  
		 *  We use s(2, 0), s(2, 1) and s(2, 2), so basically
		 *  digrams with different skips, this should make it robust
		 *  against spelling variations.
		 *  
		 *	This method returns both the average Jaccard similarity 
		 *	between the two strings and the distance between the strings.
		 */ 	
		
		//[0] gives distance, which is an int wrapped in a double
		//[1] gives jaccard sim.

		double[] r20 = calcSgramXY(w1, w2, 2, 0);
		double[] r21 = calcSgramXY(w1, w2, 2, 1);
		double[] r22 = calcSgramXY(w1, w2, 2, 2);
		
		//double distAv = (r20[0] + r21[0] + r22[0] + 0.0) / (3 + 0.0);
		double jacAv = (r20[1] + r21[1] + r22[1]) / (3 + 0.0);
		
		//System.out.println(w1+" "+w2+ " D="+distAv + " J="+jacAv);
	
		return jacAv;
	}
	
	public static double[] calcSgramXY(String w1, String w2, int n, int s){
		
		/* 	Compare words w and s with their s(kip)-grams.
		 * 	For distance calc: count double grams, 
		 * 	for Jaccard sim: no double grams.
		 * 	n: n in n-gram
		 * 	s: skip-factor
		 * 
		 * 	ONLY WORKS FOR N=2!
		 */
		w1 = "<" + w1 + ">";
		w2 = "<" + w2 + ">";
		
		//1 create gram of type s(2, 0) - digrams with skip=0,
		//	so similar to ngram of 2.
		
		//int l1 = w1.length() - (n-1) - s;
		//int l2 = w2.length() - (n-1) - s;
		
		
		//System.err.println("n="+n+" s="+s+" w1="+w1+" w2="+w2 + " l1="+l1+" l2="+l2);
		//if(l1<1 || l2 < 1){System.err.println(w1+" "+l1+" "+w2+" "+l2);}
		String[] nA = new String[w1.length() - (n-1) - s];
		String[] nB = new String[w2.length() - (n-1) - s];
		//System.err.println("n="+n+" s="+s+" w1="+w1+" w2="+w2);
		for (int i = 0; i < (w1.length() - (n-1)-s); i++) {
			nA[i] = w1.substring(i, i + 1) + w1.substring((s+i+1), (s+i+2));
			//System.err.println(i+" nA[]="+nA[i]);
		}
		
		for (int i = 0; i < (w2.length() - (n-1)-s); i++) {
			nB[i] = w2.substring(i, i + 1) + w2.substring((s+i+1), (s+i+2));
			//System.err.println(i+" nB[]="+nB[i]);
		}
		
		//2	collect all occurring unique grams from both strings
		SortedSet<String> gramSet = new TreeSet<String>();
		for (int i = 0; i < nA.length; i++) {
			//System.err.println("nA[]="+nA[i]);
			gramSet.add(nA[i]);
		}
		for (int i = 0; i < nB.length; i++) {
			if(!gramSet.contains(nB[i])){
				//System.err.println("nB[]="+nB[i]);
				gramSet.add(nB[i]);
			}
		}

		/*	3	make gram profile of both words.
		 * 	This means: iterating over de TreeSet 
		 * 	and keeping track of the presence of each
		 * 	gram from both words. If a gram occurs
		 * 	multiple times, count each occurrence.
		 * 
		 * 	e.g. for n=2, s=0, Leiden and Leyden give the treeset
		 * 	Le, ei, ey, id, yd, de, en
		 * 	Leiden gives 	{1, 1, 0, 1, 0, 1, 1}
		 * 	Leyden gives 	{1, 0, 1, 0, 1, 1, 1}
		 * 	dist gives		 0 +1 +1 +1 +1 +0 +0 = 4 
		 */
		
		Iterator<String> it = gramSet.iterator(); 
		int diff = 0;
		int sim = 0;
		int sumA = 0;
		int sumB = 0;
		while (it.hasNext()) { 
			String p = (String)(it.next());
			int a = 0;
			int b = 0;
			int a2 = 0;
			int b2 = 0;
			for(int i = 0 ; i < nA.length; i++){
				if(nA[i].equals(p)){
					a++;
					a2 = 1;
				}
			}
			for(int i = 0 ; i < nB.length; i++){
				if(nB[i].equals(p)){
					b++;
					b2 = 1;
				}
			}
			sumA += a2;
			sumB += b2;
			//System.err.println(p+" "+nA+" "+nB);
			diff += Math.abs(a - b);
			if(((a>0)&&(b>0)) || ((a == 0)&&(b==0))){
				sim += 1;
			}
		}
		double[] d = new double[2];
		d[0] = (double) diff;
		d[1] = (sim + 0.0) / (sumA + sumB - sim + 0.0);
		//System.err.println("diff20="+diff20+" sim20="+sim20+" jac="+d[1]);
		return d;
		
	}
}
