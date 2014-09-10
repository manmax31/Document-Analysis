package documentAnalysis.nlp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import documentAnalysis.nlp.struct.ComparableObj;
import documentAnalysis.nlp.struct.Document;
import documentAnalysis.nlp.struct.Sentence;
import documentAnalysis.nlp.struct.TaggedToken;
import documentAnalysis.nlp.utils.POSTagConverter;
import documentAnalysis.nlp.utils.StopList;

/*
 * Algorithm to implement C-Value and NC-Value
 * 
 * Function : getCvalue  implements the C-Value  algorithm
 * Function : getNCvalue implements the NC-Value algorithm
 * 
 * @author  : Manab Chetia
 */

public class CNCValue
{	
	/*
	 * This function calculates the NC-Value for each term
	 * @ param  : outputList          : contains terms sorted by C value in descending order
	 * @ param	: tokensList          : List of tokens 
	 * @ param  : indices             : of the matches of a regex pattern [[3,4], [8, 12], ...]
	 * @ param  : termLengthThreshold : used to stop adding terms to the outputList whose length < termLengthThreshold
	 */
	public static void getNCValue(List<Term> outputList, List<String> tokensList, List<ArrayList> indices, int termLengthThreshold)
	{
		// Stage I : Sort outputList by C-Value
		Map<String,Double> outCValue = new HashMap<String, Double>();
		for (Term term : outputList) 
		{
			if (term.getNoOfTerms() > termLengthThreshold)    // 1 signifies the number of terms
			{
				outCValue.put(term.getTerm(), term.getCvalue());
			}
		}
		
		
		List<ComparableObj<String, Double>> sortedCOutputList  = new ArrayList<ComparableObj<String, Double>>();
		for(Map.Entry<String, Double> entry : outCValue.entrySet())
		{
			sortedCOutputList.add(new ComparableObj<String, Double>(entry.getKey(),entry.getValue()));
		}
		Collections.sort(sortedCOutputList);
		
		// Here sorted C Value
		/*
		for (ComparableObj<String, Double> comparableObj : sortedCOutputList) 
		{
			System.out.println(comparableObj.toString());
		}
		*/
		
		// Stage II: Get Term Context Words
		Map<String, List<String>> contextMap = new HashMap<>();
		List termsList = getTerms(indices, tokensList, 5, 0); // Get 
		
	
		for(Term term: outputList)
		{
			List<String> contextTermList = new ArrayList<>();
			for (Object termLi : termsList) 
			{
				if(termLi.toString().contains(term.getTerm()))
				{
					String contextTerm         = termLi.toString();
					String replacedContextTerm = contextTerm.replace(term.getTerm(), "");
					if(! contextTerm.equals(term.getTerm()))
					{
						contextTermList.add(contextTerm.trim());
					}
				}
			}
			term.setContextTerms(contextTermList);
		}
		
		
		// Stage III: Calculate NC Value
		for(Term term : outputList)
		{
			// Calculate Frequency of each Word in each ContextTerm
			Map<String, Integer> wordFreqMap = (Map<String, Integer>) term.getContexTerms().parallelStream().
					flatMap(s -> Arrays.asList(s.split(" ")).stream()).collect(Collectors.toConcurrentMap(
							w -> w.toLowerCase(), w -> 1, Integer::sum));
			
			Double weight = 0.0;
			
			for (Map.Entry<String, Integer> word : wordFreqMap.entrySet()) 
			{
				int t_w = word.getValue();
				int n = wordFreqMap.size();
				weight += t_w * (t_w/n);
			}
			
			term.setNCvalue( 0.8*term.getCvalue() + 0.2*weight );
		}
		
		
		// Sort terms By NC Value in descending Order
		Map<String,Double> outNCValue = new HashMap<String, Double>();
		for (Term term : outputList) 
		{
			if (term.getNoOfTerms() > termLengthThreshold)    // 1 signifies the number of terms
			{
				outCValue.put(term.getTerm(), term.getNCvalue());
			}
		}
				
		List<ComparableObj<String, Double>> sortedNCOutputList  = new ArrayList<ComparableObj<String, Double>>();
		for(Map.Entry<String, Double> entry : outCValue.entrySet())
		{
			sortedNCOutputList.add(new ComparableObj<String, Double>(entry.getKey(),entry.getValue()));
		}
		Collections.sort(sortedNCOutputList);
		
		/*
		// Print terms sorted by NC Value
		for (ComparableObj<String, Double> sNCterm : sortedNCOutputList) 
		{
			System.out.println(sNCterm);
		}
		*/
		
		// Print terms, C Value, NC Value
		
		for (Term term: outputList) 
		{
			System.out.println(term.getTerm() + "  "  + term.getCvalue() + "   "  + term.getNCvalue());
		}
		
	}
	
	
	/*
	 * This function calculates the CValue and returns a list of terms with their CValue
	 * @param :  sortedTermsList (filteredTermsList which is sorted in descending order by length of each term
	 * @param :  original termsList. e.g. [internet task, share my great anotations, ...]
	 * @param :  threshold for CValue
	 * @return:  a list of the terms with their C-Value
	 */
	public static List getCValue(List<ComparableObj<String,Integer>> sortedTermsList, List<String> termsList, Double threshold)
	{
		List<Term> outputList = new ArrayList<Term>();
		
		for (ComparableObj<String, Integer> candidateTerm : sortedTermsList)          // Outer For Loop
		{
			List<String> extractedList = new ArrayList<>();
			double   cvalue = 0.0;
			
			for (ComparableObj<String, Integer> subTerm : sortedTermsList) 	          // Inner For Loop
			{
				if (   ( candidateTerm.toString().contains(subTerm.toString()) ) && !(  candidateTerm.toString().equals(subTerm.toString()) )   )
				//if (   ( SubItemMatcher.isContain(candidateTerm.toString(), subTerm.toString()) ) && !(  candidateTerm.toString().equals(subTerm.toString()) )   )
				{
					extractedList.add(subTerm.toString());
				}	
			}
			
			if (extractedList.size() > 0)
			{
				int   mod_a = candidateTerm.toString().length();
				int   f_a   =  Collections.frequency(termsList, candidateTerm.toString()); 
				int   p_Ta  =  extractedList.size();
				int   f_b   =  0;
				
				for (String eTerm : extractedList) 
				{
					f_b = Collections.frequency(termsList, eTerm.toString()); 
					
					int allNestedTermsFreq = 0;
					for (String subeTerm : extractedList) 
					{
						if (eTerm.contains(subeTerm) && !(eTerm.equals(subeTerm)))
						{
							allNestedTermsFreq += Collections.frequency(termsList, subeTerm);
						}
					}
					
					f_b = f_b - allNestedTermsFreq;
				}
				
				cvalue = (Math.log(mod_a) / Math.log(2)) * (f_a - (f_b/p_Ta));	
			}
			else
			{
				int   mod_a = candidateTerm.toString().length();
				int   f_a   = Collections.frequency(termsList, candidateTerm.toString()); 
				cvalue      = (Math.log(mod_a) / Math.log(2)) *  f_a;	
			}
			
			if(cvalue >= threshold)
			{
				Term term = new Term(candidateTerm.toString(), cvalue, 0.0);
				outputList.add(term);
			}
		}
		
		return outputList;	
	}
	
	
	/*
	 * This function sorts a list based on the length of term in descending order
	 * @param  : the list that is to be sorted
	 * @return : sorted list
	 */
	public static List getSortedList(List<String> termsList)
	{
		Map<String,Integer> termLength = new HashMap<String, Integer>();
		for (String term : termsList) 
		{
			termLength.put(term, term.length());
		}
		
		List<ComparableObj<String,Integer>> sortedList  = new ArrayList<ComparableObj<String,Integer>>();
		for(Map.Entry<String, Integer> entry : termLength.entrySet())
		{
			sortedList.add(new ComparableObj<String,Integer>(entry.getKey(),entry.getValue()));
		}
		Collections.sort(sortedList);
		
		return sortedList;
	}
	
	
	/*
	 * This functions filters terms through a stop-list
	 * @param  : termsList containing terms that are above the freq threshold
	 * @return : a new termsList that are filtered using the STOPLIST 
	 */
	public static List filterStopList(List<String> newTermsList) throws IOException
	{
		Set<String> stoplist = StopList.getStopList("stoplist");
		List<String> filteredTermsList =  new ArrayList<>();
		for (String term : newTermsList) 
		{
			StringBuilder termJoined  =  new StringBuilder();
			String[] termarr          =  term.split(" ");
			for (String termpart : termarr) 
			{
				if (!stoplist.contains(termpart))
				{
					termJoined.append(termpart + " ");
				}
			}
			filteredTermsList.add(termJoined.toString().trim());
		}
		return filteredTermsList;
	}
	
	
	/*
	 * This function keeps only the terms that are above the set freqThreshold
	 * @param   :  termsList [internet task, share my great anotations, ...]
	 * @param   :  freq: the frequency threshold below which all strings has to be deleted from the termsList
	 * @return  : returns a new termsList containing terms that are above the freq threshold
	 */
	public static List removeTermsByFreq(List<String> termsList, int freqThreshold)
	{
		HashSet<String> frequencyList  =  new HashSet();  // Storage to store term and freq [overview 1, inflations rest 2, ...]
		List<String> newTermsList      =  new ArrayList<>(); // Storage to store terms that are above the freqThreshold 
		
		for (String term : termsList) 
		{
			frequencyList.add(term + " " + Collections.frequency(termsList, term));
		}
		
		for (String term : frequencyList) 
		{
			String[] termarr = term.split(" ");
			int freq = Integer.parseInt(termarr[termarr.length-1]);
			if (freq >= freqThreshold)
			{
				StringBuilder termJoined = new StringBuilder();
				for (int i = 0; i < termarr.length-1; i++) 
				{
					termJoined.append(termarr[i] + " ");
				}
				newTermsList.add(termJoined.toString().trim());
			}
		}
		return newTermsList;
	}
	
	
	/*
	 * This functions returns the indices in the string where that particular regex matched
	 * @ Param   :  Stringbuilder S
	 * @ returns :  indices where that pattern was found in the string S i.e. [[3 4], [5 6], ...]
	 */
	@SuppressWarnings("unchecked")
	public static List getIndices(StringBuilder string, String pat)
	{
		List<ArrayList> indices = new ArrayList<>(); // Stores the indices where RegEx pattern was found in the document
		Pattern pattern = Pattern.compile(pat);
		Matcher matcher = pattern.matcher(string);
		Map<String,Integer> pattern2count = new HashMap<String, Integer>();
		//System.out.println("substring [start_position, end_position]");
		
		while(matcher.find())
		{
			String substring = matcher.group();
			//System.out.println(substring + "\t[" + matcher.start() + " , " + matcher.end() + "]" );
			
			//String[] substringarr = substring.split(" ")
			ArrayList index = new ArrayList<>(); // index = [start_index, end_index]
			index.add(matcher.start());
			index.add(matcher.end());
			
			indices.add(index);
			
			if(pattern2count.containsKey(substring))
			{
				pattern2count.put(substring, pattern2count.get(substring) + 1);
			}
			else{pattern2count.put(substring, 1);}
		}
		//System.out.println("\nFrequencies of substrings.");
		//System.out.println(pattern2count);
		return indices;
	}
	
	
	/*
	 * This functions returns a Terms List based on the indices that matches on the index
	 * @param: indices  : of the matches of a regex pattern [[3,4], [8, 12], ...]
	 * @param: tokens   : list of tokens [internet, task, the, who, share, my, great, annotations, ...]
	 * @return          : returns a List of String matching the regex pattern [internet task, share my great anotations, ...]
	 */
	public static List getTerms(List<ArrayList> indices, List tokens, int startThreshold, int endThreshold)
	{
		List<String> termsList = new ArrayList<>();
		for (ArrayList index : indices) 
		{
			int start_index = (int) index.get(0);
			int end_index = (int) index.get(1);
			
			StringBuilder term = new StringBuilder();
			
			int start;
			if (  (start_index - startThreshold) <0   )
			{
				start = 0; 
			}
			else
			{
				start = start_index - startThreshold;
			}
			
			for (int i = start; i < (end_index + endThreshold); i++) 
			{
				term.append(tokens.get(i) + " ");
			}
			
			termsList.add(term.toString().trim()); //Adding the term to the List termsList	
		}
		return termsList;
	}
	
