/** An Interface to OPENNLP Chunking
 * 
 *  See main() for an example.
 *  
 *  @author Kimi Sun (yuesun@nicta.com.au)	
 *  @author Scott Sanner (ssanner@nicta.com.au)
 */

package nlp.opennlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.tools.lang.english.Tokenizer;
import opennlp.tools.sentdetect.SentenceDetectorME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.stream.Collectors;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.postag.POSTaggerME;
import util.Pair;

public class NER
{
	SentenceDetectorME _sdetector;
	Tokenizer          _tokenizer;
	POSTaggerME        _tagger;
	ChunkerME          _chunker;

	public NER() throws IOException
	{
		// Load models for Sentence Detector
		System.out.println("Loading models for Sentence Detector...");
		_sdetector = new SharedSentenceDetector("./models/sentdetect/EnglishSD.bin.gz");

		// Load models for Tokenizer
		System.out.println("Loading models for Tokenizer...");
		_tokenizer = new Tokenizer("./models/tokenize/EnglishTok.bin.gz");

		// Load models for POS tagging
		System.out.println("Loading models for POS Tagging...");
		_tagger = new SharedPOSTagger("./models/postag/SpanishPOS.bin.gz", (Dictionary) null);

		// Load models for Chunking
		System.out.println("Loading models for Chunking...");
		_chunker = new ChunkerME(new SuffixSensitiveGISModelReader(new File("./models/chunker/EnglishChunk.bin.gz")).getModel());

	}

	// Return value for Chunking
	public static class Chunking
	{
		public Chunking(String[][] tokens, String[][][] taggings, String[/* sent */][/* tagging */][/* label */] labels)
		{
			_tokens   = tokens;
			_taggings = taggings;

			_chunks      = new ArrayList<ArrayList<ArrayList<String>>>();
			_chunkLabels = new ArrayList<ArrayList<ArrayList<String>>>();

			for (int si = 0; si < labels.length; si++)
			{
				ArrayList<ArrayList<String>> al_chunk = new ArrayList<ArrayList<String>>();
				ArrayList<ArrayList<String>> al_label = new ArrayList<ArrayList<String>>();
				_chunks.add(al_chunk);
				_chunkLabels.add(al_label);
				for (int ti = 0; ti < taggings[si].length; ti++)
				{
					Pair p = addChunks(_tokens[si], labels[si][ti]);
					al_chunk.addAll((ArrayList<ArrayList<String>>) p._o1);
					al_label.addAll((ArrayList<ArrayList<String>>) p._o2);
				}
			}
		}

		public String[/* sent */][/* word */]            _tokens;
		public String[/* sent */][/* word */][/* tag */] _taggings;
		public ArrayList<ArrayList<ArrayList<String>>>   _chunks;      // sent, chunk, token
		public ArrayList<ArrayList<ArrayList<String>>>   _chunkLabels; // sent, chunk, token

