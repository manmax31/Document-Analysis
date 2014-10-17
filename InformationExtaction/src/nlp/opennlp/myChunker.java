package nlp.opennlp;

import java.io.File;
import java.io.IOException;

import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.tools.lang.english.Tokenizer;
import opennlp.tools.sentdetect.SentenceDetectorME;

import java.util.ArrayList;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.postag.POSTaggerME;

import util.Pair;
import java.io.FileReader;
import java.io.BufferedReader;

public class myChunker
{

    SentenceDetectorME _sdetector;
    Tokenizer          _tokenizer;
    POSTaggerME        _tagger;
    ChunkerME          _chunker;

    public myChunker() throws IOException
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
        public Chunking(String[][] tokens, String[][][] taggings,
                        String[/*sent*/][/*tagging*/][/*label*/] labels)
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
                    al_chunk.addAll((ArrayList<ArrayList<String>>)p._o1);
                    al_label.addAll((ArrayList<ArrayList<String>>)p._o2);
                }
            }
        }

        public String[/*sent*/][/*word*/]              _tokens;
        public String[/*sent*/][/*word*/][/*tag*/]     _taggings;
        public ArrayList<ArrayList<ArrayList<String>>> _chunks; // sent, chunk, token
        public ArrayList<ArrayList<ArrayList<String>>> _chunkLabels; // sent, chunk, token

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
                    if (in_chunk) {
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
            // _tokens[si][wi] - word
            // _taggings[si][ti][wi] - part of speech

            ArrayList<String> wordList  = new ArrayList<String>();
            ArrayList<String> tagList   = new ArrayList<String>();
            ArrayList<String> chunkList = new ArrayList<String>();


            StringBuilder sb = new StringBuilder();
            for (int si = 0; si < _taggings.length; si++)
            {

                // create token list
                if (_taggings[si].length == 0) continue;
                for (int wi = 0; wi < _taggings[si][0].length; wi++)
                {
                    wordList.add(_tokens[si][wi]);
                    tagList.add(_taggings[si][0][wi]);
                }

                // remove last element
                System.out.println("Last element: " + wordList.get(wordList.size() - 1));
                if (wordList.size() > 0) wordList.remove(wordList.size() - 1);
                if (tagList.size() > 0) tagList.remove(tagList.size() - 1);

                // create chunk list
                for (int ci = 0; ci < _chunkLabels.get(si).size(); ci++)
                {
                    for (int cci = 0; cci < _chunkLabels.get(si).get(ci).size(); cci++)
                    {
                        chunkList.add(_chunkLabels.get(si).get(ci).get(cci));
                    }
                }

                System.out.println("wordList.size() = " + wordList.size() + "\ntagList.size() = " + tagList.size() + "\nchunkList.size() = " + chunkList.size() + "\n");
                if (wordList.size() != tagList.size() || tagList.size() != chunkList.size())
                {
                    System.out.println("Invalid array size!");
                    continue;
                }

                for (int i = 0; i < wordList.size(); i++)
                {
                    sb.append(wordList.get(i) + "\t" + tagList.get(i) + " " + chunkList.get(i) + "\n");
                }

                sb.append(".\t. O\n\n");
                wordList.clear();
                tagList.clear();
                chunkList.clear();
            }
            return sb.toString();
        }
    }

    public Chunking process(String content, int num_taggings)
    {

        // Extract sentences
        String[] sents = _sdetector.sentDetect(content.toString());
        String[][][] labels = new String[sents.length][][];

        // Extract tokens
        String[][] tokens = new String[sents.length][];
        for (int n = 0; n < sents.length; n++)
            tokens[n] = _tokenizer.tokenize(sents[n]);

        // Perform POS tagging
        String[][][] taggings = new String[sents.length][][];
        for (int sent_index = 0; sent_index < tokens.length; sent_index++)
        {
            taggings[sent_index] =
                    _tagger.tag(num_taggings, tokens[sent_index]);
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

    public static class MyDataElement
    {
        public String word;
        public String tag;
        public String chunk;

        boolean isEndOfSentence; // this is false by default

        public MyDataElement()
        {
        };
    }

    public static ArrayList<MyDataElement> createMyData(Chunking chunkData)
    {
        ArrayList<MyDataElement> myData = new ArrayList<MyDataElement>();
        int wordIndex = 0;

        for (int si = 0; si < chunkData._taggings.length; si++)
        {
            // create token list
            if (chunkData._taggings[si].length == 0) continue;
            for (int wi = 0; wi < chunkData._taggings[si][0].length; wi++)
            {
                MyDataElement element = new MyDataElement();
                element.word = chunkData._tokens[si][wi];
                element.tag = chunkData._taggings[si][0][wi];
                myData.add(element);
            }

            // remove last element
            System.out.println("Last element: " + myData.get(myData.size() - 1).word);
            if (myData.size() > 0) myData.remove(myData.size() - 1);

            // create chunk list
            for (int ci = 0; ci < chunkData._chunkLabels.get(si).size(); ci++)
            {
                for (int cci = 0; cci < chunkData._chunkLabels.get(si).get(ci).size(); cci++)
                {
                    if (wordIndex > myData.size())
                    {
                        System.out.println("There are more chunks than there are words!");
                        break;
                    }
                    myData.get(wordIndex).chunk = chunkData._chunkLabels.get(si).get(ci).get(cci);
                    wordIndex++;
                }
            }

            // append '.' at the end of the sentence
            MyDataElement element = new MyDataElement();
            element.word   = new String(".");
            element.tag    = new String(".");
            element.chunk  = new String("O");
            element.isEndOfSentence = true;
            myData.add(element);
            wordIndex++;
        }

        return myData;
    }

    public static String getStringFromMyData(ArrayList<MyDataElement> data)
    {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++)
        {
            sb.append(data.get(i).word + "\t" + data.get(i).tag + " " + data.get(i).chunk + "\n");
            if (data.get(i).isEndOfSentence) sb.append("\n");
        }
        return sb.toString();
    }

    public static ArrayList<MyDataElement> loadMyDataFromFile(String filePath)
    {

        ArrayList<MyDataElement> data = new ArrayList<MyDataElement>();

        try
        {
            FileReader fr = new FileReader(filePath);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null)
            {
                MyDataElement element = new MyDataElement();
                int firstTab = line.indexOf('\t');
                int secondTab = line.indexOf('\t', firstTab + 1);
                if (firstTab < 0 || secondTab < 0) continue;
                element.word = line.substring(0, firstTab);
                element.tag = line.substring(firstTab + 1, secondTab);
                element.chunk = line.substring(secondTab + 1, line.length());
                data.add(element);
            }
            fr.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        return data;
    }

    public static String processMyData(ArrayList<MyDataElement> data) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < data.size(); i++)
        {
            if (data.get(i).tag.compareTo("NP") != 0) continue; // does not start with NP

            if (i < data.size() - 1)
            {
                if (data.get(i + 1).tag.compareTo("NP") == 0) { // NP + NP
                    sb.append(data.get(i).word + " " + data.get(i + 1).word + "\n");
                    i++; // skip next word
                    continue;
                }

                if (data.get(i + 1).tag.compareTo("VIP") == 0 || data.get(i + 1).tag.compareTo("VIC") == 0 || data.get(i + 1).tag.compareTo("AQ") == 0)
                { // NP + (VIP or VIC)
                    sb.append(data.get(i).word + " " + data.get(i + 1).word + "\n");
                    i++; // skip next word
                    continue;
                }

                if (i < data.size() - 2)
                {
                    if ((data.get(i + 1).tag.compareTo("CC") == 0 || data.get(i + 1).tag.compareTo("SPS") == 0) && data.get(i + 2).tag.compareTo("NP") == 0) { // NP + (CC or SPS) + NP
                        sb.append(data.get(i).word + " " + data.get(i + 1).word + " " + data.get(i + 2).word + "\n");
                        i += 2; // skip next 2 words
                        continue;
                    }
                }
            }

            // if we got here then this NP is alone
            sb.append(data.get(i).word + "\n");
        }

        return sb.toString();
    }

    public static void main(String[] args) throws IOException
    {

        myChunker chunker = new myChunker();
        String para1 = "Casi cinco meses despu�s de lesionarse el menisco en Cornell�-El Prat, Raphael Varane est� a punto de reaparecer. "
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
        //String para2 = "Australia is an expansive country with numerous distinctions of kangaroos. Tokyo is a wonderful city with skyscapers as far as the eye can see.";
        System.out.println("----------------------\n");
        Chunking chunks1 = chunker.process(para1, 1);
        ArrayList<MyDataElement> data = createMyData(chunks1);
        String outputString = getStringFromMyData(data);


        System.out.println("Data from file:");
        ArrayList<MyDataElement> dataFromFile = loadMyDataFromFile("./models/test_result_2");
        outputString = getStringFromMyData(dataFromFile);

        System.out.println("Result:");
        System.out.println(processMyData(dataFromFile));

    }
}
