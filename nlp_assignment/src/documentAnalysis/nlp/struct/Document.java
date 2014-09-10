package documentAnalysis.nlp.struct;

import java.util.ArrayList;

/**
 * The document class is a list of sentences with the type Sentence.
 * 
 * @author Lizhen Qu
 *
 */
public class Document extends ArrayList<Sentence> 
{
	private static final long serialVersionUID = 1L;
	protected String originalText;
	public String getOriginalText() 
	{
		return originalText;
	}
	public void setOriginalText(String originalText)
	{
		this.originalText = originalText;
	}
	
}
