package edu.stanford.cs276;

import java.util.*;

//doesn't necessarily have to use task 2 (could use task 1, in which case, you'd probably like to extend CosineSimilarityScorer instead)
public class SmallestWindowScorer extends CosineSimilarityScorer {

    /////smallest window specifichyperparameters////////
    // boostmod is meant to switch btwn diff methods of discounting 
    // window score, so possible it's not used in final copy 
    // double boostmod = 1; 

    double Burl = 0.7;
    double Btitle = 0.9;
    double Bheader = 1;
    double Banchor = 1;
    double Bbody = 0.7;
    //////////////////////////////
    
    Map<String, Double> windowBs;

    // map of (field, doc, query) -> window size
    Map<String, Map<Document, Map<Query, Double>>> smallestWindowMap;

    public SmallestWindowScorer(Map<String, Double> idfs, Map<Query, Map<String, Document>> queryDict) {
        super(idfs);
        precomputeAllSmallestWindows(queryDict);
        windowBs = new HashMap<String, Double>();
        windowBs.put("url", Burl);
        windowBs.put("body", Bbody);
        windowBs.put("title", Btitle);
        windowBs.put("header", Bheader);
        windowBs.put("anchor", Banchor);
        asdf = new HashMap<String, Integer>();
    }

    /* assumes that all values in indexTermMap are already lower-cased */
    private double minWindowSize(SortedMap<Integer, String> indexTermMap, Query q) {

        // Handle query size of 1 (special case of window size 1)

        if (q.queryWords.size() == 1) {
            String query = q.queryWords.get(0).toLowerCase();

            // try to find the single word among postings terms
            for (Integer i : indexTermMap.keySet()) {
                if (indexTermMap.get(i).equals(query)) {
                    return 1.0;
                }
            }
            // otherwise, not found
            return Double.POSITIVE_INFINITY;
        }

        // Handle query sizes of two or more

        double minWindowSize = Double.POSITIVE_INFINITY;
        int startIndex = -1;
        boolean inMiddle = false;
        int queryIndex = 0;
        int queryLength = q.queryWords.size();
        double currWindowSize = Double.POSITIVE_INFINITY;

        for (Integer currIndex : indexTermMap.keySet()) {
            String queryTerm = q.queryWords.get(queryIndex).toLowerCase();
            String docTerm = indexTermMap.get(currIndex);

            if (inMiddle && docTerm.equals(q.queryWords.get(0)) && queryIndex == 1) {
                startIndex = currIndex;
            }

            // start round
            if (!inMiddle && queryTerm.equals(docTerm)) {
                if (queryIndex == queryLength - 1) {
                    return 1.0;
                }
                startIndex = currIndex;
                inMiddle = true;
                queryIndex++;

            }

            else if (inMiddle && queryTerm.equals(docTerm)) {
                // matches last term in query
                if (queryIndex == queryLength - 1) {
                    double thisWindowSize = currIndex - startIndex;
                    if (thisWindowSize < minWindowSize) {
                        minWindowSize = thisWindowSize;
                    }
                    inMiddle = false;
                    queryIndex = 0;
                }
                // matches middle term in query
                else {
                    queryIndex++;
                }
            }
        }
        return minWindowSize;
    }

    private double getMinWindowSizeForBody(Map<String, List<Integer>> body, Query q) {
        SortedMap<Integer, String> indexTermMap = new TreeMap<Integer, String>();
        for (String term : body.keySet()) {
            for (Integer pos : body.get(term)) {
                indexTermMap.put(pos, term.toLowerCase());
            }
        }
        return minWindowSize(indexTermMap, q);
    }

    /* 
     * given array of split terms and query, computes min window size. 
     * handles cases where strs is null or an empty array
     */
    private double getMinWindowSizeForStringTerm(String[] strs, Query q) {
        if (strs == null || strs.length == 0) 
          return Double.POSITIVE_INFINITY;

        SortedMap<Integer, String> indexTermMap = new TreeMap<Integer, String>();
        for (int i = 0; i < strs.length; i++) {
            indexTermMap.put(i, strs[i].toLowerCase());
        }
        return minWindowSize(indexTermMap, q);
    }

