package nl.inl.impact.ner.utils;

public class LevDistCalculator {
	
	// bron: http://en.literateprograms.org/Levenshtein_distance_(Java)
	// LD zonder gewichten, geen transpositie
	public static int computeLD(String s1, String s2) {
		System.err.println("LD. "+s1+" "+s2);
		int[][] dp = new int[s1.length() + 1][s2.length() + 1];
		for (int i = 0; i < dp.length; i++) {
			for (int j = 0; j < dp[i].length; j++) {
				dp[i][j] = i == 0 ? j : j == 0 ? i : 0;
				if (i > 0 && j > 0) {
					if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
						dp[i][j] = dp[i - 1][j - 1];
						//System.err.println("if ==. s1="+ s1.charAt(i-1)+" s2=" + s2.charAt(j-1));
					} else {
						//System.err.println("else !=. s1="+ s1.charAt(i-1)+" s2=" + s2.charAt(j-1));
						dp[i][j] = Math.min(dp[i][j - 1] + 1, Math.min(
								dp[i - 1][j - 1] + 1, dp[i - 1][j] + 1));
					}
				}
			}
		}
		return dp[s1.length()][s2.length()];
	}
}
