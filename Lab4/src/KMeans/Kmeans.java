package KMeans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

public class Kmeans 
{
	private static List<Document>                documentVectorList;
	private static Map<Integer, Map>             centroids;
	private static Map<Integer, List<Document> > clusterMap;

	
	public static List<Document> docsToVectors( Map<String, Map>  bigFileDictionary )
	{
		List<Document> documentVectorList = new ArrayList<Document>();
		
		for ( Entry<String, Map> fileMap : bigFileDictionary.entrySet() ) 
		{
			String               docName     = fileMap.getKey()  ;
			Map<String, Integer> wordFreqMap = fileMap.getValue();
			Map<String, Double>  tfMap       = Document.getTFMap     (wordFreqMap);
			Map<String, Double>  tfIdfMap    = Document.calcTFIDFMap (bigFileDictionary, tfMap) ;
		
			documentVectorList.add( new Document(docName, tfIdfMap) );
		}
		return documentVectorList;
	}
	
	public static Map<Integer, Map> intialiseCentroids ( int K )
	{
		Map<Integer, Map>  centroids    =   new HashMap<Integer, Map>();
		Set<String>        allWords     =   Files2BigDictionary.allTermsMap.keySet() ;
		Random r = new Random();
		
		for (int i = 0; i < K; i++) 
		{
			Map<String, Double> randomTFIDFMap =   new HashMap<String, Double>();
			for (String word : allWords) 
			{
				double randomTFIDFScore = r.nextDouble(); //randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
				randomTFIDFMap.put(word, randomTFIDFScore);
			}
			centroids.put(i, randomTFIDFMap);
		}
		return centroids;
	}
	
	public static Double getSimilarityScore(Map<String, Double> A, Map<String, Double> B)
	{
		double score;
		
		double dotProduct = 0.0;
		double normA      = 0.0;
		double normB      = 0.0;
		
		if ( A.size() <= B.size() ) // Looping over smaller hashmap
		{
			for (Entry<String, Double> term : A.entrySet()) 
			{
				if(B.containsKey(term.getKey()))
				{
					dotProduct += term.getValue() * B.get(term.getKey());
					normA      += Math.pow(term.getValue(),      2);
					normB      += Math.pow(B.get(term.getKey()), 2);
				}
			}
		}
		else
		{
			for (Entry<String, Double> term : B.entrySet()) 
			{
				if ( A.containsKey(term.getKey()) )
				{
					dotProduct += term.getValue() * A.get(term.getKey());
					normB      += Math.pow(term.getValue(),      2);
					normA      += Math.pow(A.get(term.getKey()), 2);
				}
			}
		}
		
		score = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
		return score;
	}

	//public static void expectation( Map<Integer, Map> centroids, List<Document> documentVectorList )
	public static void expectation( )
	{
		clusterMap = new HashMap< Integer, List<Document> >();
		
		for (Document document : documentVectorList) 
		{
			BiMap<Integer, Double> scoresMap = HashBiMap.create();
			
			// Getting score for a document from each centroid and appending each score to scoresMap
			for ( Entry<Integer, Map> centroid : centroids.entrySet() ) 
			{
				double score = getSimilarityScore( centroid.getValue(), document.getTfIdfMap() );
				scoresMap.put(centroid.getKey(), score);
			}
			
			// Getting the cluster with highest score
			List<Double> scores = Lists.newArrayList(scoresMap.values());
			Collections.sort(scores, Collections.reverseOrder());          // Sorting in descending order
			int k = scoresMap.inverse().get(scores.get(0));                // Getting Cluster ID
			
			// Setting document to a cluster
			document.setClusterID(k);
			document.setDistance(scoresMap.get(k));
			
			// Getting all documents belonging to a cluster. E.g. {cluster0: [doc1, doc2], cluster1: [doc4, doc6], ...}
			if (! clusterMap.containsKey(document.k) )
			{
				clusterMap.put( document.k, new ArrayList<Document>() );
			}
			clusterMap.get(document.k).add(document);
			
			
			//System.out.println(k + " " + scoresMap + "  " + scoresMap.get(k));
		}
	}
	
	public static void maximisation()
	{
		for ( Entry<Integer, Map> centroid : centroids.entrySet() )              // Looping over each centroid
		{
			Map<String, Double>  tfIdfMapOfCentroid  = centroid.getValue();
			List<Document>       documentsInCluster  = clusterMap.get( centroid.getKey() ); // [Doc1, Doc2, Doc3, ..]
			int                  noOfDocsInCluster   = documentsInCluster.size();
			
			for ( Entry<String, Double> term : tfIdfMapOfCentroid.entrySet() )  // Looping over each TERM of a centroid's value
			{
				String word = term.getKey();
				
				double score = 0.0;
				for ( Document document: documentsInCluster )
				{
					if ( document.tfIdfMap.containsKey(word) )
					{
						score += document.tfIdfMap.get(word);
					}
				}
				score /= noOfDocsInCluster;
				tfIdfMapOfCentroid.put(word, score);
			}
		}
	}
	
	private static void runKmeans(int maxIterations) 
	{
		for (int i = 0; i < maxIterations; i++) 
		{
			expectation();
			maximisation();
		}
	}
	
	private static void printDocuments(int top) 
	{
		int clusterNo = 1;
		for (Entry<Integer, List<Document>> cluster : clusterMap.entrySet()) 
		{
			List<Document> documents = cluster.getValue();
			
			Collections.sort(documents);
			Collections.reverse(documents);
			
			System.out.println("Cluster " + clusterNo);
			clusterNo++;
			
			for (int i = 0; i < top; i++) 
			{
				System.out.println(documents.get(i).docName);
			}
			System.out.println();
		}
	}
	
	
	public static void main(String[] args) 
	{
		int K = 2;                           // Number of Clusters
		String directory          = args[0]; // Enter Directory name
		boolean ignore_stop_words = true;
		
		Map<String, Map>  bigFileDictionary   = Files2BigDictionary.filesToHashMap(directory, ignore_stop_words);
		
		documentVectorList  = docsToVectors ( bigFileDictionary ); // Convert all files to TfIdf Vectors
		centroids           = intialiseCentroids(K);               // Get initial Centroids
		
		System.out.println("\nRunning K Means...");
		runKmeans(1000); // Run KMeans 1000 times
		System.out.println();
		printDocuments(5); // Print top 5 documents of each cluster
	
		/*
		// For 2 Documents as of now
		Map<String, Double> tfMap_1    = Document.getTFMap    ( bigFileDictionary.get("file_36.txt") );
		Map<String, Double> tfIdfMap_1 = Document.getTFIDFMap ( bigFileDictionary, tfMap_1);
		
		Map<String, Double> tfMap_2    = Document.getTFMap    ( bigFileDictionary.get("file_96.txt") );
		Map<String, Double> tfIdfMap_2 = Document.getTFIDFMap ( bigFileDictionary, tfMap_2);
		
		System.out.print( getSimilarityScore( tfIdfMap_1, tfIdfMap_2)  ); */
		
	}
}