    /*
     * depending on the field, calculate the window size sligtly differently.
     * Returns Double.POSITIVE_INFINITY if no window size is found
     */
    private double getMinWindowSizeForField(String field, Document d, Query q) {
        double minSize = Double.POSITIVE_INFINITY;

        // if body, merge posting lists
        if (field.equals("body")) {
            if (d.body_hits != null)
                minSize = getMinWindowSizeForBody(d.body_hits, q);
        }

        else if (field.equals("anchors")) {
            if (d.anchors != null) {
                for (String anchor : d.anchors.keySet()) {
                    double curSize = getMinWindowSizeForStringTerm(this.splitTitleOrHeader(anchor), q);
                    if (minSize > curSize) {
                        minSize = curSize;
                    }
                }
            }
        }

        else if (field.equals("header")) {
            if (d.headers != null) {
                for (String header : d.headers) {
                    double curSize = getMinWindowSizeForStringTerm(this.splitTitleOrHeader(header), q);
                    if (minSize > curSize) {
                        minSize = curSize;
                    }
                }
            }
        }

        else {
            String [] value = null;
            if (field.equals("url")) value = this.splitUrl(d.url);
            if (field.equals("title")) value = this.splitTitleOrHeader(d.title);
            minSize = getMinWindowSizeForStringTerm(value, q);
        }
        return minSize;
    }

    /*
        during constructor, iterate over each (query, document) pair and construct
        all necessary window sizes
     */
    public void precomputeAllSmallestWindows(Map<Query, Map<String, Document>> queryDict) {
        // Initialization
        smallestWindowMap = new HashMap<String, Map<Document, Map<Query, Double>>>();
        for (String field : this.TFTYPES) {
            smallestWindowMap.put(field, new HashMap<Document, Map<Query, Double>>());
        }

        // Iterate over queryDict and for each (query, doc), find the min window size
        for (Query query : queryDict.keySet()) {
            Map<String, Document> urlDocumentMap = queryDict.get(query);
            for (String url : urlDocumentMap.keySet()) {
                Document d = urlDocumentMap.get(url);
                for (String field : this.TFTYPES) {
                    Map<Document, Map<Query, Double>> docMap = smallestWindowMap.get(field);
                    if (!docMap.containsKey(d))
                        docMap.put(d, new HashMap<Query, Double>());

                    Map<Query, Double> queryMap = docMap.get(d);
                    double windowSize = getMinWindowSizeForField(field, d, query);
                    queryMap.put(query, windowSize);
                }
            }
        }
    }

    private double getNumUniqueTerms(Query q) {
      Set<String> uniqueTerms = new HashSet<String>();
      for (String word : q.queryWords) {
        uniqueTerms.add(word.toLowerCase());
      }
      return (double)uniqueTerms.size();
    }

    private double boost(double score, double windowScore, double numUniqueTerms, double B) {
        // default boost
        // no window size found
        if (windowScore == Double.POSITIVE_INFINITY) {
          return score;
        } 

        // exact sequence found
        // full boost
        if (numUniqueTerms == windowScore) {
          return (score) * B;
        } 

        // potentially large window size found
        // boost by dereasing exponential fn
        // NOTE: 1/B seems to work better
        // than exponential decay (e.g. score * exp(-B))
        return score  * (1/B);
        // return score * Math.exp(-B);
    }
    Map<String, Integer> asdf;
    @Override
    public double getSimScore(Document d, Query q) {
        Map<String, Map<String, Double>> tfs = this.getDocTermFreqs(d, q);

        this.normalizeTFs(tfs, d, q);

        Map<String, Double> tfQuery = getQueryFreqs(q);

        double numUniqueTerms = getNumUniqueTerms(q);

        // score from cosine similarity
        double score = this.getNetScore(tfs, q, tfQuery, d);

        // find min window size across fields
        double minWindowSize = Double.POSITIVE_INFINITY;
        String minWindowSizeField = "url";

        // apply window boost for each field
        for (String field : this.TFTYPES) {
            double fieldWindowSize = smallestWindowMap.get(field).get(d).get(q);
            if (fieldWindowSize < minWindowSize) {
                minWindowSize = fieldWindowSize;
                minWindowSizeField = field;
            }
        }

        if (!asdf.containsKey(minWindowSizeField)) {
            asdf.put(minWindowSizeField, 1);
        } else 
            asdf.put(minWindowSizeField, asdf.get(minWindowSizeField) + 1);

        double factor = windowBs.get(minWindowSizeField);
        score = boost(score, minWindowSize, numUniqueTerms, factor);
        return score;
    }

}