		public Pair addChunks(String[] tokens, String[] labels)
		{

			ArrayList<String> al_chunk = new ArrayList<String>();
			ArrayList<ArrayList<String>> al_chunks = new ArrayList<ArrayList<String>>();
			ArrayList<String> al_label = new ArrayList<String>();
			ArrayList<ArrayList<String>> al_labels = new ArrayList<ArrayList<String>>();

			boolean in_chunk = false;
			for (int wi = 0; wi < tokens.length; wi++)
			{
				if (labels[wi].startsWith("B"))
				{
					if (in_chunk)
					{
						// End current chunk and start a new one
						al_chunks.add(al_chunk);
						al_labels.add(al_label);
						al_chunk = new ArrayList<String>();
						al_label = new ArrayList<String>();
					}
					// Append word to current chunk
					al_chunk.add(tokens[wi]);
					al_label.add(labels[wi]);
					in_chunk = true;
				} 
				else if (labels[wi].startsWith("I"))
				{
					// Inside a chunk, just append word to current chunk
					al_chunk.add(tokens[wi]);
					al_label.add(labels[wi]);
				} 
				else
				{
					// Don't append this word, if in chunk, this
					// terminates the current chunk
					if (in_chunk)
					{
						al_chunks.add(al_chunk);
						al_labels.add(al_label);
						al_chunk = new ArrayList<String>();
						al_label = new ArrayList<String>();
					}
					in_chunk = false;
				}
			}
			if (al_chunk.size() > 0)
			{
				al_chunks.add(al_chunk);
				al_labels.add(al_label);
			}

			return new Pair(al_chunks, al_labels);
		}

		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			for (int si = 0; si < _taggings.length; si++)
			{
				sb.append("Sentence #" + si + " [" + _tokens[si].length + "]: ");
				for (int ti = 0; ti < _taggings[si].length; ti++)
				{
					sb.append("\n- Tagging #" + ti + ": ");
					for (int wi = 0; wi < _taggings[si][ti].length; wi++)
					{
						sb.append(_tokens[si][wi] + "/");
						sb.append(_taggings[si][ti][wi] + " ");
					}
				}
				sb.append("\n- Chunks");
				for (int ci = 0; ci < _chunks.get(si).size(); ci++)
				{
					sb.append("\n  * " + _chunks.get(si).get(ci) + " / "
							+ _chunkLabels.get(si).get(ci));
				}
				sb.append("\n");
			}
			return sb.toString();
		}
	}

	public Chunking process(String content, int num_taggings)
	{

		// Extract sentences
		String[] sents      = _sdetector.sentDetect(content.toString());
		String[][][] labels = new String[sents.length][][];

		// Extract tokens
		String[][] tokens = new String[sents.length][];
		for (int n = 0; n < sents.length; n++)
			tokens[n] = _tokenizer.tokenize(sents[n]);

		// Perform POS tagging
		String[][][] taggings = new String[sents.length][][];
		for (int sent_index = 0; sent_index < tokens.length; sent_index++)
		{
			taggings[sent_index] = _tagger.tag(num_taggings, tokens[sent_index]);
		}

		// Produce chunks for each sentence
		for (int si = 0; si < taggings.length; si++)
		{
			labels[si] = new String[taggings[si].length][];
			for (int ti = 0; ti < taggings[si].length; ti++)
				labels[si][ti] = _chunker.chunk(tokens[si], taggings[si][ti]);
		}

		return new Chunking(tokens, taggings, labels);
	}
	
	public static ArrayList<Element> createElementList(Chunking chunkedData)
	{
	
		ArrayList<Element>                      elementList = new ArrayList<>();
		
		String[][]                              tokens      = chunkedData._tokens;
		String[][][]                            taggings    = chunkedData._taggings;
		ArrayList<ArrayList<ArrayList<String>>> chunkLabels = chunkedData._chunkLabels;
		
		// Adding word, tag to object Element
		for (int si = 0; si < tokens.length; si++)
		{
			String[] sentence = tokens[si];
			for (int wi = 0; wi < sentence.length; wi++)
			{
				String word = sentence[wi];
				if (! word.equals("."))
				{
					String tag      = taggings[si][0][wi];
					Element element = new Element(word, tag);
					elementList.add(element);
				}
				
			}
		}
		
		// Adding chunklable to object Element
		int elementIndex = 0;
		for (ArrayList<ArrayList<String>> chunkSentence : chunkLabels)
		{
			//System.out.println(chunkSentence);
			for (ArrayList<String> chunkWordGroup : chunkSentence)
			{
				for (String chunk : chunkWordGroup)
				{
					//System.out.println(chunk);
					Element element = elementList.get(elementIndex);
					element.setChunk(chunk);
					elementIndex++;
				}
			}
			//System.out.println();
		}
		
		// Uncomment to print out the elements
		/*for (Element element : elementList)
		{
			System.out.println(element.word + " " + element.tag + " " + element.chunk);
		}
		*/
		return elementList;

	}
	
	public static void writeToFile(ArrayList<Element> elementList, String filepath ) throws IOException
	{
		PrintWriter pw = new PrintWriter(filepath);
		for (Element element : elementList)
		{
			pw.println(element.word + "\t" + element.tag + "\t" +  element.chunk);
		}
		pw.close();
		
	}
	
	public static ArrayList<Element> loadData(String filepath) throws IOException
	{
		ArrayList<Element> elementList = new ArrayList<>();
		
		BufferedReader br =  new BufferedReader(new FileReader(filepath));
		String line;
		while ( (line = br.readLine()) != null)
		{
			String[] linearr = line.split("\t");
			
			// Input file contains 4 columns. E.g.: Sao	NC	B-LOC	B-LOC
			if (linearr.length != 4)
				continue;
			
			String  word              = linearr[0];
			String  tag               = linearr[1];
			String  predicted_chunk   = linearr[3];
			
			Element element = new Element(word, tag);
			element.setChunk(predicted_chunk);
			
			elementList.add(element);
			//System.out.println(linearr[0] + " " + linearr[1] + " " + linearr[2]);
		}
		br.close();
		
		return elementList;
	}
	
	public static void printMap(Map<String, Integer> map)
	{
		 
		for (Map.Entry<String, Integer> entry : map.entrySet()) 
		{
			System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
	 
	}
	
	@SuppressWarnings("unchecked")
	public static void clusterElements(ArrayList<Element> elementList)
	{
		ArrayList<String> perList   = new ArrayList<>();
		ArrayList<String> locList   = new ArrayList<>();
		ArrayList<String> orgList   = new ArrayList<>();
		
		for (int i = 0; i < elementList.size(); i++)
		{
			Element rootElement = elementList.get(i);
			String  rootWord    = rootElement.getWord();
			String  rootChunk   = rootElement.getChunk();
			
		    // CLustering and joining words based on chunks
			if (rootChunk.equals("B-LOC") )
			{
				int    ci         = i;
				String wordarr    = rootWord;
				
				while(true)
				{
					ci++;
					Element element   = elementList.get(ci);
					String  chunk     = element.getChunk();
					if ( chunk.equals("I-LOC"))
					{
						String word = element.getWord();
						wordarr = wordarr + " " + word;
					}
					else
						break;
				}
				locList.add(wordarr);
			}
			
			if (rootChunk.equals("B-PER") )
			{
				int    ci         = i;
				String wordarr    = rootWord;
				
				while(true)
				{
					ci++;
					Element element   = elementList.get(ci);
					String  chunk     = element.getChunk();
					if ( chunk.equals("I-PER"))
					{
						String word = element.getWord();
						wordarr = wordarr + " " + word;
					}
					else
						break;	
				}
				perList.add(wordarr);
			}
			
			if (rootChunk.equals("B-ORG") )
			{
				int    ci         = i;
				String wordarr    = rootWord;
				
				while(true)
				{
					ci++;
					Element element   = elementList.get(ci);
					String  chunk     = element.getChunk();
					if ( chunk.equals("I-ORG"))
					{
						String word = element.getWord();
						wordarr = wordarr + " " + word;
					}
					else
						break;
				}
				orgList.add(wordarr);
			}	
		}
		
		// Hash word and frequency of word
		// Hashing Organisation
		Map<String, Integer> orgMap = new HashMap<String, Integer>();
		 
		for (String temp : orgList) 
		{
			Integer count = orgMap.get(temp);
			orgMap.put(temp, (count == null) ? 1 : count + 1);
		}
		
		// Hashing Location
		Map<String, Integer> locMap = new HashMap<String, Integer>();
		 
		for (String temp : locList) 
		{
			Integer count = locMap.get(temp);
			locMap.put(temp, (count == null) ? 1 : count + 1);
		}
		
		// Hashing Person
		Map<String, Integer> perMap = new HashMap<String, Integer>();
		 
		for (String temp : perList) 
		{
			Integer count = perMap.get(temp);
			perMap.put(temp, (count == null) ? 1 : count + 1);
		}
		
		// Print out Sorted Maps
		// Print sorted Organisations
		System.out.println("Organisation:");
		Object[] o = orgMap.entrySet().toArray();
	    Arrays.sort(o, (o1, o2) -> ((Map.Entry<String, Integer>) o2).getValue().compareTo(((Map.Entry<String, Integer>) o1).getValue()));
	    for (Object e : o) 
	    {
	        System.out.println(((Map.Entry<String, Integer>) e).getKey() + " : "+ ((Map.Entry<String, Integer>) e).getValue());
	    }
	    System.out.println("\n");
	    
	    
	    // Print sorted Locations
	    System.out.println("Location:");
	    Object[] l = locMap.entrySet().toArray();
	    Arrays.sort(l, (o1, o2) -> ((Map.Entry<String, Integer>) o2).getValue().compareTo(((Map.Entry<String, Integer>) o1).getValue()));
	    for (Object e : l) 
	    {
	        System.out.println(((Map.Entry<String, Integer>) e).getKey() + " : "+ ((Map.Entry<String, Integer>) e).getValue());
	    }
	    System.out.println("\n");
	    
	    
	    //Print sorted Persons
	    System.out.println("Person:");
	    Object[] p = perMap.entrySet().toArray();
	    Arrays.sort(p, (o1, o2) -> ((Map.Entry<String, Integer>) o2).getValue().compareTo( ((Map.Entry<String, Integer>) o1).getValue()));
	    for (Object e : p) 
	    {
	        System.out.println(((Map.Entry<String, Integer>) e).getKey() + " : "+ ((Map.Entry<String, Integer>) e).getValue());
	    }
					
	}
	
	public static void main(String[] args) throws IOException
	{

		NER chunker = new NER();
		String para = "Rafael Nadal es un gran jugador. El FC Barcelona es uno de los mejores equipos del mundo.";
		//System.out.println("----------------------\n");
		Chunking chunks = chunker.process(para, 1);
		//System.out.print(chunks);
		
		//Convert Paragraph Text to Vector in the format: word tag chunk
		ArrayList<Element> elementList = createElementList(chunks);
		writeToFile(elementList, "models/myTestFile"); // Writing the elementList to a file formatted for CRF++
		
		ArrayList<Element> loadedElementList = loadData("models/test_result_3");
		clusterElements(loadedElementList);
		

	}
}
