import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.net.URL;
import java.net.URLConnection;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
//import org.json.simple.JSONObject;
/**
 * @author Trupti Bavalatti (tb455@cornell.edu)
 *
 */
public class Disambiguate {
	final static String Wikipedia_url=  "http://en.wikipedia.org/wiki";
	
	public static final CharArraySet stopwords = createStopWordMap();
	public static final int windowSize=5;
	public static final int resultsize=5;

	public static void main(String[] args) throws Exception
	{
		String term="Bond_option";
<<<<<<< HEAD
		String query="An FDIC-supervised institution must determine if a repo-style transaction eligible margin loan bond option, "
				+ "or equity derivative contract or purchased to which the FDIC-supervised institution applies the internal models methodology under this paragraph (d) has specific wrong-way risk";
=======
		String query="An FDIC-supervised institution must determine if a repo-style transaction eligible margin loan bond "
				+ "option, or equity derivative contract or "
				+ "purchased to which the FDIC-supervised institution applies the internal models methodology under this paragraph (d) has specific wrong-way risk.";
>>>>>>> origin/master
		Map<String,String> docMap=new HashMap<String,String>();
		
		List<String> titles=new ArrayList<String>(getTitleList(term));
		docMap=getExtractList(titles);
		/*for(Entry<String,String> e:docMap.entrySet())
		{
			System.out.println(e.getKey()+":"+e.getValue());
		}*/
		//docMap=createDocumentList(term);
		String bestTitle=bestMatch(query,docMap);
		String linkTag="<a href=\""+Wikipedia_url+"/"+bestTitle+"\">"+term+"</a>";
		
	    System.out.println("best match:"+bestTitle);
		
	}
	/* *************** takes query & docs, call similary function to compute similarity and returns the best matching title************ */
	public static String bestMatch(String query,Map<String,String> docMap)
	{
		Map<String,Double> similarityMap=new HashMap<String,Double>();
		String bestTitle=null;
		similarityMap=computeSimilarity(query,docMap);
		similarityMap=sortByValue(similarityMap);
		for(Map.Entry<String, Double> entry : similarityMap.entrySet()) {
			bestTitle = entry.getKey();
			break;
		}
		return bestTitle;
		
	}
	public static Map<String,Double> computeSimilarity(String query,Map<String,String> docMap)
	{
		List<String> query_tokens=null;
		Map<String,Double> similarityMap=new HashMap<String,Double>();
		
		
		//HashMap<String, Integer> documentTermToCountMap = new HashMap<String, Integer>();
		try {
			query_tokens = getTokensForFile(query);
			//query_tokens.add("finance");
		} catch (IOException e1) {
			
			e1.printStackTrace();
		}
		HashMap<String, Integer> querytermToCountMap = new HashMap<String, Integer>();
		querytermToCountMap=getKeyValueCount(query_tokens);
		
		Map<String,Map<String,Integer>> docTokencount=new HashMap<String,Map<String,Integer>>();
		Double avdl=findAvdl(docMap);
		/* *****find all the document key value pairs ****** */
		
			      //title ,  KeyValuePairs(term,count)
		
		for(Entry<String, String> docMap1:docMap.entrySet())
		{
			List<String> document_tokens=null;
			Map<String,Integer> keyvalueCount=new HashMap<String,Integer>();
			try {
				document_tokens=getTokensForFile(docMap1.getValue());
			} catch (IOException e) {
				e.printStackTrace();
			}
			keyvalueCount=getKeyValueCount(document_tokens);
			docTokencount.put(docMap1.getKey(), keyvalueCount);
			
			int n = resultsize;
			int ni = 0;
			double bm25Result = 0.0;
			double k = 0.0;
			int dl=0;
			for(Integer d:keyvalueCount.values())
			{
				dl=dl+d;
			}
			k = 1.2 * (0.25 + (double) ((0.75 * dl) / avdl));
			
			for(String s:query_tokens)
			{
				ni = numberOfDocumentsContainingTerm(s, docTokencount);
				double part1 = 0.0;
				part1 = (double) ((n - ni + 0.5) / (ni + 0.5));
				
				int fi = 0;
				fi = findTf(s, keyvalueCount);
				double part2 = 0.0;
				part2 = (double) ((2.2 * fi) / (k + fi));

				int qfi = 0;
				qfi = findTf(s, querytermToCountMap);
				double part3 = 0.0;
				part3 = (double) ((101 * qfi) / (100 + qfi));
				
				bm25Result = bm25Result + (Math.log(part1) * part2 * part3);
			}
			similarityMap.put(docMap1.getKey(), bm25Result);
			
		}
		

		return similarityMap;
	}
	public static int findTf(String term, Map<String, Integer> map) {
		if (map.get(term) == null) {
			return 0;
		} else {
			return map.get(term);
		}
	}
	public static HashMap<String,Integer> getKeyValueCount(List<String> list)
	{
		HashMap<String, Integer> termToCountMap = new HashMap<String, Integer>();
		for (String token : list) {
			Integer currentCount = termToCountMap.get(token);
			if (currentCount == null) {
				termToCountMap.put(token, 1);
			} else {
				termToCountMap.put(token, currentCount + 1);
			}
		}
		return termToCountMap;
	}
	public static double findAvdl(Map<String,String>docMap)
	{
		Double avdl=0.0;
		for(Entry<String, String> docMap1:docMap.entrySet())
		{
			List<String> document_tokens=null;
			Map<String,Integer> keyvalueCount=new HashMap<String,Integer>();
			try {
				document_tokens=getTokensForFile(docMap1.getValue());
			} catch (IOException e) {
				e.printStackTrace();
			}
			keyvalueCount=getKeyValueCount(document_tokens);
			for(String s:keyvalueCount.keySet())
			{
				avdl=avdl+keyvalueCount.get(s);
			}	
			
		}
		avdl=avdl/resultsize;
		return avdl;
	}
	public static int numberOfDocumentsContainingTerm(String s,Map<String,Map<String,Integer>> docs)
	{
		int ni=0;
		for(Entry<String, Map<String, Integer>> doc:docs.entrySet())
		{
			Map<String,Integer> keyval=new HashMap<String,Integer>();
			keyval=doc.getValue();
			if(keyval.containsKey(s))
			{
				ni=ni+1;
			}
		}
		
		return ni;
	}
	/* ***************** get meanings from Wikipedia and return a key(title),value(meaning) pairs*********** */
	/* ********************* not used for now. Currently comparing extracts***************************  */
	public static Map<String,String> createDocumentList(String term) throws Exception{
		Map<String,String> docMap=new HashMap<String,String>();
		String wikiSearchURL = "http://en.wikipedia.org/w/api.php?action=query&list=search&srsearch="+term+"&srprop=timestamp&format=json&srlimit="+resultsize+"&srprop=snippet|titlesnippet|size";
		URL myURL = new URL(wikiSearchURL);
	    URLConnection myURLConnection = myURL.openConnection();
	    
	    BufferedReader bread = new BufferedReader(new InputStreamReader(myURLConnection.getInputStream()));
	    String resultStr = bread.readLine();
	    System.out.println(resultStr);

	    JSONParser parser=new JSONParser();
	    JSONObject jsonObject = (JSONObject) parser.parse(resultStr);
	    JSONObject query = (JSONObject) jsonObject.get("query");
	    JSONArray search= (JSONArray) query.get("search");

	    for( int i = 0; i < search.size(); i++){
	    	//System.out.println(" " + search.get(i));
	    	JSONObject ithObject = (JSONObject) search.get(i);
		    String snippet = (String) ithObject.get("snippet");
		    String title = (String) ithObject.get("title");
		    snippet=snippet.replaceAll("<span class='searchmatch'>", "");
		    snippet=snippet.replaceAll("</span>", "");
		    snippet=snippet.replaceAll("<b>...</b>", "");
		    docMap.put(title, snippet);
		    
	    	System.out.println(title + ":" + snippet);
	    	
	    }
		return docMap; 
	}
	/* ************************get titles******************************************* */
	public static List<String> getTitleList(String term) throws Exception
	{
		List<String> titles=new ArrayList<String>();
		String wikiSearchURL = "http://en.wikipedia.org/w/api.php?action=query&list=search&srsearch="+term+"&srprop=timestamp&format=json&srlimit="+resultsize+"&srprop=snippet|titlesnippet|size";
		URL myURL = new URL(wikiSearchURL);
		System.out.println("URL : " +  myURL);
	    URLConnection myURLConnection = myURL.openConnection();
	    
	    BufferedReader bread = new BufferedReader(new InputStreamReader(myURLConnection.getInputStream()));
	    String resultStr = bread.readLine();
	    System.out.println(resultStr);

	    JSONParser parser=new JSONParser();
	    JSONObject jsonObject = (JSONObject) parser.parse(resultStr);
	    JSONObject query = (JSONObject) jsonObject.get("query");
	    JSONArray search= (JSONArray) query.get("search");

	    for( int i = 0; i < search.size(); i++){
	    	//System.out.println(" " + search.get(i));
	    	JSONObject ithObject = (JSONObject) search.get(i);
		    String title = (String) ithObject.get("title");
		    title=title.replace(" ","_");
		    titles.add(title);
	    }
		return titles;
	}
	/* **************************get extracts from titles ************************** */
	public static Map<String,String> getExtractList(List<String> titles) throws Exception{
		Map<String,String> extracts=new HashMap<String,String>();
		
		for(int i=0;i<titles.size();i++)
		{
			String title=titles.get(i);
			String wikiSearchURL = "http://en.wikipedia.org/w/api.php?action=query&prop=extracts&format=json&exintro=&titles="+title;
			URL myURL = new URL(wikiSearchURL);
		    URLConnection myURLConnection = myURL.openConnection();
		    
		    BufferedReader bread = new BufferedReader(new InputStreamReader(myURLConnection.getInputStream()));
		    String resultStr = bread.readLine();
		    JSONParser parser=new JSONParser();
		    JSONObject jsonObject = (JSONObject) parser.parse(resultStr);
		    JSONObject query = (JSONObject) jsonObject.get("query");
		    JSONObject pages= (JSONObject) query.get("pages");
		    //JSONObject pageid=(JSONObject) pages.get("extract");
		    Collection<JSONObject> values = pages.values();
		    Iterator<JSONObject> iterator = values.iterator();
		    JSONObject valueString = iterator.next();
		    
		    String extract = (String) valueString.get("extract");
		    String pattern="</.>|<.>|<..>|</..>";
		    extract=extract.replaceAll(pattern,"");
<<<<<<< HEAD
		    extracts.put(title, extract);
	
		}
		
		return extracts;
	}
=======
		    
		    
		 
