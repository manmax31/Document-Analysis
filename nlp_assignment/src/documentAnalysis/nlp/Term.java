package documentAnalysis.nlp;

import java.util.List;

public class Term 
{
	private String term;
	private double c_value;
	private double nc_value;
	private List<String> contexTerms;
	
	public Term(String term, double c_value, double nc_value)
	{
		this.term = term;
		this.c_value = c_value;
		this.nc_value = nc_value;
	}
	
	public void setTerm(String term)
	{
		this.term = term;
	}
	
	public void setCvalue(double c_value)
	{
		this.c_value = c_value;
	}
	
	public void setNCvalue(double nc_value)
	{
		this.nc_value = nc_value;
	}
	
	public void setContextTerms(List<String> contexTerms)
	{
		this.contexTerms = contexTerms;
	}
	
	public String getTerm()
	{
		return this.term;
	}
	
	public double getCvalue()
	{
		return this.c_value;
	}	
	
	public double getNCvalue()
	{
		return this.nc_value;
	}
	
	public List<String> getContexTerms()
	{
		return this.contexTerms;
	}
	public int getNoOfTerms()
	{
		String[] termarr = this.term.split(" ");
		return termarr.length;
	}
	
}
