package com.rumly.pocketlibj;

public class TextPreprocess {
	
	public static String process(String s) {
		StringBuilder sb = new StringBuilder(s.length());
		
		if (s.length() < 10) {
			return s;
		}
		
		int iStart = 1;
		int iEnd = s.length() - 2;
		int lastMatch = 0;
		for (int i = iStart; i < iEnd; i++) {
			if (s.charAt(i) != '\n') {
				continue;
			}
			String sAppend = " ";
			int iStartAppend = lastMatch;
			int iEndAppend = i;
			lastMatch = i + 1;

			if (i > 0 && s.charAt(i - 1) == '\n') {
				continue;
			}
			
			int j = i - 1;
			while (j > 0 && Character.isWhitespace(s.charAt(j))) {
				j--;
			} // and search for [.!?"']
			switch (s.charAt(j)) {
				case '.':
				case '!':
				case '?':
				case '"':
				case '\'':
					sAppend = "\n";
			}
			
			// if a newline is followed by a whitespace
			if (i < s.length() && Character.isWhitespace(s.charAt(i + 1))) {
				sAppend = "\n";
			}
			
			sb.append(s.substring(iStartAppend, iEndAppend));				
			sb.append(sAppend);
		}
		
		sb.append(s.substring(lastMatch));
		
		return sb.toString();
	}
}
