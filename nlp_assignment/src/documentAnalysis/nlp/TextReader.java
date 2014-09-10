package documentAnalysis.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TextReader {

	public String read(File textFile)throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(textFile));
		
		StringBuilder buffer = new StringBuilder();
		
		String s;
		while((s = in.readLine()) != null){
			buffer.append(s);
			buffer.append(System.lineSeparator());
		}
		
		in.close();
		return buffer.toString();
	}
	
	
}
