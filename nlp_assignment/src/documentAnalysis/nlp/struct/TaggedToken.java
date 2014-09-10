package documentAnalysis.nlp.struct;

/**
 * The token class represents a word.
 * 
 * @author Lizhen Qu
 *
 */
public class TaggedToken 
{
	// the surface form of the word.
	protected String wordForm;
	// the lemma of the word.
	protected String lemma;
	// the POS of the word.
	protected String pos;
	
	public TaggedToken(String wordForm) 
	{
		super();
		this.wordForm = wordForm;
	}
	public String getWordForm() 
	{
		return wordForm;
	}
	public void setWordForm(String wordForm) 
	{
		this.wordForm = wordForm;
	}
	public String getPos() 
	{
		return pos;
	}
	public void setPos(String pos) 
	{
		this.pos = pos;
	}
	public String getLemma() 
	{
		return lemma;
	}
	public void setLemma(String lemma) 
	{
		this.lemma = lemma;
	}
	
	@Override
	public String toString() 
	{
		return (lemma == null ? wordForm:lemma) + (pos == null?"":"/" + pos);
	}
	
}
