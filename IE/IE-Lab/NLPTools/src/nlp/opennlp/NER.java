/** An Interface to OPENNLP Chunking
 * 
 *  See main() for an example.
 *  
 *  @author Kimi Sun (yuesun@nicta.com.au)	
 *  @author Scott Sanner (ssanner@nicta.com.au)
 */

package nlp.opennlp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.tools.lang.english.Tokenizer;
import opennlp.tools.sentdetect.SentenceDetectorME;

import java.util.ArrayList;

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
		_tagger = new SharedPOSTagger("./models/postag/SpanishPOS.bin.gz",(Dictionary) null);

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
		public ArrayList<ArrayList<ArrayList<String>>>   _chunks; // sent, chunk,
																// token
		public ArrayList<ArrayList<ArrayList<String>>>   _chunkLabels; // sent,
																		// chunk,
																		// token

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
			System.out.println();
		}
		
		for (Element element : elementList)
		{
			System.out.println(element.word + " " + element.tag + " " + element.chunk);
		}
		
		return elementList;

	}
	
	public static void writeToFile(ArrayList<Element> elementList ) throws IOException
	{
		PrintWriter pw = new PrintWriter("models/myTestFile");
		for (Element element : elementList)
		{
			pw.println(element.word + "\t" + element.tag + "\t" +  element.chunk);
		}
		pw.close();
		
	}

	public static void main(String[] args) throws IOException
	{

		NER chunker = new NER();
		String para = "Casi cinco meses despu�s de lesionarse el menisco en Cornell�-El Prat, Raphael Varane est� a punto de reaparecer. "
				+ "Podr�a hacerlo hoy mismo, en el Bernab�u, ante una afici�n que le a�ora m�s que nunca.La noticia surgi� ayer a las cuatro de la tarde. "
				+ "Los jugadores del Real Madrid saltaban al terreno de juego para encarar el entrenamiento previo al partido de Champions y Bale no aparec�a "
				+ "en el grupo. Estaban todos menos Xabi Alonso y el gal�s, que aquejado de unas molestias se quedaba en el gimnasio.La pizarra de Simeone volvi� a "
				+ "funcionar. En el Atl�tico todo est� trabajado y las jugadas a bal�n parado cobran una importancia vital. En Do Dragao, una falta sacada por Gabi "
				+ "sorprendi� a la defensa del Oporto. Se esperaba un disparo directo sobre la porter�a de Helton, pero Gabi dej� a Arda ante el portero con un pase"
				+ ". El turco no lo desaprovech�. El gol del Atl�tico debi� ser anulado por Howard Webb, seg�n And�jar Oliver, porque Turan recibe la pelota de Gabi "
				+ "en posici�n de fuera de juego aunque por escasos cent�metros. on estas palabras abandon� Xavi la zona mixta tras la victoria en Celtic Park por 0"
				+ "-1. Se mostr� contento con el inicio de temporada del Barcelona, en el que lo han ganado todo por ahora."
				+ " Rafael Nadal Parera (Manacor, Mallorca, 3 de junio de 1986), conocido tambi�n como Rafa Nadal, es un tenista espa�ol, actual n0 2 del ranking de "
				+ "la ATP . Hasta la fecha, ha resultado campe�n en 13 torneos de Grand Slam, lo que supone ser el tercer jugador profesional con m�s t�tulos "
				+ " \"grandes\" en la historia del tenis tras Pete Sampras y Roger Federer (17). El Arsenal, el Atl�tico Madrid y el Barcelona, igual que el Schalke"
				+ ", subrayaron su dominio al obtener su segunda victoria en la Liga de Campeones, renacida para el Chelsea y el Borussia Dortmund, que ganaron con "
				+ "autoridad sus respectivos compromisos despu�s de salir malparados de la primera jornada.La sesi�n dej� tocados al Steaua Bucarest, al Marsella y "
				+ "al Celtic, sin puntos despu�s de los dos partidos jugados.";
		//String para = "Rafael Nadal es un gran jugador. El FC Barcelona es uno de los mejores equipos del mundo.";
		//System.out.println("----------------------\n");
		Chunking chunks = chunker.process(para, 1);
		//System.out.print(chunks);
		ArrayList<Element> elementList = createElementList(chunks);
		writeToFile(elementList);
		
		//ArrayList<Element> data = createData(chunks);
		

	}
}