	public static void main(String[] args) throws IOException 
	{
		// It requires the file path of the data directory as the argument of the main program.
		String errorMsg = "INPUT_DIRECTORY_FILE_PATH\n";
		List<String> tokensList = new ArrayList<String>(); // ArrayList of tagged words [(word1,N), (word2,V),...]
		StringBuilder posTags = new StringBuilder();
		int count = 0;
		if(args.length == 1)
		{			
			String dirFilePath = args[0];
			File dir = new File(dirFilePath);
			if(dir.exists())
			{
				if(dir.isDirectory())
				{					
					NLPPipeline pipeline = new NLPPipeline(false, true);					
					try 
					{
						for(Document doc : pipeline.process(dirFilePath))
						{
							count++;
							for(Sentence sentence : doc)
							{
								if(sentence.size() > 0)
								{
									
									int lastTokenIndex= sentence.size() - 1;
									for(int i = 0 ;i < lastTokenIndex; i++)
									{
										TaggedToken token = sentence.get(i);
										token.setPos(  POSTagConverter.map(sentence.get(i).getPos())   ); // Setting Coarse POS
										//taggedTokensList.add(token.getWordForm().toLowerCase() + " " + token.getPos());
										
										tokensList.add(token.getWordForm().toLowerCase()); // List of Tokens
										posTags.append(token.getPos()); // String of Tags
										//System.out.println(token.getWordForm().toLowerCase() + " " + token.getPos());
									}
								}
							}
						}
					} catch (TextProcessingException e) {e.printStackTrace();}
				}
				else{System.err.println(dirFilePath + " is not a directory.");}
			}
			else{System.err.println(dirFilePath + " cannot be found.");}
		}
		else{System.err.println(errorMsg);}
		
		// get indices of the substrings that matched the pattern [[3 4], [5 6], ...]
		List indices = getIndices(posTags, "N+N");    //((J|N)+|((J|N)*(NP)?)(J|N)*)N
		
		// [internet task, share my great annotations, ...]
		List termsList = getTerms(indices, tokensList, 0, 0);   
		
		// remove all terms from termlist that are less than the frequencyThreshold
		List newTermsList = removeTermsByFreq(termsList, 2);  //Here
		
		// filters all terms using the STOPLIST
		List filteredTermsList = filterStopList(newTermsList);
		
		// Sorts the terms of a List based on length of a term in descending order
		List sortedTermsList = getSortedList(filteredTermsList);
		
		// Get a list of terms that contains terms and Cvalue
		List outputList = getCValue(sortedTermsList, termsList, 1.0);  //Here
		

		// Print out terms ranked by NC Values, 1 is the termLengthThreshold
		getNCValue(outputList, tokensList, indices, 1);
	}
}