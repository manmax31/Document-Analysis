package documentAnalysis.nlp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubItemMatcher 
{

	/**
	 * This function checks if a substring is present in a longer Source String
	 * @param source  : sourceString
	 * @param subItem : subIstring
	 * @return        : true if SubString is present in subString else false
	 */
	public static boolean isContain(String source, String subItem)
	{
		String pattern = "\\b"+subItem+"\\b";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(source);
		return m.find();
	}
	
	public static void main(String[] args)
	{
		// Test Cases
		String source1="search engines are amazing";
	    String source2="search engine";
	    String subterm_1 = "engines";
	    String subterm_2 = "engine";

	    System.out.println(isContain(source1, subterm_1));
	    System.out.println(isContain(source2, subterm_1));
	    System.out.println(isContain(source1, subterm_2));
	    System.out.println(isContain(source2, subterm_2));
	}

}
