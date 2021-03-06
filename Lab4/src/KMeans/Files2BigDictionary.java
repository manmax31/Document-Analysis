package KMeans;

import util.FileFinder;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Files2BigDictionary {
    public static HashMap<String, Term> allTermsMap;

    /*
     * This function reads each file in a directory, calculates the frequency of each word in a file and creates a wordFreqMap of that file
     * It then puts the fileName and its wordFreqMap to bigFileDictionary
     * @param directory         : path to directory
     * @param ignore_stop_words : true to ignore stop words
     */
    public static Map<String, Map> filesToHashMap(String directory, boolean ignore_stop_words) {
        allTermsMap = new HashMap<String, Term>();
        Map<String, Map> bigFileDictionary = new HashMap<String, Map>(); // {fileName1: {wordFreqMap}, ...}

        ArrayList<File> files = FileFinder.GetAllFiles(directory, "txt", true); // Get all .TXT files
        System.out.println("Found " + files.size() + " files.");

        int file_count = 0;

        for (File file : files) {

            String file_content = DocUtils.ReadFile(file);
            List<String> tokensList = DocUtils.getTokensList(file_content, ignore_stop_words);
            Set<String> tokensSet = DocUtils.getTokensSet(tokensList);

            // Getting the frequency of each token in the tokensList and creating a dictionary of word and frequency
            Map<String, Integer> wordFreqMap = tokensList.parallelStream().flatMap(s -> Arrays.asList(s.split(" ")).stream()).
                    collect(Collectors.toConcurrentMap(w -> w.toLowerCase(), w -> 1, Integer::sum));

            // Adding the file and wordFreqMap to bigFileDictionary
            bigFileDictionary.put(file.getName(), wordFreqMap);

            // Number of Documents with term t in it
            for (String token : tokensSet) {
                if (!allTermsMap.containsKey(token)) {
                    allTermsMap.put(token, new Term(token));
                }
                allTermsMap.get(token).docFreq++;
            }

            if (++file_count % 500 == 0)
                System.out.println("Read " + file_count + " files.");
        }

        System.out.println("All files and words, frequencies are Hashed");
        System.out.println("Corpus contains " + allTermsMap.size() + " unique words");

        return bigFileDictionary;
    }


    public static void main(String[] args)
    {
        String directory = "data/blog_data/";
        Map<String, Map> bigFileDictionary = filesToHashMap(directory, true);
        System.out.println(bigFileDictionary);

    }

}
