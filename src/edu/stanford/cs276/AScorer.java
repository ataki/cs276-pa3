package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AScorer 
{

	Map<String,Double> idfs;
	String[] TFTYPES = {"url","title","body","header","anchor"};

	public AScorer(Map<String,Double> idfs)
	{
		this.idfs = idfs;
	}
	
	//scores each document for each query
	public abstract double getSimScore(Document d, Query q);
	
	//handle the query vector
	public Map<String,Double> getQueryFreqs(Query q)
	{
		Map<String,Double> tfQuery = new HashMap<String,Double>();

        // --- Begin edits ---

        for (String word : q.queryWords) {
            word = word.toLowerCase();
            if (!tfQuery.containsKey(word)) {
                tfQuery.put(word, 1.0);
            } else {
                double count = tfQuery.get(word);
                tfQuery.put(word, count + 1.0);
            }
        }

        // -----------------

		return tfQuery;
	}
	

	
	////////////////////Initialization/Parsing Methods/////////////////////

    private String [] splitUrl(String url) {
        return url.split("\\W+");
    }

    private String [] splitTitleOrHeader(String sentence) {
        return sentence.split("\\s+");
    }
	
    ////////////////////////////////////////////////////////
	private Map<String, Double> createFreqMap(String[] terms) {
        Map<String, Double> freqMap = new HashMap<String, Double>();
        for (String term : terms) {
            if (!freqMap.containsKey(term)) {
                freqMap.put(term, 1.0);
            }
            else {
                freqMap.put(term, freqMap.get(term)+1.0);
            }
        }
        return freqMap;
    }

	
	/*/
	 * Creates the various kinds of term frequences (url, title, body, header, and anchor)
	 * You can override this if you'd like, but it's likely that your concrete classes will share this implementation
	 */
	public Map<String,Map<String, Double>> getDocTermFreqs(Document d, Query q)
	{
		//map from tf type -> queryWord -> score
		Map<String,Map<String, Double>> tfs = new HashMap<String,Map<String, Double>>();
		
		////////////////////Initialization/////////////////////

        // when calculating raw scores, lower-case terms of all fields

        String docUrl = d.url.toLowerCase();
        String docTitle = d.title.toLowerCase();
        List<String> docHeaders = new ArrayList<String>();
        if (d.headers != null) {
            for (String s : d.headers) {
                docHeaders.add(s.toLowerCase());
            }
        }
        Map<String, List<Integer>> docBodyHits = new HashMap<String, List<Integer>>();
        if (d.body_hits != null) {
            for (String term : d.body_hits.keySet()) {
                docBodyHits.put(term.toLowerCase(), d.body_hits.get(term));
            }
        }
        Map<String, Integer> docAnchors = new HashMap<String, Integer>();
        if (d.anchors != null) {
            for (String term: d.anchors.keySet()) {
                docAnchors.put(term.toLowerCase(), d.anchors.get(term));
            }

        }
       List<String> queryWords = new ArrayList<String>();
        for (String word : q.queryWords) {
            queryWords.add(word.toLowerCase());
        }

        // url
        Map<String, Double> urlMap = createFreqMap(splitUrl(docUrl));

        // title
        Map<String, Double> titleMap = createFreqMap(splitTitleOrHeader(docTitle));

        // headers
        Map<String, Double> headersMap = new HashMap<String, Double>();
        for (String header : docHeaders) {
            String headerTerms[] = splitTitleOrHeader(header);
            for (String term : headerTerms) {
                if (!headersMap.containsKey(term)) {
                    headersMap.put(term, 1.0);
                }
                else {
                    headersMap.put(term, headersMap.get(term) + 1.0);
                }
            }
        }

        // body_hits
        Map<String, Double> bodyMap = new HashMap<String, Double>();
        for (String term : docBodyHits.keySet()) {
            bodyMap.put(term, (double)docBodyHits.get(term).size());
        }

        // anchors
        Map<String, Double> anchorsMap = new HashMap<String, Double>();
        for (String anchor : docAnchors.keySet()) {
            String anchorTerms[] = anchor.split("\\s+");
            for (String term : anchorTerms) {
                double termCount = (double)docAnchors.get(anchor);
                if (!anchorsMap.containsKey(term)) {
                    anchorsMap.put(term, termCount);
                }
                else {
                    anchorsMap.put(term, anchorsMap.get(term) + termCount);
                }
            }
        }

        Map<String, Map<String, Double>> allMaps = new HashMap<String, Map<String, Double>>();
        allMaps.put("url", urlMap);
        allMaps.put("title", titleMap);
        allMaps.put("header", headersMap);
        allMaps.put("body", bodyMap);
        allMaps.put("anchor", anchorsMap);

	    ////////////////////////////////////////////////////////
		
		//////////handle counts//////

        for (String type: TFTYPES) {
            Map<String, Double> rawCountMap = new HashMap<String, Double>();
            Map<String, Double> typeCountMap = allMaps.get(type);
            for (String queryWord : queryWords) {
                double count;
                if (typeCountMap.containsKey(queryWord))
                    count = typeCountMap.get(queryWord);
                else
                    count = 0.0;

                // handle duplicate words in query
                // by accumulating scores e.g.
                // query "hello world hello" is treated
                // differently from "hello world"

                if (!rawCountMap.containsKey(queryWord))
                    rawCountMap.put(queryWord, count);
                else
                    rawCountMap.put(queryWord, rawCountMap.get(queryWord) + count);
            }
            tfs.put(type, rawCountMap);
        }
		return tfs;
	}
	

}
