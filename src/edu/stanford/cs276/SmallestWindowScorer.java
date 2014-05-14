package edu.stanford.cs276;

import java.util.*;

//doesn't necessarily have to use task 2 (could use task 1, in which case, you'd probably like to extend CosineSimilarityScorer instead)
public class SmallestWindowScorer extends CosineSimilarityScorer {

    /////smallest window specifichyperparameters////////
    double B = -1;
    double boostmod = -1;

    double Burl = -1;
    double Btitle = -1;
    double Bheader = -1;
    double Banchor = -1;
    double Bbody = -1;
    //////////////////////////////

    // map of (field, doc, query) -> window size
    Map<String, Map<Document, Map<Query, Double>>> smallestWindowMap;

    public SmallestWindowScorer(Map<String, Double> idfs, Map<Query, Map<String, Document>> queryDict) {
        super(idfs);
        precomputeAllSmallestWindows(queryDict);
    }

    private double minWindowSize(SortedMap<Integer, String> indexTermMap, Query q) {
        double minWindowSize = Double.POSITIVE_INFINITY;
        int startIndex = -1;
        boolean inMiddle = false;
        int queryIndex = 0;
        int queryLength = q.queryWords.size();
        double currWindowSize = Double.POSITIVE_INFINITY;

        for (Integer currIndex : indexTermMap.keySet()) {
            String queryTerm = q.queryWords.get(queryIndex);
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
                indexTermMap.put(pos, term);
            }
        }
        return minWindowSize(indexTermMap, q);
    }

    private double getMinWindowSizeForString(String str, Query q) {
        SortedMap<Integer, String> indexTermMap = new TreeMap<Integer, String>();
        List<String> plist = Arrays.asList(str.split("\\s+"));
        for (int i = 0; i < plist.size(); i++) {
            indexTermMap.put(i, plist.get(i));
        }
        return minWindowSize(indexTermMap, q);
    }

    /*
        depending on the field, calculate the window size sligtly differently.
        Returns Double.POSITIVE_INFINITY if no window size is found
     */
    private double getMinWindowSizeForField(String field, Document d, Query q) {
        double minSize = Double.POSITIVE_INFINITY;

        // if body, merge posting lists
        if (field.equals("body")) {
            if (d.body_hits != null)
                return getMinWindowSizeForBody(d.body_hits, q);
        }

        else if (field.equals("anchors")) {
            if (d.anchors != null) {
                for (String anchor : d.anchors.keySet()) {
                    double curSize = getMinWindowSizeForString(anchor, q);
                    if (minSize > curSize) {
                        minSize = curSize;
                    }
                }
            }
        }

        else if (field.equals("header")) {
            if (d.headers != null) {
                for (String header : d.headers) {
                    double curSize = getMinWindowSizeForString(header, q);
                    if (minSize > curSize) {
                        minSize = curSize;
                    }
                }
            }
        }

        else {
            String value = "";
            if (field.equals("url")) value = d.url;
            if (field.equals("title")) value = d.title;
            return getMinWindowSizeForString(value, q);
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

    /* calcluates exact window score */
    private double getWindowScore(Document d, Query q) {
        return (Burl * smallestWindowMap.get("url").get(d).get(q) +
                Bbody * smallestWindowMap.get("body").get(d).get(q) +
                Btitle * smallestWindowMap.get("title").get(d).get(q) +
                Banchor * smallestWindowMap.get("anchor").get(d).get(q) +
                Bheader * smallestWindowMap.get("header").get(d).get(q));
    }

    @Override
    public double getSimScore(Document d, Query q) {
        Map<String, Map<String, Double>> tfs = this.getDocTermFreqs(d, q);

        this.normalizeTFs(tfs, d, q);

        Map<String, Double> tfQuery = getQueryFreqs(q);

        return this.getNetScore(tfs, q, tfQuery, d) +
                this.getWindowScore(d, q);
    }

}
