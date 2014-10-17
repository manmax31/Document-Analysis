/** An Interface to OPENNLP Part of Speech Tagging
 * 
 *  See main() for an example.
 *  
 *  @author Kimi Sun (yuesun@nicta.com.au)	
 *  @author Scott Sanner (ssanner@nicta.com.au)
 */

package nlp.opennlp;

import java.io.IOException;
import opennlp.tools.lang.english.Tokenizer;
import opennlp.tools.sentdetect.SentenceDetectorME;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.InvalidFormatException;

public class POSTagger {
	
	SentenceDetectorME _sdetector;
	Tokenizer          _tokenizer;
	POSTaggerME        _tagger;

	// Constructor
	public POSTagger() throws IOException {

		// Load models for Sentence Detector
		System.out.println("Loading models for Sentence Detector...");
		_sdetector = new SharedSentenceDetector(
				"/home/gferraro/Documents/anu/Lab1/models/sentdetect/EnglishSD.bin.gz");

		// Load models for Tokenizer
		System.out.println("Loading models for Tokenizer...");
		_tokenizer = new Tokenizer(
				"/home/gferraro/Documents/anu/Lab1/models/tokenize/EnglishTok.bin.gz");

		// Load models for POS tagging
		System.out.println("Loading models for POS Tagging...");
		_tagger = new SharedPOSTagger(
				"/home/gferraro/Documents/anu/Lab1/models/postag/SpanishPOS.bin.gz", (Dictionary) null);
	}

	// Return value for POSTagging
	public static class POSTagging {
		public POSTagging(String[][] tokens, String[][][] taggings) {
			_tokens   = tokens;
			_taggings = taggings;
		}
		public String[/*sent*/][/*word*/]          _tokens;
		public String[/*sent*/][/*tag*/][/*word*/] _taggings;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int si = 0; si < _taggings.length; si++) {
				sb.append("Sentence #" + si + " [" + _tokens[si].length + "]: ");
				for (int ti = 0; ti < _taggings[si].length; ti++) {
					sb.append("\n- Tagging #" + ti + ": ");
					for (int wi = 0; wi < _taggings[si][ti].length; wi++) {
						sb.append(_tokens[si][wi] + "/");
						sb.append(_taggings[si][ti][wi] + " ");
					}
				}
				sb.append("\n\n");
			}
			return sb.toString();
		}
	}
	
	// Main method to process a String of text and return a POS
	// tagging for each word of each sentence in the text.
	public POSTagging process(String para, int num_tags) {

		// Extract sentences
		String[] sents = _sdetector.sentDetect(para.toString());

		// Extract tokens
		String[][] tokens = new String[sents.length][];
		for (int n = 0; n < sents.length; n++) 
			tokens[n] = _tokenizer.tokenize(sents[n]);
		
		// Perform POS tagging
		String[][][] taggings = new String[sents.length][][];
		for (int sent_index = 0; sent_index < tokens.length; sent_index++) {
			taggings[sent_index] = 
				_tagger.tag(num_tags, tokens[sent_index]);
		}

		return new POSTagging(tokens, taggings);
	}	
	
	//////////////////////////////////////////////////////////////////
	//                              Tests
	//////////////////////////////////////////////////////////////////
	
	public static void main(String[] args) throws IOException {
		
		POSTagger tagger = new POSTagger();
		String para1 = "Jack isn't a girl. Colorless dreams swim through steel.";
		String para2 = "El gato come pescado.  Hoy es jueves";
		System.out.println("----------------------\n");
		POSTagging tags1 = tagger.process(para1, 1); // best tagging only
		System.out.print(tags1);
		System.out.println("----------------------\n");
		POSTagging tags2 = tagger.process(para2, 3); // 3 best taggings
		System.out.print(tags2);
		System.out.println("----------------------\n");
	}
}
