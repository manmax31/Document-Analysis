package KMeans;

import nlp.nicta.filters.SnowballStemmer;
import nlp.nicta.filters.StopWordChecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DocUtils {
    public static final String SPLIT_TOKENS = "[!\"#$%&'()*+,./:;<=>?\\[\\]^`{|}~\\s]"; // missing: [_-@]

    public static String ReadFile(File f) {
        return ReadFile(f, false);
    }

    public static String ReadFile(File f, boolean keep_newline) {
        try {
            StringBuilder sb = new StringBuilder();
            java.io.BufferedReader br = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                sb.append((sb.length() > 0 ? (keep_newline ? "\n" : " ") : "") + line);
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            System.out.println("ERROR: " + e);
            return null;
        }
    }

    public static List<String> getTokensList(String file_content, boolean ignore_stop_words) {

        List<String> tokensList = new ArrayList<String>();
        StopWordChecker _swc = new StopWordChecker();
        SnowballStemmer sbs = new SnowballStemmer();

        String[] tokens = file_content.split(SPLIT_TOKENS);

        for (String token : tokens) {
            token = token.trim().toLowerCase().replaceAll("�", "'");
            token = sbs.stem(token); // Using Snow Ball Stemmer
            if (token.length() == 0 || (ignore_stop_words && _swc.isStopWord(token)))
                continue;
            tokensList.add(token);
        }

        return tokensList;
    }

    public static Set<String> getTokensSet(List<String> tokensList) {
        Set<String> tokensSet = new HashSet<String>(tokensList);
        return tokensSet;
    }

    public static void main(String[] args) throws IOException {
        String file = "data/blog_data/file_1.txt";
        byte[] encoded = Files.readAllBytes(Paths.get(file));
        String file_content = new String(encoded, "UTF8");
        List tokensList = getTokensList(file_content, true);
        System.out.println(tokensList);
        System.out.println(getTokensSet(tokensList));
    }

}
