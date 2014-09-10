package KMeans;

public class Term 
{
	String term;
	int docFreq;
	
	
	public Term(String term)
	{
		this.term = term;
		docFreq   = 0;
	}
	
	public void setTerm(String term)
	{
		this.term = term;
	}
	public void setDocFreq(int docFreq)
	{
		this.docFreq = docFreq;
	}
	
	
	public String getTerm()
	{
		return this.term;
	}
	public int getDocFreq()
	{
		return this.docFreq;
	}

}