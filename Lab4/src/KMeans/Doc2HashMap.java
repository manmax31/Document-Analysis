package KMeans;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import util.FileFinder;

public class Doc2HashMap 
{	
	public Doc2HashMap(String directory, boolean ignore_stop_words)
	{
		HashMap<String, Term> allTermsMap =  new HashMap<String, Term>();
		ArrayList<File> files = FileFinder.GetAllFiles(directory, "", true);
		System.out.println("Found " + files.size() + " files.");
		
		int file_count = 0;
		
		for (File file : files) 
		{
			String       file_content  = DocUtils.ReadFile(file);
			List<String> tokensList    = DocUtils.getTokensList(file_content, ignore_stop_words);
			Set<String>  tokensSet     = DocUtils.getTokensSet(tokensList);
			
			// Revise Collection Freq
			for (String token : tokensList) 
			{
				if(!allTermsMap.containsKey(token))
				{
					allTermsMap.put(token, new Term(token));
				}
				allTermsMap.get(token).totalFreq++;
			}
			
			// Revise Doc Freq
			for (String token : tokensSet) 
			{
				if(!allTermsMap.containsKey(token))
				{
					allTermsMap.put(token, new Term(token));
				}
				allTermsMap.get(token).docFreq++;
			}
			
			if (++file_count % 10 == 0)
				System.out.println("Read " + file_count + " files.");
			
			
			// Iterating over HashMap
			Iterator it = allTermsMap.entrySet().iterator();
			while(it.hasNext())
			{
				Map.Entry pairs = (Map.Entry)it.next();
				String term = ((Term) pairs.getValue()).getTerm();
				int docFreq = ((Term) pairs.getValue()).getDocFreq();
				int totalFreq = ((Term) pairs.getValue()).getCollectionFreq();
				//if (docFreq > 1)
				System.out.println(term + " DocFreq: " + docFreq + " TotalFreq: " + totalFreq);
				it.remove();
				
			}
		System.out.println();
			
		}
	}
		
	public static void toStringByDocFreqDesc()
	{
		/*
		Collection<Term> words =  doc2Map.values();
		Term[] sortedWords     = words.toArray(new Term[0]);
		Arrays.sort(sortedWords, new WordComparatorByDocFreqDesc());
		
		StringBuilder sb = new StringBuilder();
		for (Term word : sortedWords)
		{
			sb.append("[document frequency: " + word.docFreq + ", collection frequency: " +word.collectionFreq +"] " + word.term +"\n");
		}
		return sb.toString();
		*/
	}
		
		
		
	

	public static void main(String[] args) 
	{
		String directory = "data/blog_data/";
		Doc2HashMap d2h  = new Doc2HashMap(directory, true); 

	}

}
