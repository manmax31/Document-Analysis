package documentAnalysis.nlp.struct;

/**
 * A wrapper class for comparing objects with numerical values. You can reuse or implement your own class for comparison purpose.
 * 
 * @author Lizhen Qu
 *
 * @param <T> the type of the object in comparison.
 * @param <V> the type of the value to compare. It should be a subclass of Number.
 */
public class ComparableObj<T,V extends Comparable> implements Comparable<ComparableObj<T,V>>
{
	// the object in comparison
	protected T comObject;
	// the value to compare
	protected V comValue;
	
	
	public ComparableObj(T comObject, V comValue) 
	{
		super();
		this.comObject = comObject;
		this.comValue = comValue;
	}


	@Override
	public int compareTo(ComparableObj<T, V> o) 
	{
		return -comValue.compareTo(o.getComValue());
	}


	public T getComObject() 
	{
		return comObject;
	}


	public V getComValue() 
	{
		return comValue;
	}


	@Override
	public String toString() 
	{
		return comObject.toString();
	}

	
}