		    extracts.put(title, extract);

			
		}
		
		
		return extracts;
	}
	
	
>>>>>>> origin/master
	/* ******************get tokens************** */
	public static List<String> getTokensForFile(String fileContents)
			throws IOException {

		List<String> result = new ArrayList<String>();
		TokenStream tokenStream = new StandardTokenizer(new StringReader(
				fileContents));
		tokenStream = new StopFilter(tokenStream, stopwords);
		tokenStream = new PorterStemFilter(tokenStream);
		CharTermAttribute charTermAttribute = tokenStream
				.addAttribute(CharTermAttribute.class);
		tokenStream.reset();
		while (tokenStream.incrementToken()) {
			String term = charTermAttribute.toString();
			result.add(term.toLowerCase());
		}
		tokenStream.close();
		return result;

	}
	/* ******************create list of stopWords************** */
	private static CharArraySet createStopWordMap() {
		final List<String> stop_Words = new ArrayList<String>();

		BufferedReader stopWordReader = null;
		try {
			stopWordReader = new BufferedReader(new FileReader(
					"data/stopwords_indri.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line = null;
		try {
			while ((line = stopWordReader.readLine()) != null) {
				stop_Words.add(line);
			}
			stopWordReader.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return (new CharArraySet(stop_Words, true));

	}
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(
			Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(
				map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	/* ***************************** computing similarity with tf-idf ************************ */
	public static Map<String,Double> computeSimilarity_tfidf(String query,Map<String,String> docMap)
	{
		List<String> query_tokens=null;
		Map<String,Double> similarityMap=new HashMap<String,Double>();   //title,similarity
		
		
		//HashMap<String, Integer> documentTermToCountMap = new HashMap<String, Integer>();
		try {
			query_tokens = getTokensForFile(query);
			//query_tokens.add("finance");
		} catch (IOException e1) {
			
			e1.printStackTrace();
		}
		Map<String,Map<String,Integer>> docTokencount=new HashMap<String,Map<String,Integer>>();
		HashMap<String, Integer> querytermToCountMap = new HashMap<String, Integer>();
		querytermToCountMap=getKeyValueCount(query_tokens);
		for(Entry<String, String> docMap1:docMap.entrySet())
		{
			List<String> document_token=null;
			Map<String,Integer> keyvalue=new HashMap<String,Integer>();
			try {
				document_token=getTokensForFile(docMap1.getValue());
			} catch (IOException e) {
				e.printStackTrace();
			}
			keyvalue=getKeyValueCount(document_token);
			docTokencount.put(docMap1.getKey(), keyvalue);      //title ,  KeyValuePairs(term,count)
		}
		
		for(Entry<String, String> docMap1:docMap.entrySet())
		{
			List<String> document_tokens=null;
			Map<String,Integer> keyvalueCount=new HashMap<String,Integer>();
			try {
				document_tokens=getTokensForFile(docMap1.getValue());
			} catch (IOException e) {
				e.printStackTrace();
			}
			keyvalueCount=getKeyValueCount(document_tokens);
			
			int N = resultsize;
			int queryidf=0;
			int querytf=0;
			int query_tfidf=0;
			
			int docidf=0;
			int doctf=0;
			int doc_tfidf=0;
			Map<String,Integer> query_tfidfMap  = new TreeMap<String,Integer>();
			Map<String,Integer> doc_tfidfMap  = new TreeMap<String,Integer>();
			for(String s:query_tokens)
			{
				queryidf = numberOfDocumentsContainingTerm(s, docTokencount);
				querytf = findTf(s, querytermToCountMap);
				query_tfidf=querytf*queryidf;
				query_tfidfMap.put(s, query_tfidf);
				
				doctf = findTf(s, keyvalueCount);
				docidf = numberOfDocumentsContainingTerm(s, docTokencount);
				doc_tfidf=doctf*docidf;
				doc_tfidfMap.put(s, doc_tfidf);
				
			}
			Double tfidf=0.0;
			for(String key : query_tfidfMap.keySet() )
			{
				tfidf+=query_tfidfMap.get(key)* doc_tfidfMap.get(key);
			}
			similarityMap.put(docMap1.getKey(), tfidf);
			
		}
		return similarityMap;
	}
	/* ***************************** computing similarity with tf= 1+log(tf)-idf=log(N/n) ************************ */
	public static Map<String,Double> computeSimilarity_logtfidf(String query,Map<String,String> docMap)
	{
		List<String> query_tokens=null;
		Map<String,Double> similarityMap=new HashMap<String,Double>();   //title,similarity
		
		
		//HashMap<String, Integer> documentTermToCountMap = new HashMap<String, Integer>();
		try {
			query_tokens = getTokensForFile(query);
			//query_tokens.add("finance");
		} catch (IOException e1) {
			
			e1.printStackTrace();
		}
		HashMap<String, Integer> querytermToCountMap = new HashMap<String, Integer>();
		querytermToCountMap=getKeyValueCount(query_tokens);
		
		Map<String,Map<String,Integer>> docTokencount=new HashMap<String,Map<String,Integer>>();
		for(Entry<String, String> docMap1:docMap.entrySet())
		{
			List<String> document_token=null;
			Map<String,Integer> keyvalue=new HashMap<String,Integer>();
			try {
				document_token=getTokensForFile(docMap1.getValue());
			} catch (IOException e) {
				e.printStackTrace();
			}
			keyvalue=getKeyValueCount(document_token);
			docTokencount.put(docMap1.getKey(), keyvalue);      //title ,  KeyValuePairs(term,count)
		}
		for(Entry<String, String> docMap1:docMap.entrySet())
		{
			List<String> document_tokens=null;
			Map<String,Integer> keyvalueCount=new HashMap<String,Integer>();
			try {
				document_tokens=getTokensForFile(docMap1.getValue());
			} catch (IOException e) {
				e.printStackTrace();
			}
			keyvalueCount=getKeyValueCount(document_tokens);
			
			
			int N = resultsize;
			int queryidf=0;
			int querytf=0;
			double query_logtf=0.0;
			double query_logidf=0.0;
			double query_tfidf=0;
			
			int docidf=0;
			int doctf=0;
			double doc_logtf=0.0;
			double doc_logidf=0.0;
			double doc_tfidf=0;
			Map<String,Double> query_tfidfMap  = new TreeMap<String,Double>();
			Map<String,Double> doc_tfidfMap  = new TreeMap<String,Double>();
			for(String s:query_tokens)
			{
				queryidf = numberOfDocumentsContainingTerm(s, docTokencount);
				querytf = findTf(s, querytermToCountMap);
				query_logtf=1+Math.log(querytf);
				query_logidf=Math.log((double)N/queryidf);
				query_tfidf=query_logtf*query_logidf;
				
				query_tfidfMap.put(s, query_tfidf);
				
				doctf = findTf(s, keyvalueCount);
				docidf = numberOfDocumentsContainingTerm(s, docTokencount);
				doc_logtf=1+Math.log(doctf);
				doc_logidf=Math.log((double)N/docidf);
				doc_tfidf=doc_logtf*doc_logidf;
				doc_tfidfMap.put(s, doc_tfidf);
				
			}
			Double tfidf=0.0;
			for(String key : query_tfidfMap.keySet() )
			{
				tfidf+=query_tfidfMap.get(key)* doc_tfidfMap.get(key);
			}
			similarityMap.put(docMap1.getKey(), tfidf);
			
		}
		return similarityMap;
	}
}
