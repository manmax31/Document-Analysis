package documentAnalysis.nlp.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class StopList 
{
	
	/**
	 * The file path of the stop words.
	 * @param stopListPath
	 * @return a set of stop words.
	 * @throws IOException
	 */
	public static Set<String> getStopList(String stopListPath) throws IOException
	{
		
		Path file = Paths.get(stopListPath);
		List<String> strings = Files.readAllLines(file, Charset.forName("UTF-8"));
		Set<String> stoplist = new HashSet<String>();
		Properties props = new Properties();
	    props.put("annotators", "tokenize");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// add first string of line
		Pattern pattern = Pattern.compile("(\\w|')+");		
		for(String str : strings)
		{
			Matcher matcher = pattern.matcher(str);
			if(matcher.find())
			{
				if(matcher.start()==0)
				{
					String word = matcher.group();
					stoplist.add(word);
					if(word.contains("'"))
					{
						Annotation w = new Annotation(word);
						pipeline.annotate(w);
						for (CoreLabel token: w.get(TokensAnnotation.class)) 
						{
							String wordForm = token.get(TextAnnotation.class);
							stoplist.add(wordForm);
						}
					}
					
				}
			}
		}
		return stoplist;
	}
	public static void main(String[] args) 
	{
		try 
		{
			System.out.println(getStopList("stoplist"));
		} 
		catch (IOException e) 
		{// TODO Auto-generated catch block 
			e.printStackTrace();
		}
	}
}
