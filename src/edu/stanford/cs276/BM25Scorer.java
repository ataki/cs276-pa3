package edu.stanford.cs276;

import java.util.*;

public class BM25Scorer extends AScorer {
    Map<Query, Map<String, Document>> queryDict;

    public BM25Scorer(Map<String, Double> idfs, Map<Query, Map<String, Document>> queryDict) {
        super(idfs);
        this.queryDict = queryDict;

        ////////////// Initialize Lengths & Pagerank Scores /////////////////

        this.lengths = new HashMap<Document, Map<String, Double>>();
        this.avgLengths = new HashMap<String, Double>();
        this.pagerankScores = new HashMap<Document, Double>();
        this.fieldWs = new HashMap<String, Double>();
        this.fieldBs = new HashMap<String, Double>();

        this.avgLengths.put("url", 0.0);
        this.avgLengths.put("title", 0.0);
        this.avgLengths.put("header", 0.0);
        this.avgLengths.put("body", 0.0);
        this.avgLengths.put("anchor", 0.0);

        Set<String> docsSeen = new HashSet<String>();
        for (Query q : queryDict.keySet()) {
            Map<String, Document> docUrlMap = queryDict.get(q);
            for (String url : docUrlMap.keySet()) {
                Document d = docUrlMap.get(url);
                updateGlobalLengths(d);
                updatePagerankScores(d);
                docsSeen.add(url);
            }
        }
        this.totalNumDocs = docsSeen.size();

        for (String field : this.avgLengths.keySet()) {
            this.avgLengths.put(field, this.avgLengths.get(field) / totalNumDocs);
        }


        /////////////// Parameter Initialization ////////////////////////////

        this.fieldWs.put("url", this.urlweight);
        this.fieldWs.put("body", this.bodyweight);
        this.fieldWs.put("title", this.titleweight);
        this.fieldWs.put("header", this.headerweight);
        this.fieldWs.put("anchor", this.anchorweight);

        this.fieldBs.put("url", this.burl);
        this.fieldBs.put("body", this.bbody);
        this.fieldBs.put("title", this.btitle);
        this.fieldBs.put("header", this.bheader);
        this.fieldBs.put("anchor", this.banchor);
    }


    ///////////////weights///////////////////////////
    double urlweight = -1;
    double titleweight = -1;
    double bodyweight = -1;
    double headerweight = -1;
    double anchorweight = -1;

    ///////bm25 specific weights///////////////
    double burl = -1;
    double btitle = -1;
    double bheader = -1;
    double bbody = -1;
    double banchor = -1;

    double k1 = -1;
    double pageRankLambda = -1;
    double pageRankLambdaPrime = -1;
    double pageRankLambdaDoublePrime = -1;
    //////////////////////////////////////////

    ////////////bm25 data structures--feel free to modify ////////

    // doc -> (field -> len)
    Map<Document, Map<String, Double>> lengths;

    // field -> avglen
    Map<String, Double> avgLengths;

    // doc -> pagerank
    Map<Document, Double> pagerankScores;

    // field -> B values
    Map<String, Double> fieldBs;

    // field -> W values
    Map<String, Double> fieldWs;

    // number of docs in training set
    int totalNumDocs;

    //////////////////////////////////////////

    private void updateGlobalLengths(Document d) {
        Map<String, Double> fieldLenMap = new HashMap<String, Double>();

        double urlLen = (double)this.splitUrl(d.url).length;
        double titleLen = (d.title != null) ? (double)this.splitTitleOrHeader(d.title).length : 0;
        double bodyLen = (double)d.body_length;
        double headerLen = 0.0;
        if (d.headers != null) {
            for (String header : d.headers) {
                headerLen += (double) this.splitTitleOrHeader(header).length;
            }
        }
        double anchorLen = 0.0;
        if (d.anchors != null) {
            for (String anchor : d.anchors.keySet()) {
                double len = (double)anchor.split("\\W+").length;
                anchorLen += len * d.anchors.get(anchor);
            }
        }

        // update lengths for each document
        fieldLenMap.put("url",    urlLen);
        fieldLenMap.put("title",  titleLen);
        fieldLenMap.put("body",   bodyLen);
        fieldLenMap.put("header", headerLen);
        fieldLenMap.put("anchor", anchorLen);

        this.lengths.put(d, fieldLenMap);

        // update average length for each field
        this.avgLengths.put("url", this.avgLengths.get("url") + urlLen);
        this.avgLengths.put("title", this.avgLengths.get("title") + titleLen);
        this.avgLengths.put("header", this.avgLengths.get("header") + headerLen);
        this.avgLengths.put("body", this.avgLengths.get("body") + bodyLen);
        this.avgLengths.put("anchor", this.avgLengths.get("anchor") + anchorLen);
    }

    private void updatePagerankScores(Document d) {
        this.pagerankScores.put(d, (double)d.page_rank);
    }

    private double pagerankVj(double pagerank) {
        return Math.log(pageRankLambdaPrime + pagerank);
    }

    private double pagerankVjSigmoid(double pagerank) {
        return 1.0 / (pageRankLambdaPrime + Math.exp(-pagerank * pageRankLambdaDoublePrime));
    }

    private double pagerankVjSaturation(double pagerank) {
        return pagerank / (pagerank + pageRankLambdaPrime);
    }


    public double getNetScore(Map<String, Map<String, Double>> tfs, Query q, Map<String, Double> tfQuery, Document d) {
        double score = 0.0;

        for (String term : q.queryWords) {
            // w_(d,t)
            double overall_weight = 0.0;

            for (String field : this.TFTYPES) {
                // term weight
                double Wf = this.fieldWs.get(field);

                // field-dependent normalized term frequency
                double ftf = tfs.get(field).get(term);

                overall_weight += (Wf * ftf);
            }

            double weightTerm = overall_weight / (this.k1 + overall_weight);
            double idfTerm = this.idfs.get(term);
            double pagerankTerm = pageRankLambda * pagerankVj(this.pagerankScores.get(d));

            double termScore = weightTerm * idfTerm + pagerankTerm;
            score += termScore;
        }

        return score;
    }

    //do bm25 normalization
    public void normalizeTFs(Map<String, Map<String, Double>> tfs, Document d, Query q) {
        for (String term : q.queryWords) {
            double overall_weight = 0.0;
            for (String field : this.TFTYPES) {
                double raw_tf = tfs.get(field).get(term);
                double len_df = this.lengths.get(d).get(field);
                double avlen = this.avgLengths.get(field);
                double B = this.fieldBs.get(field);

                double norm = 1 + B * ((len_df / avlen) - 1);

                double ftf = raw_tf / norm;
                tfs.get(field).put(term, ftf);
            }
        }
    }

    @Override
    public double getSimScore(Document d, Query q) {

        Map<String, Map<String, Double>> tfs = this.getDocTermFreqs(d, q);

        this.normalizeTFs(tfs, d, q);

        Map<String, Double> tfQuery = getQueryFreqs(q);

        return getNetScore(tfs, q, tfQuery, d);
    }


}
