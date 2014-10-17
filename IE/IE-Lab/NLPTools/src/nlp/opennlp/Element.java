package nlp.opennlp;

public class Element
{
	public String word;
	public String tag;
	public String chunk;
	
	public Element(String word, String tag)
	{
		this.word  = word;
		this.tag   = tag;
	}
	public void setChunk(String chunk)
	{
		this.chunk = chunk;
	}

}
