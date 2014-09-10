package documentAnalysis.nlp.utils;


public class POSTagConverter 
{
	/**
	 * Map the fine grained POS tag name to coarse grained POS tag.
	 * N: Noun
	 * A: Adj
	 * P: Prep
	 * O: other POS tags
	 * 
	 * The detailed list of fine grained POS tags is on https://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html
	 * 
	 * @param fineGrainedTag
	 * @return a coarse grained tag
	 */
	public static String map(String fineGrainedTag)
	{
		if(fineGrainedTag == null)
		{
			throw new IllegalArgumentException("The input argument is null.");
		}
		if(fineGrainedTag.startsWith("N"))
		{
			return "N";
		}
		else if(fineGrainedTag.startsWith("J"))
		{
			return "J";
		}
		else if(fineGrainedTag.startsWith("IN"))
		{
			return "P";
		}
		else
		{
			return "O";
		}
	}
	
	public static void main(String[] args) 
	{
		String[] posSeq = {"JJ", "NNP", "VB", "IN","NN"};
		for(String pos : posSeq)
		{
			System.out.print(map(pos));
		}
		System.out.println();
	}
}
