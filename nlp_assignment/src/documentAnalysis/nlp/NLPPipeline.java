package documentAnalysis.nlp;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import documentAnalysis.nlp.struct.Document;
import documentAnalysis.nlp.struct.Sentence;
import documentAnalysis.nlp.struct.TaggedToken;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * The NLPPipeline wraps the StanfordCoreNLP and returns the NLP preprocessing results with 
 * the data structures in documentAnalysis.nlp.struct. It has enabled the sentence splitter and the tokenizer as default.
 * You can configure whether to use the lemmatizer and the POS tagger by setting the NLPPipeline constructor.
 * 
 * @author Lizhen Qu
 *
 */
public class NLPPipeline 
{
	protected boolean lemmatizationEnabled;
	protected boolean posTaggingEnabled;
	
	/**
	 * Initialize the pipeline. 
	 * 
	 * @param enableLemmatization the lemmatizer is enabled if enableLemmatization = true.
	 * @param enablePOSTagging the POS tagger is enabled if enablePOSTagging = true.
	 */
	public NLPPipeline(boolean enableLemmatization, boolean enablePOSTagging)
	{
		if(enableLemmatization)
		{
			if(!enablePOSTagging)
			{
				throw new IllegalArgumentException("Lemmatization requires POS tagging.");
			}
		}
		lemmatizationEnabled = enableLemmatization;
		posTaggingEnabled = enablePOSTagging;
	}
	
	/**
	 * Return a list of documents after preprocessing with Stanford NLP tools.
	 * @param dirFilePath the file path of the data directory.
	 * @return a list of document objects after preprocessing with Stanford NLP tools.
	 * @throws TextProcessingException
	 */
	public List<Document> process(String dirFilePath)throws TextProcessingException
	{
		File dir = new File(dirFilePath);
		return process(dir);
		
	}
	/**
	 * Return a list of documents after preprocessing with Stanford NLP tools.
	 * @param directory the file object of the data directory.
	 * @return a list of document objects.
	 * @throws TextProcessingException
	 */
	public List<Document> process(File directory) throws TextProcessingException 
	{
		File[] textFiles = directory.listFiles(new FilenameFilter()
		{

			@Override
			public boolean accept(File dir, String name) 
			{
				// TODO Auto-generated method stub
				return name.endsWith(".txt");
			}
			 
		 });
		
		TextReader reader = new TextReader();
		Properties props = new Properties();
		
		String config = "tokenize, ssplit";
		if(this.posTaggingEnabled)
		{
			config +=", pos";
			if(this.lemmatizationEnabled)
			{
				config +=", lemma";
			}
		}
		
	    props.put("annotators", config);
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
		List<Document> docList = new ArrayList<Document>(textFiles.length);
		
		for(File txtFile : textFiles)
		{
			try 
			{
				String txt = reader.read(txtFile);
				Annotation doc = new Annotation(txt);
				pipeline.annotate(doc);
				
				Document document = new Document();
				document.setOriginalText(txt);
				List<CoreMap> sentences = doc.get(SentencesAnnotation.class);
				for(CoreMap sentence: sentences) 
				{
				// traversing the words in the current sentence
				  Sentence sens = new Sentence();
			      // a CoreLabel is a CoreMap with additional token-specific methods
			      for (CoreLabel token: sentence.get(TokensAnnotation.class))
			      {
			        // this is the text of the token
			        String word = token.get(TextAnnotation.class);
			        
			        TaggedToken wordToken = new TaggedToken(word);
			        if(posTaggingEnabled)
			        {
			        	// this is the POS tag of the token
				        String pos = token.get(PartOfSpeechAnnotation.class);
			        	wordToken.setPos(pos);
			        }
			        
			        if(lemmatizationEnabled)
			        {
			        	 // this is the lemma of the token
				        String lemma = token.get(LemmaAnnotation.class);   
				        wordToken.setLemma(lemma);
			        }
			        
			        sens.add(wordToken);
			      }
			      document.add(sens);
				 }
				docList.add(document);
			} catch (IOException e) 
			{
				e.printStackTrace();
				throw new TextProcessingException(e);
			}
			
		}
		
		return docList;
	}
}