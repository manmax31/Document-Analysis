package nlp.opennlp;

public class Element
{
	public String word;
	public String tag;
	public String chunk;
	public int    freq;
	
	public Element(String word, String tag)
	{
		this.word  = word;
		this.tag   = tag;
	}
	
	
	// SETs
	public void setChunk(String chunk)
	{
		this.chunk = chunk;
	}
	
	public void setFreq(int freq)
	{
		this.freq = freq;
	}
	
	// GETs
	public String getWord()
	{
		return this.word;
	}
	
	public String getChunk()
	{
		return this.chunk;
	}
	
	public int getFreq()
	{
		return this.freq;
	}

}
